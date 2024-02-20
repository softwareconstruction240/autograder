package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.canvas.Rubric;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.RubricConfig;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class RubricSqlConfigDao implements RubricConfigDao {
    @Override
    public RubricConfig getRubricConfig(Phase phase) {

        RubricConfig.RubricConfigItem passoffTests = getRubricItem(phase, Rubric.RubricType.PASSOFF_TESTS);
        RubricConfig.RubricConfigItem unitTests = getRubricItem(phase, Rubric.RubricType.UNIT_TESTS);
        RubricConfig.RubricConfigItem quality = getRubricItem(phase, Rubric.RubricType.QUALITY);

        List<RubricConfig.RubricConfigItem> rubricItems = new ArrayList<>();

        if (passoffTests != null)
            rubricItems.add(passoffTests);

        if (unitTests != null)
            rubricItems.add(unitTests);

        if (quality != null)
            rubricItems.add(quality);

        return new RubricConfig(rubricItems);
    }

    private RubricConfig.RubricConfigItem getRubricItem(Phase phase, Rubric.RubricType type) throws DataAccessException {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM rubric_config WHERE phase = ? AND type = ?");
            statement.setString(1, phase.name());
            statement.setString(2, type.toString());
            ResultSet results = statement.executeQuery();
            if (results.next()) {
                return new RubricConfig.RubricConfigItem(
                        phase,
                        type,
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
