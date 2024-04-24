package edu.byu.cs.dataAccess;

import java.util.Map;

public interface ConfigurationDao {
    <T> void setConfiguration(String key, T value, Class<T> type);
    <T> T getConfiguration(String key, T value, Class<T> type);

    enum Configuration {
        STUDENT_SUBMISSION_ENABLED
    }
}
