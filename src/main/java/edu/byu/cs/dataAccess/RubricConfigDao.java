package edu.byu.cs.dataAccess;

import edu.byu.cs.model.Phase;
import edu.byu.cs.model.RubricConfig;

public interface RubricConfigDao {

    /**
     * Gets the rubric for the given phase
     *
     * @param phase the phase of the rubric
     * @return the rubric for the given phase
     */
    RubricConfig getRubricConfig(Phase phase) throws DataAccessException;

    default int getTotalPossiblePoints(Phase phase) throws DataAccessException {
        RubricConfig rubricConfig = getRubricConfig(phase);
        return getTotalPossiblePoints(rubricConfig);
    }
    default int getTotalPossiblePoints(RubricConfig rubricConfig) {
        int total = 0;
        for (var configItem : rubricConfig.allConfigItems()) {
            if (configItem == null) continue;
            total += configItem.points();
        }

        return total;
    }

    void setRubricConfig(Phase phase, RubricConfig rubricConfig) throws DataAccessException;
}
