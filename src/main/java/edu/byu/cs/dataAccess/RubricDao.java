package edu.byu.cs.dataAccess;

import edu.byu.cs.canvas.Rubric;
import edu.byu.cs.model.Phase;

public interface RubricDao {

    /**
     * Gets the rubric for the given phase
     *
     * @param phase the phase of the rubric
     * @return the rubric for the given phase
     */
    Rubric getRubric(Phase phase);
}
