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

    // Приватный конструктор для Singleton паттерна
    private DatabaseConnection(HikariConfig config) {
        this.hikariConfig = config;
        initializeDataSource();
    }

    // Конструктор для создания экземпляра без Singleton
    public DatabaseConnection(HikariConfig config, boolean useSingleton) {
        if (useSingleton) {
            throw new UnsupportedOperationException("Use getInstance() for singleton pattern");
        }
        this.hikariConfig = config;
        initializeDataSource();
    }

    // Singleton паттерн (опционально)
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

    // Инициализация пула соединений
    private void initializeDataSource() {
        try {
            // Дополнительные настройки для MS SQL Server
            configureMSSQLProperties();

            // Создание пула соединений
            this.dataSource = new HikariDataSource(hikariConfig);

            // Проверка соединения
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

    // Дополнительные настройки для MS SQL Server
    private void configureMSSQLProperties() {
        // Настройки для MS SQL Server, если не установлены в конфиге
        if (!hikariConfig.getDataSourceProperties().containsKey("sendStringParametersAsUnicode")) {
            hikariConfig.addDataSourceProperty("sendStringParametersAsUnicode", "false");
        }

        if (!hikariConfig.getDataSourceProperties().containsKey("encrypt")) {
            hikariConfig.addDataSourceProperty("encrypt", "true");
        }

        if (!hikariConfig.getDataSourceProperties().containsKey("trustServerCertificate")) {
            hikariConfig.addDataSourceProperty("trustServerCertificate", "true");
        }

        // Настройка таймаутов
        if (hikariConfig.getConnectionTimeout() == 30000) {
            hikariConfig.setConnectionTimeout(30000); // 30 секунд
        }

        if (hikariConfig.getIdleTimeout() == 600000) {
            hikariConfig.setIdleTimeout(600000); // 10 минут
        }

        if (hikariConfig.getMaxLifetime() == 1800000) {
            hikariConfig.setMaxLifetime(1800000); // 30 минут
        }

        // Настройка валидации соединений
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setValidationTimeout(5000);

        // Настройка для быстрого освобождения соединений при ошибках
        hikariConfig.setLeakDetectionThreshold(60000); // 60 секунд
    }

    // Тестирование соединения с базой данных
    private void testConnection() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("SELECT 1");
            logger.info("Database connection test successful");

            // Получение информации о версии БД
            String dbVersion = conn.getMetaData().getDatabaseProductVersion();
            logger.info("MS SQL Server version: {}", dbVersion);

        } catch (SQLException e) {
            logger.error("Database connection test failed", e);
            throw new RuntimeException("Cannot connect to database", e);
        }
    }

    // Получение соединения из пула
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

    // Получение соединения с таймаутом
    public Connection getConnection(long timeoutMillis) throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            logger.error("Failed to get connection from pool within timeout", e);
            throw new SQLException("Cannot acquire database connection within " + timeoutMillis + "ms", e);
        }
    }

    // Закрытие пула соединений
    public void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Closing database connection pool");
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    // Проверка, закрыт ли пул
    public boolean isClosed() {
        return dataSource == null || dataSource.isClosed();
    }

    // Получение статистики пула соединений
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

    // Внутренний класс для статистики пула
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

    // Проверка доступности БД
    public boolean isDatabaseAvailable() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warn("Database availability check failed", e);
            return false;
        }
    }

    // Выполнение транзакции с автоматическим управлением
    public <T> T executeInTransaction(TransactionOperation<T> operation) throws SQLException {
        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            T result = operation.execute(conn);

            conn.commit();
            logger.debug("Transaction committed successfully");
            return result;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Transaction rolled back due to error", e);
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                }
            }
            throw new SQLException("Transaction execution failed", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Failed to close connection after transaction", e);
                }
            }
        }
    }

    // Функциональный интерфейс для операций в транзакции
    @FunctionalInterface
    public interface TransactionOperation<T> {
        T execute(Connection connection) throws SQLException, Exception;
    }

    // Выполнение пакетной операции
    public int[] executeBatch(BatchOperation operation) throws SQLException {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            int[] results = operation.execute(conn);

            conn.commit();
            logger.debug("Batch operation completed successfully");
            return results;

        } catch (SQLException e) {
            logger.error("Batch operation failed", e);
            throw e;
        }
    }

    @FunctionalInterface
    public interface BatchOperation {
        int[] execute(Connection connection) throws SQLException;
    }

    // Сброс пула соединений (пересоздание)
    public void resetPool() {
        logger.info("Resetting connection pool");
        closePool();
        initializeDataSource();
        logger.info("Connection pool reset completed");
    }

    // Получение диагностической информации
    public String getDiagnosticInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Database Connection Diagnostic Info ===\n");
        sb.append("Pool is closed: ").append(isClosed()).append("\n");

        if (!isClosed()) {
            PoolStatistics stats = getPoolStatistics();
            sb.append(stats.toString()).append("\n");
            sb.append("Database available: ").append(isDatabaseAvailable()).append("\n");
            sb.append("JDBC URL: ").append(maskPassword(hikariConfig.getJdbcUrl())).append("\n");
            sb.append("Max Pool Size: ").append(hikariConfig.getMaximumPoolSize()).append("\n");
            sb.append("Min Idle: ").append(hikariConfig.getMinimumIdle()).append("\n");
            sb.append("Connection Timeout: ").append(hikariConfig.getConnectionTimeout()).append("ms\n");
            sb.append("Idle Timeout: ").append(hikariConfig.getIdleTimeout()).append("ms\n");
            sb.append("Max Lifetime: ").append(hikariConfig.getMaxLifetime()).append("ms\n");
            sb.append("Leak Detection Threshold: ").append(hikariConfig.getLeakDetectionThreshold()).append("ms\n");
        }

        return sb.toString();
    }

    // Маскирование пароля в URL для логирования
    private String maskPassword(String url) {
        if (url != null && url.contains("password=")) {
            return url.replaceAll("password=[^&;]+", "password=***");
        }
        return url;
    }
}