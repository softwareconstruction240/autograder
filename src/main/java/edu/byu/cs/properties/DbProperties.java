package edu.byu.cs.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbProperties {
    private static final DbProperties INSTANCE = new DbProperties();
    private final Properties props;

    private DbProperties() {
        props = new Properties();
        createInstance();
    }

    private void createInstance() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            props.load(input);

        } catch (IOException ex) {
            System.err.println("Error loading properties file");
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static String dbUrl() {
        return INSTANCE.props.getProperty("db.url");
    }

    public static String dbUser() {
        return INSTANCE.props.getProperty("db.user");
    }

    public static String dbPassword() {
        return INSTANCE.props.getProperty("db.password");
    }

    public static String dbName() {
        return INSTANCE.props.getProperty("db.name");
    }


}
