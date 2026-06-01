package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

import java.util.EnumMap;
import java.util.Map;

public class RubricConfigMemoryDao implements RubricConfigDao {
    private final Map<Phase, RubricConfig> rubricConfigs = new EnumMap<>(Phase.class);
    @Override
    public RubricConfig getRubricConfig(Phase phase) {
        return rubricConfigs.get(phase);
    }

    @Override
    public void setRubricConfig(Phase phase, RubricConfig rubricConfig) {
        rubricConfigs.put(phase, rubricConfig);
    }

    @Override
    public void setRubricIdAndPoints(Phase phase, Rubric.RubricType type, Integer points, String rubric_id) {
        RubricConfig rubricConfig = rubricConfigs.get(phase);
        if (rubricConfig == null) {
            //RubricConfig.RubricConfigItem rubricConfigItem = new RubricConfig.RubricConfigItem("", "", points, rubric_id);
            EnumMap<Rubric.RubricType, RubricConfig.RubricConfigItem> items = new EnumMap<>(Rubric.RubricType.class);
            for (Rubric.RubricType t : Rubric.RubricType.values()){
                items.put(t, null);
            }
            rubricConfig = new RubricConfig(phase, items);
            rubricConfigs.put(phase, rubricConfig);
        }

        var rubricTypeToConfigItem = rubricConfig.items();
        RubricConfig.RubricConfigItem oldRubricConfigItem = rubricTypeToConfigItem.get(type);
        RubricConfig.RubricConfigItem newRubricConfigItem = null;
        if (oldRubricConfigItem != null){
            newRubricConfigItem = new RubricConfig.RubricConfigItem(
                oldRubricConfigItem.category(),
                oldRubricConfigItem.criteria(),
                points,
                rubric_id
            );
        }
        rubricTypeToConfigItem.put(type, newRubricConfigItem);
    }

}
