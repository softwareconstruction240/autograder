package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.ConfigurationDao;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationMemoryDao implements ConfigurationDao {

    private final Map<Configuration, Object> configuration = new HashMap<>();

    @Override
    public <T> void setConfiguration(Configuration key, T value, Class<T> type) {
        configuration.put(key, value);
    }

    @Override
    public <T> T getConfiguration(Configuration key, Class<T> type) {
        return type.cast(configuration.get(key));
    }
}
