package edu.byu.cs.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbProperties {
    private static final DbProperties INSTANCE = new DbProperties();
    private final Properties props;
    private static final Logger LOGGER = LoggerFactory.getLogger(DbProperties.class);

    private DbProperties() {
        props = new Properties();
        createInstance();
    }

    private void createInstance() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            props.load(input);

        } catch (IOException ex) {
            LOGGER.error("Error loading properties file", ex);
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
