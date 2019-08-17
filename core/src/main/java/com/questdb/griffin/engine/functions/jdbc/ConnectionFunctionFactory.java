package com.questdb.griffin.engine.functions.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class ConnectionFunctionFactory {

    private static final Map<String, HikariDataSource> DBCP = new HashMap<>();

    static {
        String source = "mem";
        HikariDataSource dataSource = createDataSource(source, "jdbc:h2:mem:a", "", "");
        dirtyInit(dataSource);
        DBCP.put(source, dataSource);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> DBCP.values().forEach(HikariDataSource::close)));
    }

    @SneakyThrows
    private static void dirtyInit(HikariDataSource dataSource) {
        Connection connection = dataSource.getConnection();
        connection.createStatement().executeUpdate("CREATE TABLE TEST(X INT, XY LONG, DATA VARCHAR(255), ts TIMESTAMP)");
        connection.createStatement().executeUpdate("INSERT INTO TEST(X,XY,DATA,ts) VALUES (1,30,'abc',now()),(2,301,'abc2',null)");
    }

    private static HikariDataSource createDataSource(String dataSource, String jdbcUrl, String userName, String password) {
        HikariConfig configuration = new HikariConfig();
        configuration.setPoolName(dataSource);

        //configuration.setDriverClassName();
        configuration.setJdbcUrl(jdbcUrl);
        configuration.setUsername(userName);
        configuration.setPassword(password);

        //configuration.setMaximumPoolSize();

        //configuration.setAutoCommit();
        //configuration.setTransactionIsolation();

        //configuration.setCatalog();
        //configuration.setSchema();

        //configuration.setIdleTimeout();
        //configuration.setInitializationFailTimeout();
        //configuration.setConnectionTimeout();
        //configuration.setMaxLifetime();
        //configuration.setMinimumIdle();
        //configuration.setValidationTimeout();

        configuration.setRegisterMbeans(true);

        return new HikariDataSource(configuration);
    }

    static DataSource getDataSource(String dataSourceName) {
        HikariDataSource dataSource = DBCP.get(dataSourceName);
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource " + dataSourceName + " not found");
        }
        return dataSource;
    }
}
