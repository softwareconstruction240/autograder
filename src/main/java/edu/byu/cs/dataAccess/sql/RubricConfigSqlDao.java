package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.EnumMap;
import java.util.Map;

public class RubricConfigSqlDao implements RubricConfigDao {

    // NOTE: This class skipped for conversion to SqlReader since it wouldn't improve readability of this file.
    // This file isn't heavy in SQl queries, but in the logic of joining together the results.

    @Override
    public RubricConfig getRubricConfig(Phase phase) throws DataAccessException {
        EnumMap<Rubric.RubricType, RubricConfig.RubricConfigItem> items = new EnumMap<>(Rubric.RubricType.class);
        for(Rubric.RubricType type : Rubric.RubricType.values()) {
            items.put(type, getRubricItem(phase, type));
        }
        return new RubricConfig(phase, items);
    }

    @Override
    public int getPhaseTotalPossiblePoints(Phase phase) throws DataAccessException {
        RubricConfig rubricConfig = getRubricConfig(phase);

        int total = 0;
        for(RubricConfig.RubricConfigItem item : rubricConfig.items().values()) {
            if(item != null) {
                total += item.points();
            }
        }

        return total;
    }

    @Override
    public void setRubricConfig(Phase phase, RubricConfig rubricConfig) throws DataAccessException {
        for (Map.Entry<Rubric.RubricType, RubricConfig.RubricConfigItem> entry : rubricConfig.items().entrySet()) {
            RubricConfig.RubricConfigItem item = entry.getValue();
            if (item != null) {
                addRubricConfigItem(phase, entry.getKey(), item.category(), item.criteria(), item.points());
            }
        }
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
