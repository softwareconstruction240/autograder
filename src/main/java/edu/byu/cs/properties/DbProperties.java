package edu.byu.cs.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DbProperties {
    private static final Logger LOGGER = LoggerFactory.getLogger(DbProperties.class);

    private static final String DB_URL;
    private static final String DB_USER;
    private static final String DB_PASSWORD;
    private static final String DB_NAME;

    static {
        DB_URL = System.getenv("DB_URL");
        DB_USER = System.getenv("DB_USER");
        DB_PASSWORD = System.getenv("DB_PASSWORD");
        DB_NAME = System.getenv("DB_NAME");

        if (DB_URL == null || DB_USER == null || DB_PASSWORD == null || DB_NAME == null) {
            LOGGER.error("DB_URL, DB_USER, DB_PASSWORD, and DB_NAME must be set as environment variables.");
            System.exit(1);
        }
    }

    public static String dbUrl() {
        return DB_URL;
    }

    public static String dbUser() {
        return DB_USER;
    }

    public static String dbPassword() {
        return DB_PASSWORD;
    }

    public static String dbName() {
        return DB_NAME;
    }


}
