package ru.filden.api.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static volatile DatabaseConnection instance;
    private HikariDataSource dataSource;
    private HikariConfig hikariConfig;

    private DatabaseConnection(HikariConfig config) {
        this.hikariConfig = config;
        initializeDataSource();
    }

    public DatabaseConnection(HikariConfig config, boolean useSingleton) {
        if (useSingleton) {
            throw new UnsupportedOperationException("Use getInstance() for singleton pattern");
        }
        this.hikariConfig = config;
        initializeDataSource();
    }
    public static DatabaseConnection getInstance(HikariConfig config) {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection(config);
                }
            }
        }
        return instance;
    }

    private void initializeDataSource() {
        try {
            configureMSSQLProperties();

            this.dataSource = new HikariDataSource(hikariConfig);
            testConnection();
            logger.info("HikariCP connection pool initialized successfully");
            logger.info("Configuration: URL={}, MaxPoolSize={}, MinIdle={}",
                    hikariConfig.getJdbcUrl(),
                    hikariConfig.getMaximumPoolSize(),
                    hikariConfig.getMinimumIdle());

        } catch (Exception e) {
            logger.error("Failed to initialize database connection pool", e);
            throw new RuntimeException("Database connection pool initialization failed", e);
        }
    }

    private void configureMSSQLProperties() {
        if (!hikariConfig.getDataSourceProperties().containsKey("sendStringParametersAsUnicode")) {
            hikariConfig.addDataSourceProperty("sendStringParametersAsUnicode", "false");
        }

        if (!hikariConfig.getDataSourceProperties().containsKey("encrypt")) {
            hikariConfig.addDataSourceProperty("encrypt", "true");
        }

        if (!hikariConfig.getDataSourceProperties().containsKey("trustServerCertificate")) {
            hikariConfig.addDataSourceProperty("trustServerCertificate", "true");
        }

        if (hikariConfig.getConnectionTimeout() == 30000) {
            hikariConfig.setConnectionTimeout(30000);
        }

        if (hikariConfig.getIdleTimeout() == 600000) {
            hikariConfig.setIdleTimeout(600000);
        }

        if (hikariConfig.getMaxLifetime() == 1800000) {
            hikariConfig.setMaxLifetime(1800000);
        }

        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setValidationTimeout(5000);

        hikariConfig.setLeakDetectionThreshold(60000);
    }

    private void testConnection() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("SELECT 1");
            logger.info("Database connection test successful");

            String dbVersion = conn.getMetaData().getDatabaseProductVersion();
            logger.info("MS SQL Server version: {}", dbVersion);

        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            throw new RuntimeException("Cannot connect to database", e);
        }
    }


    public Connection getConnection() throws SQLException {
        try {
            Connection conn = dataSource.getConnection();
            logger.debug("Connection acquired from pool. Active connections: {}",
                    dataSource.getHikariPoolMXBean().getActiveConnections());
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to get connection from pool", e);
            throw new SQLException("Cannot acquire database connection", e);
        }
    }

    public Connection getConnection(long timeoutMillis) throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Failed to get connection from pool within timeout", e);
            throw new SQLException("Cannot acquire database connection within " + timeoutMillis + "ms", e);
        }
    }

    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool");
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    public boolean isClosed() {
        return dataSource == null || dataSource.isClosed();
    }

    public PoolStatistics getPoolStatistics() {
        if (dataSource == null || dataSource.isClosed()) {
            return new PoolStatistics(0, 0, 0, 0, 0);
        }

        var poolMXBean = dataSource.getHikariPoolMXBean();
        return new PoolStatistics(
                poolMXBean.getActiveConnections(),
                poolMXBean.getIdleConnections(),
                poolMXBean.getTotalConnections(),
                poolMXBean.getThreadsAwaitingConnection(),
                dataSource.getMaximumPoolSize()
        );
    }
    //на потом для гуишки
    public static class PoolStatistics {
        private final int activeConnections;
        private final int idleConnections;
        private final int totalConnections;
        private final int threadsAwaiting;
        private final int maxPoolSize;

        public PoolStatistics(int activeConnections, int idleConnections,
                              int totalConnections, int threadsAwaiting, int maxPoolSize) {
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.totalConnections = totalConnections;
            this.threadsAwaiting = threadsAwaiting;
            this.maxPoolSize = maxPoolSize;
        }

        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getTotalConnections() { return totalConnections; }
        public int getThreadsAwaiting() { return threadsAwaiting; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public double getPoolUsagePercent() {
            return maxPoolSize > 0 ? (double) activeConnections / maxPoolSize * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("PoolStats{active=%d, idle=%d, total=%d, awaiting=%d, max=%d, usage=%.1f%%}",
                    activeConnections, idleConnections, totalConnections,
                    threadsAwaiting, maxPoolSize, getPoolUsagePercent());
        }
    }

    public boolean isDatabaseAvailable() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warn("Database availability check failed", e);
            return false;
        }
    }

    private String maskPassword(String url) {
        if (url != null && url.contains("password=")) {
            return url.replaceAll("password=[^&;]+", "password=***");
        }
        return url;
    }
}