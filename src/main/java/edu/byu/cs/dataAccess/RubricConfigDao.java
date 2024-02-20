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
    RubricConfig getRubricConfig(Phase phase);
}
