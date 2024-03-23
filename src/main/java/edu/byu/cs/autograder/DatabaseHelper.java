package edu.byu.cs.autograder;

import edu.byu.cs.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

public class DatabaseHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHelper.class);
    private static String HOST;
    private static String PORT;
    private static String USER;
    private static String PASS;

    private final String databaseName;

    private final GradingContext gradingContext;

    private Collection<String> existingDatabaseNames;
    private boolean finished = false;

    public DatabaseHelper(long salt, GradingContext gradingContext) {
        this.gradingContext = gradingContext;
        this.databaseName = "chessdb" + salt;
        loadProperties();
    }

    public void setUp() throws GradingException {
        existingDatabaseNames = getExistingDatabaseNames();
        injectDatabaseConfig(gradingContext.stageRepo());
    }

    public void finish() throws GradingException {
        cleanupDatabase();
        finished = true;
        assertNoExtraDatabases(existingDatabaseNames, getExistingDatabaseNames());
    }

    public void cleanUp() {
        if (!finished) {
            try {
                Collection<String> currentDatabaseNames = getExistingDatabaseNames();
                currentDatabaseNames.removeAll(existingDatabaseNames);
                cleanUpExtraDatabases(currentDatabaseNames);
            } catch (GradingException e) {
                LOGGER.error("Error cleaning up after user " + gradingContext.netId() + " and repository " + gradingContext.repoUrl(), e);
            }
        }
    }

    private void loadProperties() {
        if(HOST == null) {
            HOST = ApplicationProperties.studentDbHost();
            PORT = ApplicationProperties.studentDbPort();
            USER = ApplicationProperties.studentDbUser();
            PASS = ApplicationProperties.studentDbPass();
        }
    }

    private void injectDatabaseConfig(File stageRepo) throws GradingException {
        File dbPropertiesFile = new File(stageRepo, "server/src/main/resources/db.properties");
        if (dbPropertiesFile.exists())
            dbPropertiesFile.delete();

        Properties dbProperties = new Properties();
        try {
            dbProperties.put("db.name", databaseName);
            dbProperties.put("db.host", HOST);
            dbProperties.put("db.port", PORT);
            dbProperties.put("db.user", USER);
            dbProperties.put("db.password", PASS);
            try(FileOutputStream os = new FileOutputStream(dbPropertiesFile.getAbsolutePath())) {
                dbProperties.store(os, "");
                os.flush();
            }
        } catch (IOException e) {
            throw new GradingException("Could add db config", e);
        }
    }

    private Collection<String> getExistingDatabaseNames() throws GradingException {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement("SHOW DATABASES");
             ResultSet rs = ps.executeQuery()) {
            Collection<String> databaseNames = new HashSet<>();
            while(rs.next()) {
                databaseNames.add(rs.getString(1));
            }
            return databaseNames;
        } catch (SQLException e) {
            LOGGER.error("Could not find database names", e);
            throw new GradingException("Failed to setup environment", e);
        }
    }

    private void cleanupDatabase() throws GradingException {
        try (Connection connection = getConnection()) {
            connection.createStatement().executeUpdate(
                    "DROP DATABASE IF EXISTS " + databaseName
            );
        } catch (SQLException e) {
            LOGGER.error("Failed to cleanup database", e);
            throw new GradingException("Failed to cleanup environment", e);
        }
    }

    private void cleanUpExtraDatabases(Collection<String> databaseNames) throws GradingException {
        try (Connection connection = getConnection()) {
            for (String databaseName : databaseNames) {
                try (PreparedStatement ps = connection.prepareStatement("DROP DATABASE IF EXISTS " + databaseName)) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Could not clean up databases", e);
            throw new GradingException("Failed to clean up db", e);
        }
    }

    private void assertNoExtraDatabases(Collection<String> previousDatabaseNames,
                                        Collection<String> currentDatabaseNames) throws GradingException {
        Collection<String> extraDatabaseNames = new HashSet<>(currentDatabaseNames);
        extraDatabaseNames.removeAll(previousDatabaseNames);
        if(!extraDatabaseNames.isEmpty()) {
            cleanUpExtraDatabases(extraDatabaseNames);
            throw new GradingException("Code created extra databases: " + extraDatabaseNames +
                    ". Only use the database name from db.properties");
        }
    }

    private Connection getConnection() throws SQLException {
        String connectionString = "jdbc:mysql://" + HOST + ":" + PORT;
        return DriverManager.getConnection(connectionString, USER, PASS);
    }
}
