package edu.byu.cs.dataAccess;

public interface ConfigurationDao {
    <T> void setConfiguration(Configuration key, T value, Class<T> type) throws DataAccessException;
    <T> T getConfiguration(Configuration key, Class<T> type) throws DataAccessException;

    enum Configuration {
        STUDENT_SUBMISSIONS_ENABLED,
        BANNER_MESSAGE,
        GITHUB_ASSIGNMENT_NUMBER,
        PHASE0_ASSIGNMENT_NUMBER,
        PHASE1_ASSIGNMENT_NUMBER,
        PHASE3_ASSIGNMENT_NUMBER,
        PHASE4_ASSIGNMENT_NUMBER,
        PHASE5_ASSIGNMENT_NUMBER,
        PHASE6_ASSIGNMENT_NUMBER,
        COURSE_NUMBER
    }
}
