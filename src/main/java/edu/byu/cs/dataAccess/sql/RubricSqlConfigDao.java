package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RubricSqlConfigDao implements RubricConfigDao {
    @Override
    public RubricConfig getRubricConfig(Phase phase) {

        RubricConfig.RubricConfigItem passoffTests = getRubricItem(phase, Rubric.RubricType.PASSOFF_TESTS);
        RubricConfig.RubricConfigItem unitTests = getRubricItem(phase, Rubric.RubricType.UNIT_TESTS);
        RubricConfig.RubricConfigItem quality = getRubricItem(phase, Rubric.RubricType.QUALITY);

        return new RubricConfig(
                phase,
                passoffTests,
                unitTests,
                quality
        );
    }

    @Override
    public int getPhaseTotalPossiblePoints(Phase phase) {
        RubricConfig rubricConfig = getRubricConfig(phase);

        int total = 0;
        if (rubricConfig.passoffTests() != null)
            total += rubricConfig.passoffTests().points();
        if (rubricConfig.unitTests() != null)
            total += rubricConfig.unitTests().points();
        if (rubricConfig.quality() != null)
            total += rubricConfig.quality().points();

        return total;
    }

    private RubricConfig.RubricConfigItem getRubricItem(Phase phase, Rubric.RubricType type) throws DataAccessException {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM rubric_config WHERE phase = ? AND type = ?");
            statement.setString(1, phase.name());
            statement.setString(2, type.toString());
            ResultSet results = statement.executeQuery();
            if (results.next()) {
                return new RubricConfig.RubricConfigItem(
                        results.getInt("points"),
                        results.getString("description")
                );
            }
        } catch (Exception e) {
            throw new DataAccessException("Error getting rubric item", e);
        }

        return null;
    }
}
