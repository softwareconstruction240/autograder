package edu.byu.cs.dataAccess;

import edu.byu.cs.model.Phase;
import edu.byu.cs.model.PhaseConfiguration;

import java.time.ZonedDateTime;

public interface PhaseConfigurationDao {
    /**
     * Sets the due date for a phase
     *
     * @param phase   the phase to set the due date for
     * @param dueDate the due date to set
     */
    void modifyDueDate(Phase phase, ZonedDateTime dueDate);

    /**
     * Returns the configuration for a phase
     *
     * @param phase the phase to get the configuration for
     * @return the configuration for the phase
     */
    PhaseConfiguration getPhaseConfiguration(Phase phase);
}
