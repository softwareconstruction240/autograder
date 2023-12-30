package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.properties.DbProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlDb {

    private static final String DB_URL = DbProperties.dbUrl();
    private static final String DB_USER = DbProperties.dbUser();
    private static final String DB_PASSWORD = DbProperties.dbPassword();

    private static final String DB_NAME = DbProperties.dbName();

    private static final String CONNECTION_STRING = "jdbc:mysql://" + DB_URL ;

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
                      `first_name` varchar(50) NOT NULL,
                      `last_name` varchar(50) NOT NULL,
                      `role` varchar(15) NOT NULL,
                      PRIMARY KEY (`net_id`)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                    """);

            connection.createStatement().executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS `submissions` (
                        `id` INT NOT NULL AUTO_INCREMENT,
                        `net_id` VARCHAR(20) NOT NULL,
                        `repo_url` VARCHAR(20) NOT NULL,
                        `head_hash` VARCHAR(40) NOT NULL,
                        `timestamp` DATETIME NOT NULL,
                        `phase` FLOAT NOT NULL,
                        `score` FLOAT NOT NULL,
                        PRIMARY KEY (`id`),
                        CONSTRAINT `net_id`
                            FOREIGN KEY (`net_id`)
                            REFERENCES `user` (`net_id`)
                            ON DELETE CASCADE
                            ON UPDATE CASCADE
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
                    """);

        } catch (SQLException e) {
            System.err.println("Error connecting to database");
            e.printStackTrace();

            throw new DataAccessException("Error connecting to database", e);
        }
    }

    static Connection getConnection() {
        try {
            Connection connection = DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD);
            connection.setCatalog(DB_NAME);
            return connection;
        } catch (SQLException e) {
            System.err.println("Error connecting to database");
            e.printStackTrace();

            throw new DataAccessException("Error connecting to database", e);
        }
    }
}
