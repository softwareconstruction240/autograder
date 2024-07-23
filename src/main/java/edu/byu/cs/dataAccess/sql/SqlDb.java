package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlDb {

    private static final String DB_URL = ApplicationProperties.dbUrl();

    private static final String DB_USER = ApplicationProperties.dbUser();

    private static final String DB_PASSWORD = ApplicationProperties.dbPass();

    private static final String DB_NAME = ApplicationProperties.dbName();

    private static final String CONNECTION_STRING = "jdbc:mysql://" + DB_URL;

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlDb.class);

    public static void setUpDb() throws DataAccessException {
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD);
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            connection.setCatalog(DB_NAME);

            try (Statement createUserTableStatement = connection.createStatement()) {
                createUserTableStatement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `user` (
                          `net_id` varchar(20) NOT NULL,
                          `canvas_user_id` int NOT NULL,
                          `first_name` varchar(50) NOT NULL,
                          `last_name` varchar(50) NOT NULL,
                          `repo_url` varchar(200),
                          `role` varchar(15) NOT NULL,
                          PRIMARY KEY (`net_id`)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                        """);
            }
            try (Statement createSubmissionTableStatement = connection.createStatement()) {
                createSubmissionTableStatement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `submission` (
                                `id` INT NOT NULL AUTO_INCREMENT,
                                `net_id` VARCHAR(20) NOT NULL,
                                `repo_url` VARCHAR(200) NOT NULL,
                                `head_hash` VARCHAR(40) NOT NULL,
                                `timestamp` DATETIME NOT NULL,
                                `phase` VARCHAR(9) NOT NULL,
                                `passed` BOOL NOT NULL,
                                `score` FLOAT NOT NULL,
                                `notes` TEXT,
                                `rubric` JSON,
                                `verified_status` VARCHAR(30),
                                `verification` JSON,
                                `admin` BOOL NOT NULL,
                                PRIMARY KEY (`id`),
                                INDEX sort_index (timestamp),
                                CONSTRAINT `net_id`
                                    FOREIGN KEY (`net_id`)
                                    REFERENCES `user` (`net_id`)
                                    ON DELETE CASCADE
                                    ON UPDATE CASCADE
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                        """);
            }
            try (Statement createQueueTableStatement = connection.createStatement()) {
                createQueueTableStatement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `queue` (
                                `net_id` VARCHAR(20) NOT NULL,
                                `phase` VARCHAR(9) NOT NULL,
                                `time_added` DATETIME NOT NULL,
                                `started` BOOL,
                                PRIMARY KEY (`net_id`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                        """);
            }
            try (Statement createRubricConfigTableStatement = connection.createStatement()) {
                createRubricConfigTableStatement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `rubric_config` (
                                `phase` VARCHAR(9) NOT NULL,
                                `type` VARCHAR(15) NOT NULL,
                                `category` TEXT NOT NULL,
                                `criteria` TEXT NOT NULL,
                                `points` INT NOT NULL,
                                `rubric_id` VARCHAR(15),
                                PRIMARY KEY (`phase`, `type`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                        """);
            }
            try (Statement createConfigurationTableStatement = connection.createStatement()) {
                createConfigurationTableStatement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS `configuration` (
                                `config_key` VARCHAR(50) NOT NULL,
                                `value` TEXT NOT NULL,
                                PRIMARY KEY (`config_key`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                        """);
            }
        } catch (SQLException e) {
            LOGGER.error("Error connecting to database", e);
            throw new DataAccessException("Error connecting to database", e);
        }
    }

    public static Connection getConnection() throws DataAccessException {
        try {
            Connection connection = DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD);
            connection.setCatalog(DB_NAME);
            return connection;
        } catch (SQLException e) {
            LOGGER.error("Error connecting to database", e);

            throw new DataAccessException("Error connecting to database", e);
        }
    }
}
