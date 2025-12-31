package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationMemoryDao implements ConfigurationDao {

    private final Map<Configuration, Object> configuration = new HashMap<>();

    @Override
    public <T> void setConfiguration(Configuration key, T value, Class<T> type) throws DataAccessException {
        if (key == null || type == null){
            throw new DataAccessException("Cannot set a configuration with a null key or type");
        }
        switch(type.getName()){
            case  "java.lang.Boolean", "java.lang.String", "java.lang.Integer", "java.time.Instant",
                  "java.lang.Float" -> configuration.put(key, value);
            case null, default -> throw new DataAccessException("Invalid type:" + type.getName());
        }
    }

    @Override
    public <T> T getConfiguration(Configuration key, Class<T> type) {
        if (configuration.get(key) == null){
            String className = type.getSimpleName();
            return type.cast(switch (className) {
                case "String" -> "";
                case "Integer" -> 0;
                case "Boolean" -> false;
                case "Instant" -> Instant.MAX;
                case "Float" -> 0f;
                default -> throw new IllegalArgumentException("Unsupported configuration type: " + type);
            });
        }
        return type.cast(configuration.get(key));
    }
}
