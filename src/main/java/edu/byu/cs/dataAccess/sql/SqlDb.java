package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.properties.DbProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlDb {

    private static final String DB_URL = DbProperties.dbUrl();
    private static final String DB_USER = DbProperties.dbUser();
    private static final String DB_PASSWORD = DbProperties.dbPassword();

    private static final String DB_NAME = DbProperties.dbName();

    private static final String CONNECTION_STRING = "jdbc:mysql://" + DB_URL;

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlDb.class);

    static {
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD)) {
            connection.createStatement().executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS " + DB_NAME
            );
            connection.setCatalog(DB_NAME);

            connection.createStatement().executeUpdate(
                    """
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

            connection.createStatement().executeUpdate(
                    """
                            CREATE TABLE IF NOT EXISTS `submission` (
                                `id` INT NOT NULL AUTO_INCREMENT,
                                `net_id` VARCHAR(20) NOT NULL,
                                `repo_url` VARCHAR(200) NOT NULL,
                                `head_hash` VARCHAR(40) NOT NULL,
                                `timestamp` DATETIME NOT NULL,
                                `phase` VARCHAR(9) NOT NULL,
                                `passed` BOOL NOT NULL,
                                `score` FLOAT NOT NULL
                                `num_commits` INT,
                                `notes` TEXT,
                                `results` JSON,
                                PRIMARY KEY (`id`),
                                CONSTRAINT `net_id`
                                    FOREIGN KEY (`net_id`)
                                    REFERENCES `user` (`net_id`)
                                    ON DELETE CASCADE
                                    ON UPDATE CASCADE
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                            """);

            connection.createStatement().executeUpdate(
                    """
                            CREATE TABLE IF NOT EXISTS `queue` (
                                `net_id` VARCHAR(20) NOT NULL,
                                `phase` VARCHAR(9) NOT NULL,
                                `time_added` DATETIME NOT NULL,
                                `started` BOOL,
                                PRIMARY KEY (`net_id`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                            """);

            connection.createStatement().executeUpdate(
                    """
                            CREATE TABLE IF NOT EXISTS `phase_configuration` (
                                `phase` VARCHAR(9) NOT NULL,
                                `due_date_mountain_time` DATETIME NOT NULL,
                                PRIMARY KEY (`phase`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                            """);

            connection.createStatement().executeUpdate(
                    """
                            INSERT IGNORE INTO `phase_configuration` (`phase`, `due_date_mountain_time`) VALUES
                                ('Phase0', '2006-01-02 15:04:05'),
                                ('Phase1', '2006-01-02 15:04:05'),
                                ('Phase3', '2006-01-02 15:04:05'),
                                ('Phase4', '2006-01-02 15:04:05'),
                                ('Phase6', '2006-01-02 15:04:05')
                            """);

        } catch (SQLException e) {
            LOGGER.error("Error connecting to database", e);
            throw new DataAccessException("Error connecting to database", e);
        }
    }

    static Connection getConnection() {
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
