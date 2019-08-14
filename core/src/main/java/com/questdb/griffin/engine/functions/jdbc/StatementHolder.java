package com.questdb.griffin.engine.functions.jdbc;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;

public class StatementHolder implements Closeable {
    private final Connection connection;
    private final Statement statement;
    @Getter
    private ResultSet resultSet;

    StatementHolder(Connection connection, PreparedStatement statement) throws SQLException {
        this.connection = connection;
        this.statement = statement;
        try {
            resultSet = statement.executeQuery();
        } catch (SQLException e) {
            try (Connection ignored = this.connection){
                statement.close();
            }
            throw e;
        }
    }

    @Override
    @SneakyThrows
    public void close() throws IOException {
        try (Connection ignoredConnection = connection){
            try (Statement ignored = statement){
                resultSet.close();
            }
        }
    }
}
