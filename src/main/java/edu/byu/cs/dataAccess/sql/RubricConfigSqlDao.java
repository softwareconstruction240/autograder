package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RubricConfigSqlDao implements RubricConfigDao {

    // NOTE: This class skipped for conversion to SqlReader since it wouldn't improve readability of this file.
    // This file isn't heavy in SQl queries, but in the logic of joining together the results.

    @Override
    public RubricConfig getRubricConfig(Phase phase) throws DataAccessException {

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
    public int getPhaseTotalPossiblePoints(Phase phase) throws DataAccessException {
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

    @Override
    public void setRubricConfig(Phase phase, RubricConfig rubricConfig) throws DataAccessException {
        if (rubricConfig.passoffTests() != null)
            addRubricConfigItem(phase, Rubric.RubricType.PASSOFF_TESTS, rubricConfig.passoffTests());
        if (rubricConfig.unitTests() != null)
            addRubricConfigItem(phase, Rubric.RubricType.UNIT_TESTS, rubricConfig.unitTests());
        if (rubricConfig.quality() != null)
            addRubricConfigItem(phase, Rubric.RubricType.QUALITY, rubricConfig.quality());
    }

    private void addRubricConfigItem(Phase phase, Rubric.RubricType type, RubricConfig.RubricConfigItem item) throws DataAccessException {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO rubric_config (phase, type, category, criteria, points, rubric_id) VALUES (?, ?, ?, ?, ?, ?)");
            statement.setString(1, phase.name());
            statement.setString(2, type.toString());
            statement.setString(3, item.category());
            statement.setString(4, item.criteria());
            statement.setInt(5, item.points());
            statement.setString(6, item.rubricId());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error getting rubric item", e);
        }
    }

    private RubricConfig.RubricConfigItem getRubricItem(Phase phase, Rubric.RubricType type) throws DataAccessException {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM rubric_config WHERE phase = ? AND type = ?")) {
            statement.setString(1, phase.name());
            statement.setString(2, type.toString());
            try(ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return new RubricConfig.RubricConfigItem(results.getString("category"),
                            results.getString("criteria"), results.getInt("points"), results.getString("rubric_id"));
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("Error getting rubric item", e);
        }

        return null;
    }
}
