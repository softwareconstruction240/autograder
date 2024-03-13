package edu.byu.cs.autograder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;

public class DatabaseHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHelper.class);
    private static String dbHost;
    private static String dbPort;
    private static String dbUser;
    private static String dbPassword;
    private static boolean needsLoad = true;

    private final String databaseName;

    public DatabaseHelper(long salt) {
        this.databaseName = "chessdb" + salt;
    }

    public void injectDatabaseConfig(File stageRepo) {
        File dbPropertiesFile = new File(stageRepo, "server/src/main/resources/db.properties");
        if (dbPropertiesFile.exists())
            dbPropertiesFile.delete();

        File dbPropertiesSource = new File("./phases/libs/db.properties");
        Properties dbProperties = new Properties();
        try {
            dbProperties.load(Files.newInputStream(dbPropertiesSource.getAbsoluteFile().toPath()));
            System.out.println("loaded");
            dbProperties.put("db.name", databaseName);
            try(ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                dbProperties.store(os, "");
                InputStream is = new ByteArrayInputStream(os.toByteArray());
                Files.copy(is, dbPropertiesFile.toPath());
            }
            System.out.println("injected");
        } catch (IOException e) {
            throw new RuntimeException("Could add db config", e);
        }
    }

    public Collection<String> getExistingDatabaseNames() {
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
            throw new RuntimeException("Failed to setup environment", e);
        }
    }

    public void cleanupDatabase() {
        try (Connection connection = getConnection()) {
            connection.createStatement().executeUpdate(
                    "DROP DATABASE IF EXISTS " + databaseName
            );
        } catch (SQLException e) {
            LOGGER.error("Failed to cleanup database", e);
            throw new RuntimeException("Failed to cleanup environment", e);
        }
    }

    public void cleanUpExtraDatabases(Collection<String> databaseNames) {
        try (Connection connection = getConnection()) {
            for (String databaseName : databaseNames) {
                try (PreparedStatement ps = connection.prepareStatement("DROP DATABASE IF EXISTS " + databaseName)) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Could not clean up databases", e);
            throw new RuntimeException("Failed to clean up db", e);
        }
    }

    public void assertNoExtraDatabases(Collection<String> previousDatabaseNames,
                                        Collection<String> currentDatabaseNames) {
        Collection<String> extraDatabaseNames = new HashSet<>(currentDatabaseNames);
        extraDatabaseNames.removeAll(previousDatabaseNames);
        if(!extraDatabaseNames.isEmpty()) {
            cleanUpExtraDatabases(extraDatabaseNames);
            throw new RuntimeException("Code created extra databases: " + extraDatabaseNames +
                    ". Only use the database name from db.properties");
        }
    }

    private Connection getConnection() throws SQLException {
        if(needsLoad) {
            String dbPropertiesPath = new File("./phases/libs/db.properties").getAbsolutePath();
            Properties dbProperties = new Properties();
            try (InputStream input = Files.newInputStream(Path.of(dbPropertiesPath))) {
                dbProperties.load(input);
            } catch (IOException ex) {
                LOGGER.error("Error loading properties file", ex);
                System.exit(1);
            }

            dbHost = dbProperties.getProperty("db.host");
            dbPort = dbProperties.getProperty("db.port");
            dbUser = dbProperties.getProperty("db.user");
            dbPassword = dbProperties.getProperty("db.password");
            needsLoad = false;
        }
        String connectionString = "jdbc:mysql://" + dbHost + ":" + dbPort;

        return DriverManager.getConnection(connectionString, dbUser, dbPassword);
    }
}
