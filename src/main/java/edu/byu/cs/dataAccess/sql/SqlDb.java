package edu.byu.cs.dataAccess.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.sql.helpers.SqlScriptParser;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
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

    private static HikariDataSource dataSource;

    public static void setUpDb() throws DataAccessException {
        ResourceUtils.copyResourceFiles("sql", new File(""));
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD);
             Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME);
            connection.setCatalog(DB_NAME);

            for (String tableStatement : SqlScriptParser.parseSQLScript(new File("./sql/db-startup.sql"))) {
                try (Statement createTableStatement = connection.createStatement()) {
                    createTableStatement.executeUpdate(tableStatement);
                }
            }

            setupConnectionPool();
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
        config.setCatalog(DB_NAME);

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
