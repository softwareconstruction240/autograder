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

    public static String frontendAppUrl() {
        return INSTANCE.props.getProperty("frontend_app.url");
    }

    public static String backendAppUrl() {
        return INSTANCE.props.getProperty("backend_app.url");
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

    public static String casCallback() {
        return INSTANCE.props.getProperty("backend_app.cas_callback_url");
    }

    public static String canvasAuthorizationHeader() {
        return INSTANCE.props.getProperty("canvas_authorization_header");
    }
}
