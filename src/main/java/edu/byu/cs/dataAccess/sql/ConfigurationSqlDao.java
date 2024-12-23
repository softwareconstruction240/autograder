package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class ConfigurationSqlDao implements ConfigurationDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationSqlDao.class);

    private static final String DEFAULT_VALUE = "default/changeme";

    public ConfigurationSqlDao() {
        for (var key : Configuration.values()) {
            try {
                getConfiguration(key, String.class);
            } catch (DataAccessException e) {
                try {
                    setConfiguration(key, DEFAULT_VALUE, String.class);
                } catch (DataAccessException ex) {
                    LOGGER.error("Error setting default configuration value for key: {}", key, ex);
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    @Override
    public <T> void setConfiguration(Configuration key, T value, Class<T> type) throws DataAccessException {
        try (var connection = SqlDb.getConnection()) {
            var statement = connection.prepareStatement("""
            INSERT INTO configuration (config_key, value) VALUES (?, ?)
            ON DUPLICATE KEY UPDATE value = VALUES(value);
            """);
            statement.setString(1, key.toString());
            statement.setString(2, value.toString());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error setting configuration", e);
        }
    }

    @Override
    public <T> T getConfiguration(Configuration key, Class<T> type) throws DataAccessException {
        try (var connection = SqlDb.getConnection();
             var statement = connection.prepareStatement("SELECT value FROM configuration WHERE config_key = ?")) {
            statement.setString(1, key.toString());
            var rs = statement.executeQuery();
            if (rs.next()) {
                return getValue(key, rs.getString("value"), type);
            }
            throw new DataAccessException("Configuration not found: " + key);
        } catch (Exception e) {
            throw new DataAccessException("Error getting configuration", e);
        }
    }

    private <T> T getValue(Configuration key, String value, Class<T> type) throws ReflectiveOperationException {
        Object typedObj = type.getDeclaredConstructor().newInstance();

        if (value.equals(DEFAULT_VALUE)) {
            LOGGER.warn("Using default configuration value for key: {} of type {}", key, type);

            return switch (typedObj) {
                case String ignored -> type.cast("");
                case Integer ignored -> type.cast(0);
                case Boolean ignored -> type.cast(false);
                case Instant ignored -> type.cast(Instant.MAX);
                case Float ignored -> type.cast(0f);
                default -> throw new IllegalArgumentException("Unsupported configuration type: " + type);
            };
        }

        return switch (typedObj) {
            case String ignored -> type.cast(value);
            case Integer ignored -> type.cast(Integer.parseInt(value));
            case Boolean ignored -> type.cast(Boolean.parseBoolean(value));
            case Instant ignored -> type.cast(Instant.parse(value));
            case Float ignored -> type.cast(Float.parseFloat(value));
            default -> throw new IllegalArgumentException("Unsupported configuration type: " + type);
        };
    }
}
