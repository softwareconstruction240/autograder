package edu.byu.cs.properties;
import java.util.Properties;

public class ApplicationProperties {
    private static final ApplicationProperties INSTANCE = new ApplicationProperties();
    private final Properties properties;

    private ApplicationProperties() {
        properties = new Properties();
    }

    public static void loadProperties(Properties properties) {
        INSTANCE.properties.putAll(properties);
    }

    private static String mustGet(String propertyName) {
        String value = INSTANCE.properties.getProperty(propertyName);
        if (value == null)
            throw new RuntimeException("Property " + propertyName + " not found in config.properties");
        return INSTANCE.properties.getProperty(propertyName);
    }

    public static String dbUrl() {
        return mustGet("db-url");
    }

    public static String dbName() {
        return mustGet("db-name");
    }

    public static String dbUser() {
        return mustGet("db-user");
    }

    public static String dbPass() {
        return mustGet("db-pass");
    }

    public static String frontendUrl() {
        return mustGet("frontend-url");
    }


    public static String casCallbackUrl() {
        return mustGet("cas-callback-url");
    }

    public static String canvasAPIToken() {
        return "Bearer " + mustGet("canvas-token");
    }
}
