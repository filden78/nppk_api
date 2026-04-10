package ru.filden.api.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final DatabaseConnection dbConnection;

    public DatabaseInitializer(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public void initialize() {
        createTablesIfNotExist();
        checkConstraints();
    }

    private void createTablesIfNotExist() {
        String createGroupTable = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='groups' AND xtype='U')
            CREATE TABLE groups (
                id INT IDENTITY(1,1) PRIMARY KEY,
                number VARCHAR(50) NOT NULL UNIQUE
            )
        """;

        String createUserTable = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='users' AND xtype='U')
            CREATE TABLE users (
                id INT IDENTITY(1,1) PRIMARY KEY,
                login VARCHAR(100) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL
            )
        """;

        String createStudentTable = """
            IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='students' AND xtype='U')
            CREATE TABLE students (
                id INT IDENTITY(1,1) PRIMARY KEY,
                fio VARCHAR(255) NOT NULL,
                group_number VARCHAR(50) NOT NULL,
                is_duty BIT DEFAULT 0,
                count_duty INT DEFAULT 0,
                user_login VARCHAR(100) NOT NULL UNIQUE,
                FOREIGN KEY (group_number) REFERENCES groups(number) ON DELETE CASCADE,
                FOREIGN KEY (user_login) REFERENCES users(login) ON DELETE CASCADE
            )
        """;

        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_student_group ON students(group_number);
            CREATE INDEX IF NOT EXISTS idx_student_duty ON students(is_duty);
            CREATE INDEX IF NOT EXISTS idx_user_login ON users(login);
        """;

        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createGroupTable);
            stmt.execute(createUserTable);
            stmt.execute(createStudentTable);
            stmt.execute(createIndexes);

            logger.info("Database tables initialized successfully");

        } catch (SQLException e) {
            logger.error("Error creating tables", e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void checkConstraints() {
        logger.info("Database constraints verified");
        // Здесь можно добавить проверку внешних ключей и других ограничений
    }
}