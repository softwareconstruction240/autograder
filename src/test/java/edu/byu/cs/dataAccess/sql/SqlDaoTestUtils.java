package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.properties.ApplicationProperties;

import java.io.FileReader;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;

public class SqlDaoTestUtils {

    private static final String PATH_TO_ENV_VARS = "src/test/resources/testdb.env";

    /**
     * In order to successfully run these tests on your machine, you're going to need either some environment
     * variables, or a file containing them named 'testdb.env' in the test's resources folder.
     * <strong>Make sure the it's in the test resources, not the normal resources!</strong>
     * An example of what the file looks like has been provided at 'testdb.example.env'
     * You'll need the following values:
     * <ul>
     *     <li>DB_HOST</li>
     *     <li>DB_PORT</li>
     *     <li>DB_NAME</li>
     *     <li>DB_USER</li>
     *     <li>DB_PASS</li>
     * </ul>
     * It's highly recommended that you use a different DB name than the one you use for development,
     * as the tests will truncate tables when necessary
     */
    static void prepareSQLDatabase() throws DataAccessException {
        Properties props = new Properties();
        try {
            props.setProperty("db-port", System.getenv("DB_PORT"));
            props.setProperty("db-host", System.getenv("DB_HOST"));
            props.setProperty("db-name", System.getenv("DB_NAME"));
            props.setProperty("db-user", System.getenv("DB_USER"));
            props.setProperty("db-pass", System.getenv("DB_PASS"));
        } catch (NullPointerException e){
            System.out.println("Unable to find environment variables. Switching to reading 'testdb.env'...");
            try(FileReader reader = new FileReader(PATH_TO_ENV_VARS);
                Scanner scanner = new Scanner(reader)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    String[] values = line.split("=");
                    switch (values[0]) {
                        case "DB_PORT" -> props.setProperty("db-port", values[1]);
                        case "DB_HOST" -> props.setProperty("db-host", values[1]);
                        case "DB_NAME" -> props.setProperty("db-name", values[1]);
                        case "DB_USER" -> props.setProperty("db-user", values[1]);
                        case "DB_PASS" -> props.setProperty("db-pass", values[1]);
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException("Unable to load db properties", ex);
            }
        }
        ApplicationProperties.loadProperties(props);
        SqlDb.setUpDb();
    }

    /**
     * Will call "DELETE FROM tableName" on the database. Assuming all foreign key constraints are
     * defined in the table for deletes, this should work. If it doesn't, you may need to play around
     * with table structures or change how the tests clear.
     * <br>
     * You'll notice this code doesn't use a prepared statement for the table name. Though SQL injection attacks would
     * be unlikely as this code should never go into production or receive user input, as a precaution a valid table
     * name check has been implemented
     *
     * @param tableName
     * @throws DataAccessException for sql errors
     */
    public static void deleteTableWithCascade(String tableName) throws DataAccessException {
        if (!isValidTableName(tableName)){
            throw new DataAccessException("Attempted to delete a table that doesn't exist");
        }
        try(var connection = SqlDb.getConnection()){
            var statement = connection.prepareStatement("DELETE FROM " + tableName);
            statement.executeUpdate();
        }catch (SQLException e){
            throw new DataAccessException("Could not clear " + tableName, e);
        }
    }

    private static boolean isValidTableName(String tableName){
        try{
            TableNames.valueOf(tableName.toUpperCase(Locale.ROOT));
            return true;
        }
        catch (IllegalArgumentException e){
            return false;
        }
    }

    protected enum TableNames{
        USER,
        SUBMISSION,
        QUEUE,
        RUBRIC_CONFIG,
        CONFIGURATION,
        REPO_UPDATE,
    }
}
