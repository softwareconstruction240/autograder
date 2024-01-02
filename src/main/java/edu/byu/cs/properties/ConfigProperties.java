package edu.byu.cs.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProperties {
    private static final ConfigProperties INSTANCE = new ConfigProperties();
    private final Properties props;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigProperties.class);

    private ConfigProperties() {
        props = new Properties();
        createInstance();
    }


    private void createInstance() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            props.load(input);

        } catch (IOException ex) {
            LOGGER.error("Error loading properties file", ex);
            System.exit(1);
        }
    }

    public static String appUrl() {
        return INSTANCE.props.getProperty("app.url");
    }

    public static String casServerUrl() {
        return INSTANCE.props.getProperty("cas_server.url");
    }

    public static String casServerLoginEndpoint() {
        return INSTANCE.props.getProperty("cas_server.login_endpoint");
    }

    public static String casServerServiceValidateEndpoint() {
        return INSTANCE.props.getProperty("cas_server.service_validate_endpoint");
    }
}
