package edu.byu.cs.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigManager {
    private static final ConfigManager INSTANCE = new ConfigManager();
    private final Properties props;

    private ConfigManager() {
        props = new Properties();
        createInstance();
    }


    private void createInstance() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            props.load(input);

        } catch (IOException ex) {
            System.err.println("Error loading properties file");
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public static String appUrl() {
        return INSTANCE.props.getProperty("app.url");
    }

    public static String casServerUrl() {
        return INSTANCE.props.getProperty("cas_server.url");
    }

    public static String casServerServiceValidateEndpoint() {
        return INSTANCE.props.getProperty("cas_server.service_validate_endpoint");
    }
}
