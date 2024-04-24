package edu.byu.cs.dataAccess;

import java.util.Map;

public interface ConfigurationDao {
    <T> void setConfiguration(Configuration key, T value, Class<T> type) throws DataAccessException;
    <T> T getConfiguration(Configuration key, Class<T> type) throws DataAccessException;

    enum Configuration {
        STUDENT_SUBMISSION_ENABLED
    }
}
