package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.QueueDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.QueueItem;

public class QueueSqlDao implements QueueDao {
    @Override
    public void add(QueueItem item) {
        try (var connection = SqlDb.getConnection()) {
            var statement = connection.prepareStatement(
                    """
                            INSERT INTO queue (net_id, phase, time_added)
                            VALUES (?, ?, ?)
                            """);
            statement.setString(1, item.netId());
            statement.setString(2, item.phase().name());
            statement.setObject(3, item.timeAdded());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error adding item to queue", e);
        }
    }

    @Override
    public QueueItem pop() {
        try (var connection = SqlDb.getConnection()) {
            var statement = connection.prepareStatement(
                    """
                            DELETE FROM queue
                            WHERE net_id = (
                                SELECT net_id
                                FROM queue
                                ORDER BY time_added
                                LIMIT 1
                            )
                            """);
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new QueueItem(
                        resultSet.getString("net_id"),
                        Phase.valueOf(resultSet.getString("phase")),
                        resultSet.getTimestamp("timestamp").toInstant()
                );
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new DataAccessException("Error popping item from queue", e);
        }
    }
}
