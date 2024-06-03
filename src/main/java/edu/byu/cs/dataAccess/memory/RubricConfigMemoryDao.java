package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.RubricConfig;

import java.util.HashMap;
import java.util.Map;

public class RubricConfigMemoryDao implements RubricConfigDao {
    private final Map<Phase, RubricConfig> rubricConfigs = new HashMap<>();
    @Override
    public RubricConfig getRubricConfig(Phase phase) {
        return rubricConfigs.get(phase);
    }

    @Override
    public void setRubricConfig(Phase phase, RubricConfig rubricConfig) {
        rubricConfigs.put(phase, rubricConfig);
    }
}
