package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.ConfigurationDao;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationMemoryDao implements ConfigurationDao {

    private final Map<String, Object> configuration = new HashMap<>();

    @Override
    public <T> void setConfiguration(String key, T value, Class<T> type) {
        configuration.put(key, value);
    }

    @Override
    public <T> T getConfiguration(String key, T value, Class<T> type) {
        return type.cast(configuration.get(key));
    }
}
