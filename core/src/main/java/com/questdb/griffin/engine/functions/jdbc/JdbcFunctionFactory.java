/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2019 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.questdb.griffin.engine.functions.jdbc;

import com.questdb.cairo.CairoConfiguration;
import com.questdb.cairo.ColumnType;
import com.questdb.cairo.GenericRecordMetadata;
import com.questdb.cairo.TableColumnMetadata;
import com.questdb.cairo.sql.Function;
import com.questdb.cairo.sql.NoRandomAccessRecordCursor;
import com.questdb.cairo.sql.Record;
import com.questdb.griffin.FunctionFactory;
import com.questdb.griffin.engine.functions.CursorFunction;
import com.questdb.griffin.engine.functions.GenericRecordCursorFactory;
import com.questdb.ql.NullRecord;
import com.questdb.std.IntIntHashMap;
import com.questdb.std.ObjList;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.sql.DataSource;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.logging.Logger;

public class JdbcFunctionFactory implements FunctionFactory {
    private static final IntIntHashMap jdbcToQuestColumnType = new IntIntHashMap();

    static {
        jdbcToQuestColumnType.put(Types.VARCHAR, ColumnType.STRING);
        jdbcToQuestColumnType.put(Types.NVARCHAR, ColumnType.STRING);
        jdbcToQuestColumnType.put(Types.LONGVARCHAR, ColumnType.STRING);
        jdbcToQuestColumnType.put(Types.LONGNVARCHAR, ColumnType.STRING);
        jdbcToQuestColumnType.put(Types.TIMESTAMP, ColumnType.TIMESTAMP);
        jdbcToQuestColumnType.put(Types.TIMESTAMP_WITH_TIMEZONE, ColumnType.TIMESTAMP);
        jdbcToQuestColumnType.put(Types.TIME, ColumnType.TIMESTAMP);
        jdbcToQuestColumnType.put(Types.DOUBLE, ColumnType.DOUBLE);
        jdbcToQuestColumnType.put(Types.FLOAT, ColumnType.FLOAT);
        jdbcToQuestColumnType.put(Types.REAL, ColumnType.FLOAT);
        jdbcToQuestColumnType.put(Types.INTEGER, ColumnType.INT);
        jdbcToQuestColumnType.put(Types.SMALLINT, ColumnType.SHORT);
        jdbcToQuestColumnType.put(Types.BIGINT, ColumnType.LONG);
        jdbcToQuestColumnType.put(Types.BOOLEAN, ColumnType.BOOLEAN);
        jdbcToQuestColumnType.put(Types.DATE, ColumnType.DATE);
        jdbcToQuestColumnType.put(Types.BINARY, ColumnType.BINARY);
        jdbcToQuestColumnType.put(Types.LONGVARBINARY, ColumnType.BINARY);
        jdbcToQuestColumnType.put(Types.VARBINARY, ColumnType.BINARY);
        //jdbcToQuestColumnType.put(ColumnType.BYTE);
        //jdbcToQuestColumnType.put(ColumnType.SYMBOL);
    }

    private static final NullRecord NULL = NullRecord.INSTANCE;

    @Override
    public String getSignature() {
        return "jdbc(S)";
    }

    @Override
    @SneakyThrows
    public Function newInstance(ObjList<Function> args, int position, CairoConfiguration configuration) {

        final CharSequence query = args.getQuick(0).getStr(null);
        //TODO externalize database connection/ use pool
        StatementHolder statementHolder = getStatementHolder(query);
        final GenericRecordMetadata metadata = getResultSetMetadata(statementHolder.getResultSet());
        return new CursorFunction(
                position,
                new GenericRecordCursorFactory(metadata, new JdbcRecordCursor(statementHolder), false)
        );
    }

