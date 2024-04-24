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

    int getPhaseTotalPossiblePoints(Phase phase) throws DataAccessException;

    void setRubricConfig(Phase phase, RubricConfig rubricConfig) throws DataAccessException;
}
