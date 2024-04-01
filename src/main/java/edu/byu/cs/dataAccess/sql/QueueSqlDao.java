package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.QueueDao;
import edu.byu.cs.dataAccess.sql.helpers.ColumnDefinition;
import edu.byu.cs.dataAccess.sql.helpers.SqlReader;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.QueueItem;
import edu.byu.cs.model.Submission;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

public class QueueSqlDao implements QueueDao {

    private static final ColumnDefinition[] COLUMN_DEFINITIONS = {
            new ColumnDefinition<QueueItem>("net_id", QueueItem::netId),
            new ColumnDefinition<QueueItem>("phase", q -> q.phase().name()),
            new ColumnDefinition<QueueItem>("started", QueueItem::started),
            new ColumnDefinition<QueueItem>("time_added", QueueItem::timeAdded),
    };
    private static QueueItem readQueueItem(ResultSet rs) throws SQLException {
        return new QueueItem(
                rs.getString("net_id"),
                Phase.valueOf(rs.getString("phase")),
                rs.getTimestamp("time_added").toInstant(),
                rs.getBoolean("started")
        );
    }

    private final SqlReader<QueueItem> sqlReader = new SqlReader<QueueItem>(
            "queue", COLUMN_DEFINITIONS, QueueSqlDao::readQueueItem);

    @Override
    public void add(QueueItem item) {
        sqlReader.insertItem(item);
    }

    @Override
    public QueueItem pop() {
        try (var connection = SqlDb.getConnection();
            var statement = connection.prepareStatement(
                    """
                            DELETE FROM %s
                            WHERE net_id = (
                                SELECT net_id
                                FROM %1$s
                                ORDER BY time_added
                                LIMIT 1
                            )
                            """.formatted(sqlReader.getTableName()))) {
            var topItems = sqlReader.readItems(statement);
            return topItems.isEmpty() ? null : topItems.iterator().next();
        } catch (Exception e) {
            throw new DataAccessException("Error popping item from queue", e);
        }
    }

    @Override
    public void remove(String netId) {
        try (var connection = SqlDb.getConnection();
            var statement = connection.prepareStatement(
                    """
                            DELETE FROM %s
                            WHERE net_id = ?
                            """.formatted(sqlReader.getTableName()))) {
            statement.setString(1, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error removing item from queue", e);
        }
    }

    @Override
    public Collection<QueueItem> getAll() {
        return sqlReader.executeQuery("");
    }

    @Override
    public boolean isAlreadyInQueue(String netId) {
        var results = sqlReader.executeQuery(
                "WHERE net_id = ?",
                ps -> ps.setString(1, netId));
        return !results.isEmpty();
    }

    @Override
    public void markStarted(String netId) {
        updatedStartedField(netId, true);
    }

    @Override
    public void markNotStarted(String netId) {
        updatedStartedField(netId, false);
    }

    private void updatedStartedField(String netId, boolean started) {
        try (var connection = SqlDb.getConnection();
             var statement = connection.prepareStatement(
                     """
                             UPDATE %s
                             SET started = ?
                             WHERE net_id = ?
                             """.formatted(sqlReader.getTableName()))) {
            statement.setBoolean(1, started);
            statement.setString(2, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error updating 'started' status", e);
        }
    }

    @Override
    public QueueItem get(String netId) {
        var results = sqlReader.executeQuery(
                "WHERE net_id = ?",
                ps -> ps.setString(1, netId));
        return results.isEmpty() ? null : results.iterator().next();
    }
}
