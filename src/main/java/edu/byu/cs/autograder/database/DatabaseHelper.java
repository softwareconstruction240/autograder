package edu.byu.cs.autograder.database;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Sets up a MySQL database to test students' database management.
 */
public class DatabaseHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHelper.class);
    private static final String HOST;
    private static final String PORT;
    private static final String ADMIN_USER;
    private static final String ADMIN_PASS;
    private static final String connectionString;

    static {
        HOST = ApplicationProperties.dbHost();
        PORT = ApplicationProperties.dbPort();
        ADMIN_USER = ApplicationProperties.dbUser();
        ADMIN_PASS = ApplicationProperties.dbPass();
        connectionString = "jdbc:mysql://" + HOST + ":" + PORT;
    }

    private final String databaseName;
    private final String studentUser;
    private final String studentPass;
    private final GradingContext gradingContext;

    public DatabaseHelper(long salt, GradingContext gradingContext) {
        this.gradingContext = gradingContext;
        this.databaseName = "chessDb" + salt;
        this.studentUser = "dbUser" + salt;
        this.studentPass = "dbPass" + salt;
    }

    /**
     * Creates a user and a db config file
     *
     * @throws GradingException if there is a failure either creating the user
     * or the db config file
     */
    public void setUp() throws GradingException {
        createUser();
        grantPrivileges();
        injectDatabaseConfig(gradingContext.stageRepo());
    }

    /**
     * Cleans up the database and user
     */
    public void cleanUp() {
        try {
            cleanupDatabase();
            deleteUser();
        } catch (GradingException e) {
            LOGGER.error("Error cleaning up after user {} and repository {}", gradingContext.netId(),
                    gradingContext.repoUrl(), e);
        }
    }

    /**
     * Creates a db config file in the staged repo
     *
     * @param stageRepo the file where the student's repo is located
     * @throws GradingException if the db config file could not be created
     */
    private void injectDatabaseConfig(File stageRepo) throws GradingException {
        File dbPropertiesFile = new File(stageRepo, "server/src/main/resources/db.properties");
        if (dbPropertiesFile.exists() && !dbPropertiesFile.delete())
            throw new GradingException("Could not delete previous db.properties");
        else dbPropertiesFile.getParentFile().mkdirs();

        Properties dbProperties = new Properties();
        try {
            dbProperties.put("db.name", databaseName);
            dbProperties.put("db.host", HOST);
            dbProperties.put("db.port", PORT);
            dbProperties.put("db.user", studentUser);
            dbProperties.put("db.password", studentPass);
            try(FileOutputStream os = new FileOutputStream(dbPropertiesFile.getAbsolutePath())) {
                dbProperties.store(os, "");
                os.flush();
            }
        } catch (IOException e) {
            throw new GradingException("Could not add db config", e);
        }
    }

    private void createUser() throws GradingException {
        executeUpdate("CREATE USER IF NOT EXISTS ?@'%' IDENTIFIED BY ?",
                "Failed to setup environment",
                studentUser, studentPass);
    }

    private void grantPrivileges() throws GradingException {
        executeUpdate("GRANT ALL ON `" + databaseName + "`.* TO ?@'%'",
                "Failed to setup environment",
                studentUser);
    }

    private void cleanupDatabase() throws GradingException {
        executeUpdate("DROP DATABASE IF EXISTS " + databaseName, "Failed to cleanup database");
    }

    private void deleteUser() throws GradingException {
        executeUpdate("DROP USER IF EXISTS ?@'%'",
                "Failed to cleanup environment",
                studentUser);
    }

    private void executeUpdate(String statement, String errorMessage, String... params) throws GradingException {
        try (Connection conn = DriverManager.getConnection(connectionString, ADMIN_USER, ADMIN_PASS);
             PreparedStatement ps = conn.prepareStatement(statement)) {
            for (var i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error(errorMessage, e);
            throw new GradingException(errorMessage, e);
        }
    }
}
