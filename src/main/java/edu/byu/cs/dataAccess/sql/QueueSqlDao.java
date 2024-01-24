package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.QueueDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.QueueItem;

import java.util.ArrayList;
import java.util.Collection;

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
                        resultSet.getTimestamp("timestamp").toInstant(),
                        resultSet.getTimestamp("time_started").toInstant()
                );
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new DataAccessException("Error popping item from queue", e);
        }
    }

    @Override
    public void remove(String netId) {
        try (var connection = SqlDb.getConnection()) {
            var statement = connection.prepareStatement(
                    """
                            DELETE FROM queue
                            WHERE net_id = ?
                            """);
            statement.setString(1, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error removing item from queue", e);
        }
    }

    @Override
    public Collection<QueueItem> getAll() {
        Collection<QueueItem> items = new ArrayList<>();
        try (var connection = SqlDb.getConnection()) {
            var statement = connection.prepareStatement(
                    """
                            SELECT *
                            FROM queue
                            """);
            var resultSet = statement.executeQuery();
            while (resultSet.next()) {
                items.add(new QueueItem(
                        resultSet.getString("net_id"),
                        Phase.valueOf(resultSet.getString("phase")),
                        resultSet.getTimestamp("timestamp").toInstant(),
                        resultSet.getTimestamp("time_started").toInstant()
                ));
            }
            return items;
        } catch (Exception e) {
            throw new DataAccessException("Error getting all items from queue", e);
        }
    }
}
