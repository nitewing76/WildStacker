package com.bgsoftware.wildstacker.database;

import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.database.sql.Column;
import com.bgsoftware.wildstacker.database.sql.IDatabaseTransaction;
import com.bgsoftware.wildstacker.database.sql.QueryResult;
import com.bgsoftware.wildstacker.handlers.SettingsHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class DBSession {

    private static HikariDataSource dataSource;
    private static Connection sqliteConnection;
    private static boolean isMySQL = false;
    private static String tablePrefix = "";
    private static ExecutorService dbExecutor;

    private DBSession() {
    }

    public static boolean createConnection(WildStackerPlugin plugin) {
        close();

        dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "WildStacker-Database-Thread");
            thread.setDaemon(true);
            return thread;
        });

        SettingsHandler settings = plugin.getSettings();
        String dbType = settings.databaseType == null ? "SQLITE" : settings.databaseType.toUpperCase(Locale.ENGLISH);
        tablePrefix = settings.databaseMySQLPrefix == null ? "" : settings.databaseMySQLPrefix;

        try {
            if (dbType.equals("MYSQL") || dbType.equals("MARIADB")) {
                isMySQL = true;
                HikariConfig config = new HikariConfig();
                String jdbcUrl = String.format(
                        "jdbc:mysql://%s:%d/%s?useSSL=%b&allowPublicKeyRetrieval=%b",
                        settings.databaseMySQLAddress,
                        settings.databaseMySQLPort,
                        settings.databaseMySQLDBName,
                        settings.databaseMySQLSSL,
                        settings.databaseMySQLPublicKeyRetrieval
                );
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(settings.databaseMySQLUsername);
                config.setPassword(settings.databaseMySQLPassword);
                config.setPoolName("WildStacker-HikariPool");
                config.setMaximumPoolSize(4);
                if (settings.databaseMySQLWaitTimeout > 0) {
                    config.setConnectionTimeout(settings.databaseMySQLWaitTimeout);
                }
                if (settings.databaseMySQLMaxLifetime > 0) {
                    config.setMaxLifetime(settings.databaseMySQLMaxLifetime);
                }
                dataSource = new HikariDataSource(config);
                try (Connection conn = dataSource.getConnection()) {
                    return conn != null && !conn.isClosed();
                }
            } else {
                isMySQL = false;
                if (!plugin.getDataFolder().exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    plugin.getDataFolder().mkdirs();
                }
                File dbFile = new File(plugin.getDataFolder(), "database.db");
                Class.forName("org.sqlite.JDBC");
                sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
                return sqliteConnection != null && !sqliteConnection.isClosed();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static synchronized Connection acquireConnection() throws SQLException {
        if (isMySQL) {
            if (dataSource == null) {
                throw new SQLException("HikariDataSource is not initialized.");
            }
            return dataSource.getConnection();
        } else {
            if (sqliteConnection == null || sqliteConnection.isClosed()) {
                throw new SQLException("SQLite connection is closed.");
            }
            return sqliteConnection;
        }
    }

    private static void releaseConnection(Connection conn) {
        if (isMySQL && conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    public static void createTable(String tableName, Column... columns) {
        String columnDefs = Stream.of(columns)
                .map(col -> col.getName() + " " + normalizeColumnType(col.getType()))
                .collect(Collectors.joining(", "));
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + tableName + " (" + columnDefs + ");";

        Connection conn = null;
        try {
            conn = acquireConnection();
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            releaseConnection(conn);
        }
    }

    public static void addColumn(String tableName, String columnName, String typeAndDefault) {
        String fullTableName = tablePrefix + tableName;
        Connection conn = null;
        try {
            conn = acquireConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, fullTableName, columnName)) {
                if (rs.next()) {
                    return;
                }
            }
            String sql = "ALTER TABLE " + fullTableName + " ADD COLUMN " + columnName + " " + normalizeColumnType(typeAndDefault) + ";";
            try (Statement st = conn.createStatement()) {
                st.executeUpdate(sql);
            }
        } catch (SQLException ignored) {
        } finally {
            releaseConnection(conn);
        }
    }

    public static void select(String tableName, String filters, QueryResult<ResultSet> queryResult) {
        String sql = "SELECT * FROM " + tablePrefix + tableName + (filters == null || filters.isEmpty() ? "" : " " + filters) + ";";
        Connection conn = null;
        try {
            conn = acquireConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                queryResult.complete(rs);
            }
        } catch (Exception ex) {
            queryResult.fail(ex);
        } finally {
            releaseConnection(conn);
        }
    }

    public static void execute(IDatabaseTransaction transaction) {
        if (transaction != null) {
            execute(Collections.singletonList(transaction));
        }
    }

    public static void execute(Collection<IDatabaseTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return;
        }
        Runnable task = () -> {
            Connection conn = null;
            try {
                conn = acquireConnection();
                for (IDatabaseTransaction transaction : transactions) {
                    if (transaction != null) {
                        transaction.execute(conn, tablePrefix);
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            } finally {
                releaseConnection(conn);
            }
        };

        if (dbExecutor != null && !dbExecutor.isShutdown()) {
            dbExecutor.submit(task);
        } else {
            task.run();
        }
    }

    public static void close() {
        if (dbExecutor != null) {
            dbExecutor.shutdown();
            try {
                dbExecutor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            dbExecutor = null;
        }
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        if (sqliteConnection != null) {
            try {
                sqliteConnection.close();
            } catch (SQLException ignored) {
            }
            sqliteConnection = null;
        }
    }

    private static String normalizeColumnType(String rawType) {
        if (rawType == null) {
            return "TEXT";
        }
        return rawType.replace("LONG_UNIQUE_TEXT", "VARCHAR(255)");
    }

}
