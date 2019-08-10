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
import com.questdb.cairo.sql.*;
import com.questdb.griffin.FunctionFactory;
import com.questdb.griffin.engine.functions.CursorFunction;
import com.questdb.griffin.engine.functions.GenericRecordCursorFactory;
import com.questdb.std.ObjList;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;

public class JdbcFunctionFactory implements FunctionFactory {

    @Override
    public String getSignature() {
        return "jdbc(S)";
    }

    @Override
    @SneakyThrows
    public Function newInstance(ObjList<Function> args, int position, CairoConfiguration configuration) {

        final CharSequence query = args.getQuick(0).getStr(null);
        //TODO externalize database connection/ use pool
        java.util.function.Function<Boolean,ResultSet> function = new java.util.function.Function<Boolean, ResultSet>() {
            @Override
            @SneakyThrows
            public ResultSet apply(Boolean strtuctureOnly) {
                Connection connection = DriverManager.getConnection("jdbc:h2:mem:", "", "");
                try {
                    connection.createStatement().executeUpdate("CREATE TABLE TEST(X INT, XY LONG, DATA VARCHAR(255), ts TIMESTAMP)");
                    connection.createStatement().executeUpdate("INSERT INTO TEST(X,XY,DATA,ts) VALUES (1,30,'abc',now()),(2,301,'abc2',null)");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                Statement statement = connection.createStatement();
                if (strtuctureOnly) {
                    statement.setMaxRows(0);
                }
                return statement.executeQuery(String.valueOf(query));//TODO close all db statements/connections
            }
        };
        ResultSet resultSet = function.apply(Boolean.TRUE);
        final GenericRecordMetadata metadata = getResultSetMetadata(resultSet);
        return new CursorFunction(
                position,
                new GenericRecordCursorFactory(metadata, new JdbcRecordCursor(function), false)
        );
    }

    @NotNull
    private GenericRecordMetadata getResultSetMetadata(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        final GenericRecordMetadata metadata = new GenericRecordMetadata();
        for(int columnIdx = 1; columnIdx<= metaData.getColumnCount(); columnIdx++){
            String typeName = metaData.getColumnTypeName(columnIdx).replace("BIGINT","LONG").replace("VARCHAR","STRING").replace("INTEGER","INT");
            //TODO map java.sql.Types.* to com.questdb.cairo.ColumnType
            metadata.add(new TableColumnMetadata(metaData.getColumnName(columnIdx), ColumnType.columnTypeOf(typeName)));
        }
        return metadata;
    }

    static class JdbcRecordCursor implements NoRandomAccessRecordCursor {

        private final JdbcRecord record;

        JdbcRecordCursor(java.util.function.Function<Boolean,ResultSet> function) {
            record = new JdbcRecord(function);
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
        private long row;
        private ResultSet resultSet;
        private java.util.function.Function<Boolean,ResultSet> resultSetFunction;
        //TODO handle null values

        JdbcRecord(java.util.function.Function<Boolean,ResultSet> resultSetFunction) {
            this.resultSetFunction = resultSetFunction;
        }

        @SneakyThrows
        void init(){
            if(resultSet!=null){
                close();
            }
            row=0;
            resultSet = resultSetFunction.apply(Boolean.FALSE);
        }
        @Override
        @SneakyThrows
        public boolean getBool(int col) {
            return resultSet.getBoolean(col+1);
        }

        @Override
        @SneakyThrows
        public byte getByte(int col) {
            return resultSet.getByte(col+1);
        }

        @Override
        @SneakyThrows
        public long getDate(int col) {
            Date date = resultSet.getDate(col + 1);
            return date!=null?date.getTime():Long.MIN_VALUE;
        }

        @Override
        @SneakyThrows
        public double getDouble(int col) {
            return resultSet.getDouble(col+1);
        }

        @Override
        @SneakyThrows
        public float getFloat(int col) {
            return resultSet.getFloat(col+1);
        }

        @Override
        @SneakyThrows
        public int getInt(int col) {
            return resultSet.getInt(col+1);
        }

        @Override
        @SneakyThrows
        public long getLong(int col) {
            return resultSet.getLong(col+1);
        }

        @Override
        @SneakyThrows
        public short getShort(int col) {
            return resultSet.getShort(col+1);
        }

        @Override
        @SneakyThrows
        public CharSequence getStr(int col) {
            return resultSet.getString(col+1);
        }

        @Override
        @SneakyThrows
        public long getTimestamp(int col) {
            Timestamp timestamp = resultSet.getTimestamp(col + 1);
            return timestamp!=null?timestamp.getTime():Long.MIN_VALUE;
        }

        @Override
        public long getRowId() {
            return row;
        }

        @SneakyThrows
        boolean next() {
            row++;
            return resultSet.next();
        }

        @Override
        public void close() throws IOException {
            if(resultSet==null){
                return;
            }
            try {
                resultSet.close();
                resultSet=null;
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }
}
