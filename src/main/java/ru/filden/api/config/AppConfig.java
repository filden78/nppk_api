package ru.filden.api.config;

import com.zaxxer.hikari.HikariConfig;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private Properties properties = new Properties();

    // Серверные настройки
    private int serverPort;
    private int minThreads;
    private int maxThreads;
    private int idleTimeout;

    // Настройки БД
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private int dbMaxPoolSize;
    private int dbMinIdle;

    public AppConfig(String configFile) {
        loadConfig(configFile);
    }

    private void loadConfig(String configFile) {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                logger.warn("Config file not found, using default values");
                setDefaultValues();
                return;
            }
            properties.load(input);

            serverPort = Integer.parseInt(properties.getProperty("server.port", "4567"));
            minThreads = Integer.parseInt(properties.getProperty("server.threads.min", "2"));
            maxThreads = Integer.parseInt(properties.getProperty("server.threads.max", "10"));
            idleTimeout = Integer.parseInt(properties.getProperty("server.threads.idle.timeout", "60000"));

            dbUrl = properties.getProperty("db.url", "jdbc:sqlserver://localhost:1433;databaseName=restdb");
            dbUser = properties.getProperty("db.user", "sa");
            dbPassword = properties.getProperty("db.password", "password");
            dbMaxPoolSize = Integer.parseInt(properties.getProperty("db.pool.max.size", "10"));
            dbMinIdle = Integer.parseInt(properties.getProperty("db.pool.min.idle", "2"));

        } catch (Exception e) {
            logger.error("Error loading config", e);
            setDefaultValues();
        }
    }

    private void setDefaultValues() {
        serverPort = 4567;
        minThreads = 2;
        maxThreads = 10;
        idleTimeout = 60000;
        dbUrl = "jdbc:sqlserver://localhost:1433;databaseName=restdb";
        dbUser = "sa";
        dbPassword = "password";
        dbMaxPoolSize = 10;
        dbMinIdle = 2;
    }

    public HikariConfig getHikariConfig() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbUrl);
        hikariConfig.setUsername(dbUser);
        hikariConfig.setPassword(dbPassword);
        hikariConfig.setMaximumPoolSize(dbMaxPoolSize);
        hikariConfig.setMinimumIdle(dbMinIdle);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        return hikariConfig;
    }

    // Getters
    public int getServerPort() { return serverPort; }
    public int getMinThreads() { return minThreads; }
    public int getMaxThreads() { return maxThreads; }
    public int getIdleTimeout() { return idleTimeout; }
    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
}