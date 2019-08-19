package com.questdb.griffin.engine.functions.jdbc;

import com.questdb.cairo.CairoConfiguration;
import com.questdb.cairo.ColumnType;
import com.questdb.cairo.sql.Function;
import com.questdb.cairo.sql.Record;
import com.questdb.cairo.sql.RecordCursor;
import com.questdb.cairo.sql.RecordMetadata;
import com.questdb.griffin.FunctionFactory;
import com.questdb.griffin.SqlException;
import com.questdb.griffin.engine.functions.constants.NullConstant;
import com.questdb.std.ObjList;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionFunctionFactory implements FunctionFactory {
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_DRIVER = "driver";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_USER = "user";
    private static final String COLUMN_PASSWORD = "password";
    private static final String COLUMN_SCHEMA = "schema";
    private static final String COLUMN_CATALOG = "catalog";
    private static final String COLUMN_AUTO_COMMIT = "auto_commit";
    private static final String COLUMN_READ_ONLY = "read_only";
    private static final String COLUMN_JMX = "jmx";
    private static final String COLUMN_MAX_POOL_SIZE = "max_pool_size";
    private static final Map<String, HikariDataSource> DBCP = new ConcurrentHashMap<>();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> DBCP.values().forEach(HikariDataSource::close)));
    }

    @Override
    public String getSignature() {
        return "jdbc_pool_init(C)";
    }

    @Override
    public Function newInstance(ObjList<Function> args, int position, CairoConfiguration sqlConfiguration) throws SqlException {
        Function settings = args.getQuick(0);
        RecordMetadata metadata = settings.getMetadata();

        int nameIdx = getColumnIndex(metadata, COLUMN_NAME, ColumnType.STRING, true);
        int urlIdx = getColumnIndex(metadata, COLUMN_URL, ColumnType.STRING, true);
        int userIdx = getColumnIndex(metadata, COLUMN_USER, ColumnType.STRING, true);
        int passwordIdx = getColumnIndex(metadata, COLUMN_PASSWORD, ColumnType.STRING, true);
        int driverIdx = getColumnIndex(metadata, COLUMN_DRIVER, ColumnType.STRING, false);
        int schemaIdx = getColumnIndex(metadata, COLUMN_SCHEMA, ColumnType.STRING, false);
        int catalogIdx = getColumnIndex(metadata, COLUMN_CATALOG, ColumnType.STRING, false);
        int autoCommitIdx = getColumnIndex(metadata, COLUMN_AUTO_COMMIT, ColumnType.BOOLEAN, false);
        int readOnlyIdx = getColumnIndex(metadata, COLUMN_READ_ONLY, ColumnType.BOOLEAN, false);
        int jmxIdx = getColumnIndex(metadata, COLUMN_JMX, ColumnType.BOOLEAN, false);
        int maxPoolSizeIdx = getColumnIndex(metadata, COLUMN_MAX_POOL_SIZE, ColumnType.INT, false);

        RecordCursor recordCursor = settings.getRecordCursorFactory().getCursor(null);
        while (recordCursor.hasNext()){
            Record record = recordCursor.getRecord();
            HikariConfig configuration = new HikariConfig();
            String poolName = String.valueOf(record.getStr(nameIdx));
            configuration.setPoolName(poolName);

            configuration.setJdbcUrl(String.valueOf(record.getStr(urlIdx)));
            configuration.setUsername(String.valueOf(record.getStr(userIdx)));
            configuration.setPassword(String.valueOf(record.getStr(passwordIdx)));

            if(driverIdx!=-1){
                configuration.setDriverClassName(String.valueOf(record.getStr(driverIdx)));
            }
            if(schemaIdx!=-1){
                configuration.setSchema(String.valueOf(record.getStr(schemaIdx)));
            }
            if(catalogIdx!=-1){
                configuration.setCatalog(String.valueOf(record.getStr(catalogIdx)));
            }
            if(autoCommitIdx!=-1){
                configuration.setAutoCommit(record.getBool(autoCommitIdx));
            }
            if(readOnlyIdx!=-1){
                configuration.setReadOnly(record.getBool(readOnlyIdx));
            }
            if(jmxIdx!=-1){
                configuration.setRegisterMbeans(record.getBool(jmxIdx));
            }
            if(maxPoolSizeIdx!=-1){
                configuration.setMaximumPoolSize(record.getInt(maxPoolSizeIdx));
            }

            //configuration.setTransactionIsolation();
            //configuration.setIdleTimeout();
            //configuration.setInitializationFailTimeout();
            //configuration.setConnectionTimeout();
            //configuration.setMaxLifetime();
            //configuration.setMinimumIdle();
            //configuration.setValidationTimeout();

            HikariDataSource dataSource = DBCP.putIfAbsent(poolName, new HikariDataSource(configuration));
            if(dataSource != null){
                dataSource.close();
            }
        }

        return new NullConstant(position);
    }

    private int getColumnIndex(RecordMetadata metadata, String columnName, int expectedColumnType, boolean requered) throws SqlException {
        int columnIndex = metadata.getColumnIndexQuiet(columnName);
        if(requered && columnIndex==-1){
            throw SqlException.invalidColumn(columnIndex,columnName).put(" not found");
        }
        if(columnIndex!=-1 && metadata.getColumnType(columnIndex) != expectedColumnType){
            throw SqlException.invalidColumn(columnIndex, columnName).put(", expected type ").put(expectedColumnType).put(" but found ").put(metadata.getColumnType(columnIndex));
        }
        return columnIndex;
    }

    static DataSource getDataSource(String dataSourceName) {
        HikariDataSource dataSource = DBCP.get(dataSourceName);
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource " + dataSourceName + " not found");
        }
        return dataSource;
    }
}
