package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;

import java.util.HashMap;
import java.util.Map;

public class ConfigurationMemoryDao implements ConfigurationDao {

    private final Map<Configuration, Object> configuration = new HashMap<>();

    @Override
    public <T> void setConfiguration(Configuration key, T value, Class<T> type) throws DataAccessException {
        if (key == null){
            throw new DataAccessException("Cannot set a configuration with a null key");
        }
        configuration.put(key, value);
    }

    @Override
    public <T> T getConfiguration(Configuration key, Class<T> type) {
        return type.cast(configuration.get(key));
    }
}
