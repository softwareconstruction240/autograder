package edu.byu.cs.dataAccess.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.properties.ApplicationProperties;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A class responsible for setting up a SQL database and handling connections to the database.
 * <br>
 * The SQL database uses the application properties to obtain the databases' name, url, user,
 * and password in order to:
 * <ul>
 *     <li>Create the database and the appropriate tables if the database doesn't already exist</li>
 *     <li>Getting a connection to the database</li>
 * </ul>
 */
public class SqlDb {

    private static final String DB_URL = ApplicationProperties.dbUrl();

    private static final String DB_USER = ApplicationProperties.dbUser();

    private static final String DB_PASSWORD = ApplicationProperties.dbPass();

    private static final String DB_NAME = ApplicationProperties.dbName();

    private static final String CONNECTION_STRING = "jdbc:mysql://" + DB_URL;

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlDb.class);

    @Language("SQL")
    private static final String[] tableStatements = {
            """
            CREATE TABLE IF NOT EXISTS `user` (
                `net_id` varchar(20) NOT NULL,
                `canvas_user_id` int NOT NULL,
                `first_name` varchar(50) NOT NULL,
                `last_name` varchar(50) NOT NULL,
                `repo_url` varchar(200),
                `role` varchar(15) NOT NULL,
                PRIMARY KEY (`net_id`)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS `submission` (
                `id` INT NOT NULL AUTO_INCREMENT,
                `net_id` VARCHAR(20) NOT NULL,
                `repo_url` VARCHAR(200) NOT NULL,
                `head_hash` VARCHAR(40) NOT NULL,
                `timestamp` DATETIME NOT NULL,
                `phase` VARCHAR(9) NOT NULL,
                `passed` BOOL NOT NULL,
                `score` FLOAT NOT NULL,
                `raw_score` FLOAT NOT NULL,
                `notes` TEXT,
                `rubric` JSON,
                `verified_status` VARCHAR(30),
                `commit_context` JSON,
                `commit_result` JSON,
                `verification` JSON,
                `admin` BOOL NOT NULL,
                PRIMARY KEY (`id`),
                INDEX sort_index (`net_id`,`phase`,`passed`,`score`,`timestamp`),
                CONSTRAINT `net_id`
                    FOREIGN KEY (`net_id`)
                    REFERENCES `user` (`net_id`)
                    ON DELETE CASCADE
                    ON UPDATE CASCADE
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS `queue` (
                `net_id` VARCHAR(20) NOT NULL,
                `phase` VARCHAR(9) NOT NULL,
                `time_added` DATETIME NOT NULL,
                `started` BOOL,
                PRIMARY KEY (`net_id`)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS `rubric_config` (
                `phase` VARCHAR(9) NOT NULL,
                `type` VARCHAR(15) NOT NULL,
                `category` TEXT NOT NULL,
                `criteria` TEXT NOT NULL,
                `points` INT NOT NULL,
                `rubric_id` VARCHAR(15),
                PRIMARY KEY (`phase`, `type`)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS `configuration` (
                `config_key` VARCHAR(50) NOT NULL,
                `value` TEXT NOT NULL,
                PRIMARY KEY (`config_key`)
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS `repo_update` (
                `timestamp` TIMESTAMP NOT NULL,
                `net_id` VARCHAR(255) NOT NULL,
                `repo_url` VARCHAR(2048) NOT NULL,
                `admin_update` BOOLEAN NOT NULL,
                `admin_net_id` VARCHAR(255),
                PRIMARY KEY (`timestamp`)
            )
            """
    };

    private static HikariDataSource dataSource;

    public static void setUpDb() throws DataAccessException {
        setupConnectionPool();
        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            connection.setCatalog(DB_NAME);

            for (String createTable : tableStatements) {
                try (Statement createTableStatement = connection.createStatement()) {
                    createTableStatement.executeUpdate(createTable
                            + " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
                }
            }
            dataSource.setCatalog(DB_NAME);
        } catch (SQLException e) {
            LOGGER.error("Error connecting to database", e);
            throw new DataAccessException("Error connecting to database", e);
        }
    }

    private static void setupConnectionPool(){
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(CONNECTION_STRING);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit","2048");



        dataSource = new HikariDataSource(config);
    }

    public static Connection getConnection() throws DataAccessException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            LOGGER.error("Error connecting to database", e);

            throw new DataAccessException("Error connecting to database", e);
        }
    }
}
