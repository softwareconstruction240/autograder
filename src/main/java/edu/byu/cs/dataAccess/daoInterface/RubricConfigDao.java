package edu.byu.cs.dataAccess.daoInterface;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

/**
 * A data access object interface for {@link RubricConfig} objects to maintain the configuration
 * for rubric items for a particular phase
 */
public interface RubricConfigDao {

    /**
     * Gets the rubric for the given phase
     *
     * @param phase the phase of the rubric
     * @return the rubric for the given phase
     */
    RubricConfig getRubricConfig(Phase phase) throws DataAccessException;

    default int getPhaseTotalPossiblePoints(Phase phase) throws DataAccessException {
        RubricConfig rubricConfig = getRubricConfig(phase);
        int total = 0;
        for(RubricConfig.RubricConfigItem item : rubricConfig.items().values()) {
            if(item != null) {
                total += item.points();
            }
        }
        return total;
    }

    void setRubricConfig(Phase phase, RubricConfig rubricConfig) throws DataAccessException;

    void setRubricIdAndPoints(Phase phase, Rubric.RubricType type, Integer points, String rubric_id) throws DataAccessException;
}
