package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.PhaseConfigurationDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.PhaseConfiguration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZonedDateTime;

public class PhaseConfigurationSqlDao implements PhaseConfigurationDao {

    @Override
    public void modifyDueDate(Phase phase, ZonedDateTime dueDate) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            UPDATE phase_configuration
                            SET due_date_mountain_time = ?
                            WHERE phase = ?
                            """);
            statement.setObject(1, dueDate);
            statement.setString(2, phase.toString());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error modifying due date", e);
        }
    }

    @Override
    public PhaseConfiguration getPhaseConfiguration(Phase phase) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT phase, due_date_mountain_time
                            FROM phase_configuration
                            WHERE phase = ?
                            """);
            statement.setString(1, phase.toString());
            ResultSet results = statement.executeQuery();
            if (results.next()) {
                return new PhaseConfiguration(
                        Phase.valueOf(results.getString("phase")),
                        results.getObject("due_date_mountain_time", ZonedDateTime.class)
                );
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new DataAccessException("Error getting phase configuration", e);
        }
    }
}
