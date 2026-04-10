package ru.filden.api;


import org.eclipse.jetty.util.thread.ThreadPool;
import ru.filden.api.config.AppConfig;
import ru.filden.api.dao.GroupDao;
import ru.filden.api.dao.StudentDao;
import ru.filden.api.dao.UserDao;
import ru.filden.api.db.DatabaseConnection;
import ru.filden.api.db.DatabaseInitializer;
import ru.filden.api.endpoints.RestEndpoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Spark;

public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static AppConfig config;
    private static DatabaseConnection dbConnection;

    public static void main(String[] args) {
           try {
                // 1. Загрузка конфигурации
                config = new AppConfig("application.properties");
                logger.info("Configuration loaded successfully");

                // 2. Настройка пула потоков SparkJava
                configureSparkThreadPool();

                // 3. Инициализация подключения к БД
                initializeDatabase();

                // 4. Проверка существования БД и таблиц
                initializeDatabaseSchema();

                // 5. Инициализация DAO
                UserDao userDao = new UserDao(dbConnection);
                StudentDao studentDao = new StudentDao(dbConnection);
                GroupDao groupDao = new GroupDao(dbConnection);

                // 6. Регистрация эндпоинтов
                RestEndpoints endpoints = new RestEndpoints(userDao, studentDao, groupDao);
                endpoints.registerEndpoints();

                // 7. Настройка обработки ошибок
                setupExceptionHandling();

                logger.info("Server started on port: {}", config.getServerPort());

                // 8. Ожидание завершения (Spark работает в отдельном потоке)
                Spark.awaitInitialization();

            } catch (Exception e) {
                logger.error("Failed to start application", e);
                System.exit(1);
            }
        }
        private static void configureSparkThreadPool() {
            Spark.threadPool(
                    config.getMaxThreads(),
                    config.getMinThreads(),
                    config.getIdleTimeout()
            );
            Spark.port(config.getServerPort());
            logger.info("Spark thread pool configured: min={}, max={}, idleTimeout={}",
                    config.getMinThreads(), config.getMaxThreads(), config.getIdleTimeout());
        }
        private static void initializeDatabase() {
            try {
                dbConnection = new DatabaseConnection(config.getHikariConfig(),false);
                logger.info("Database connection pool initialized");
            } catch (Exception e) {
                logger.error("Failed to initialize database connection", e);
                throw new RuntimeException("Database connection failed", e);
            }
        }

        private static void initializeDatabaseSchema() {
            DatabaseInitializer initializer = new DatabaseInitializer(dbConnection);
            initializer.initialize();
            logger.info("Database schema initialized");
        }

        private static void setupExceptionHandling() {
            Spark.exception(Exception.class, (e, req, res) -> {
                logger.error("Unhandled exception", e);
                res.status(500);
                res.type("application/json");
                res.body("{\"status\":\"error\",\"message\":\"Internal server error\"}");
            });

            Spark.notFound((req, res) -> {
                res.type("application/json");
                return "{\"status\":\"error\",\"message\":\"Endpoint not found\"}";
            });

            Spark.internalServerError((req, res) -> {
                res.type("application/json");
                return "{\"status\":\"error\",\"message\":\"Internal server error\"}";
            });
        }

        // Метод для graceful shutdown
        public static void shutdown() {
            logger.info("Shutting down application...");
            Spark.stop();
            if (dbConnection != null) {
                dbConnection.closePool();
            }
            logger.info("Application stopped");
        }
    }