    @NotNull
    private StatementHolder getStatementHolder(CharSequence query) throws SQLException {

        return new StatementHolder(new DataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                Connection connection = DriverManager.getConnection("jdbc:h2:mem:", "", "");
                try {
                    connection.createStatement().executeUpdate("CREATE TABLE TEST(X INT, XY LONG, DATA VARCHAR(255), ts TIMESTAMP)");
                    connection.createStatement().executeUpdate("INSERT INTO TEST(X,XY,DATA,ts) VALUES (1,30,'abc',now()),(2,301,'abc2',null)");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return connection;
            }

            @Override
            public Connection getConnection(String s, String s1) throws SQLException {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> aClass) throws SQLException {
                return null;
            }

            @Override
            public boolean isWrapperFor(Class<?> aClass) throws SQLException {
                return false;
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return null;
            }

            @Override
            public void setLogWriter(PrintWriter printWriter) throws SQLException {

            }

            @Override
            public void setLoginTimeout(int i) throws SQLException {

            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;
            }

            @Override
            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return null;
            }
        }, String.valueOf(query), true);
    }

    @NotNull
    private GenericRecordMetadata getResultSetMetadata(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        final GenericRecordMetadata metadata = new GenericRecordMetadata();
        for (int columnIdx = 1; columnIdx <= metaData.getColumnCount(); columnIdx++) {
            int columnType = jdbcToQuestColumnType.get(metaData.getColumnType(columnIdx));
            if (columnType == -1) {
                throw new IllegalArgumentException("JDBC column type isn't supported " +
                        metaData.getColumnTypeName(columnIdx));
            }
            metadata.add(new TableColumnMetadata(metaData.getColumnName(columnIdx), columnType));
        }
        return metadata;
    }

    static class JdbcRecordCursor implements NoRandomAccessRecordCursor {

        private final JdbcRecord record;

        JdbcRecordCursor(StatementHolder statementHolder) {
            record = new JdbcRecord(statementHolder);
        }

        @Override
        @SneakyThrows
        public void close() {
            record.close();
        }

        @Override
        public Record getRecord() {
            return record;
        }

        @Override
        public boolean hasNext() {
            return record.next();
        }


        @Override
        public void toTop() {
            record.init();
        }
    }

    static class JdbcRecord implements Record, Closeable {
        private StatementHolder statementHolder;

        JdbcRecord(StatementHolder statementHolder) {
            this.statementHolder = statementHolder;
        }

        @SneakyThrows
        void init() {
            statementHolder.createUnlimitedResultSet();
        }

        @Override
        @SneakyThrows
        public boolean getBool(int col) {
            boolean val = statementHolder.getResultSet().getBoolean(col + 1);
            if (statementHolder.getResultSet().wasNull()) {
                return NULL.getBool(col);
            }
            return val;
        }

        @Override
        @SneakyThrows
        public byte getByte(int col) {
            byte val = statementHolder.getResultSet().getByte(col + 1);
            if (statementHolder.getResultSet().wasNull()) {
                return NULL.getByte(col);
            }
            return val;
        }

        @Override
        @SneakyThrows
        public long getDate(int col) {
            Date date = statementHolder.getResultSet().getDate(col + 1);
            return date != null ? date.getTime() : NULL.getDate(col);
        }

        @Override
        @SneakyThrows
        public double getDouble(int col) {
            double val = statementHolder.getResultSet().getDouble(col + 1);
            if (statementHolder.getResultSet().wasNull()) {
                return NULL.getDouble(col);
            }
            return val;
        }

        @Override
        @SneakyThrows
        public float getFloat(int col) {
            float val = statementHolder.getResultSet().getFloat(col + 1);
            if (statementHolder.getResultSet().wasNull()) {
                return NULL.getFloat(col);
            }
            return val;
        }

        @Override
        @SneakyThrows
        public int getInt(int col) {
            int val = statementHolder.getResultSet().getInt(col + 1);
            if (statementHolder.getResultSet().wasNull()) {
                return NULL.getInt(col);
            }
            return val;
        }

        @Override
        @SneakyThrows
        public long getLong(int col) {
            long val = statementHolder.getResultSet().getLong(col + 1);
            if (statementHolder.getResultSet().wasNull()) {
                return NULL.getLong(col);
            }
            return val;
        }

        @Override
        @SneakyThrows
        public short getShort(int col) {
            short val = statementHolder.getResultSet().getShort(col + 1);
            if (statementHolder.getResultSet().wasNull()) {
                return NULL.getShort(col);
            }
            return val;
        }

        @Override
        @SneakyThrows
        public CharSequence getStr(int col) {
            return statementHolder.getResultSet().getString(col + 1);
        }

        @Override
        @SneakyThrows
        public long getTimestamp(int col) {
            Timestamp timestamp = statementHolder.getResultSet().getTimestamp(col + 1);
            return timestamp != null ? timestamp.getTime() : NULL.getLong(col);
        }

        @SneakyThrows
        boolean next() {
            return statementHolder.getResultSet().next();
        }

        @Override
        public void close() throws IOException {
            statementHolder.close();
        }
    }
}
