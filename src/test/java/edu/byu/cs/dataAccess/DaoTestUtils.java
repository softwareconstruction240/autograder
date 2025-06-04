package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.sql.SqlDb;
import edu.byu.cs.properties.ApplicationProperties;

import java.io.FileReader;
import java.util.Properties;
import java.util.Scanner;

public class DaoTestUtils {

    /**
     * In order to successfully run these tests on your machine, you're going to need either some environment
     * variables, or a file containing them named 'testdb.env'. You'll need the following values:
     * <ul>
     *     <li>DB_HOST</li>
     *     <li>DB_PORT</li>
     *     <li>DB_NAME</li>
     *     <li>DB_USER</li>
     *     <li>DB_PASS</li>
     * </ul>
     * It's highly recommended that you use a different DB name than the one you use for development,
     * as the tests will drop tables when they need to
     */
    static void prepareSQLDatabase() throws DataAccessException{
        Properties props = new Properties();
        try {
            props.setProperty("db-port", System.getenv("DB_PORT"));
            props.setProperty("db-host", System.getenv("DB_HOST"));
            props.setProperty("db-name", System.getenv("DB_NAME"));
            props.setProperty("db-user", System.getenv("DB_USER"));
            props.setProperty("db-pass", System.getenv("DB_PASS"));
        } catch (NullPointerException e){
            System.out.println("Unable to find environment variables. Switching to reading \'testdb.env\'...");
            try(FileReader reader = new FileReader("testdb.env");
            Scanner scanner = new Scanner(reader);) {
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
}
