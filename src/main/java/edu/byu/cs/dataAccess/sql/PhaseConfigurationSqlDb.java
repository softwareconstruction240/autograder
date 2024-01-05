package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.PhaseConfigurationDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.PhaseConfiguration;

import java.time.ZonedDateTime;

public class PhaseConfigurationSqlDb implements PhaseConfigurationDao {

    @Override
    public void modifyDueDate(Phase phase, ZonedDateTime dueDate) {

    }

    @Override
    public PhaseConfiguration getPhaseConfiguration(Phase phase) {
        return null;
    }
}
