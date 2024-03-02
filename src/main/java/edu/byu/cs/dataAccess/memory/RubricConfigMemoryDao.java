package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.RubricConfigDao;
import edu.byu.cs.dataAccess.RubricConfigParser;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RubricConfigMemoryDao implements RubricConfigDao {

    private static final Map<RubricItemDescriptor, RubricConfig.RubricConfigItem> configItems = new HashMap<>();

    public RubricConfigMemoryDao() {
        Collection<RubricConfigParser.RubricConfigObject> fileData;
        try {
            fileData = RubricConfigParser.loadRubricConfigFile();
        } catch (FileNotFoundException e) {
            throw new DataAccessException("Failed to load rubric-config.json", e);
        }

        for (RubricConfigParser.RubricConfigObject configObject : fileData) {
            Phase phase = configObject.phase();
            Rubric.RubricType type = configObject.type();
            String criteria = configObject.criteria();
            String category = configObject.category();
            int points = configObject.points();

            RubricItemDescriptor descriptor = new RubricItemDescriptor(phase, type);
            RubricConfig.RubricConfigItem item = new RubricConfig.RubricConfigItem(category, criteria, points);

            configItems.put(descriptor, item);
        }
    }

    @Override
    public RubricConfig getRubricConfig(Phase phase) {
        RubricConfig.RubricConfigItem passoffTests = getRubricItem(phase, Rubric.RubricType.PASSOFF_TESTS);
        RubricConfig.RubricConfigItem unitTests = getRubricItem(phase, Rubric.RubricType.UNIT_TESTS);
        RubricConfig.RubricConfigItem quality = getRubricItem(phase, Rubric.RubricType.QUALITY);
        return new RubricConfig(phase, passoffTests, unitTests, quality);
    }

    @Override
    public int getPhaseTotalPossiblePoints(Phase phase) {
        // TODO extract duplicate code to a parent class?
        RubricConfig rubricConfig = getRubricConfig(phase);

        int total = 0;
        if (rubricConfig.passoffTests() != null) total += rubricConfig.passoffTests().points();
        if (rubricConfig.unitTests() != null) total += rubricConfig.unitTests().points();
        if (rubricConfig.quality() != null) total += rubricConfig.quality().points();

        return total;
    }

    private RubricConfig.RubricConfigItem getRubricItem(Phase phase, Rubric.RubricType type) {
        return configItems.get(new RubricItemDescriptor(phase, type));
    }

    record RubricItemDescriptor(Phase phase, Rubric.RubricType type) {
    }
}
