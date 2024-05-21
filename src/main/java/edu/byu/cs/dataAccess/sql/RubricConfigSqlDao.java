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

        // Use the same `gitCommits` rubric item for all phases.
        // Currently, it's defined on the `Quality` phase because that's the closest
        // I could get to a global property. Picking any particular phase doesn't seem appropriate,
        // and I didn't want to duplicate the table entries for each phase.
        // If we want to change it from phase to phase, we would need to change this going forward.
        // However, with configuration as it stands, it is sufficiently general that it shouldn't require
        // adjusting or varying in the future.
        RubricConfig.RubricConfigItem gitCommits = getRubricItem(Phase.Commits, Rubric.RubricType.GIT_COMMITS);

        return new RubricConfig(
                phase,
                passoffTests,
                unitTests,
                quality,
                gitCommits
        );
    }

    @Override
    public void setRubricConfig(Phase phase, RubricConfig rubricConfig) throws DataAccessException {
        if (rubricConfig.passoffTests() != null)
            addRubricConfigItem(phase, Rubric.RubricType.PASSOFF_TESTS, rubricConfig.passoffTests().category(), rubricConfig.passoffTests().criteria(), rubricConfig.passoffTests().points());
        if (rubricConfig.unitTests() != null)
            addRubricConfigItem(phase, Rubric.RubricType.UNIT_TESTS, rubricConfig.unitTests().category(), rubricConfig.unitTests().criteria(), rubricConfig.unitTests().points());
        if (rubricConfig.quality() != null)
            addRubricConfigItem(phase, Rubric.RubricType.QUALITY, rubricConfig.quality().category(), rubricConfig.quality().criteria(), rubricConfig.quality().points());
    }

    private void addRubricConfigItem(Phase phase, Rubric.RubricType type, String category, String criteria, int points) throws DataAccessException {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO rubric_config (phase, type, category, criteria, points) VALUES (?, ?, ?, ?, ?)");
            statement.setString(1, phase.name());
            statement.setString(2, type.toString());
            statement.setString(3, category);
            statement.setString(4, criteria);
            statement.setInt(5, points);
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
                            results.getString("criteria"), results.getInt("points"));
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("Error getting rubric item", e);
        }

        return null;
    }
}
