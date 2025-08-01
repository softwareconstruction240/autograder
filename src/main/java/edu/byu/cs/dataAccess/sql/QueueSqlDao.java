package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.dataAccess.sql.helpers.ColumnDefinition;
import edu.byu.cs.dataAccess.sql.helpers.SqlReader;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.QueueItem;

import java.sql.ResultSet;
import java.sql.SQLException;
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
    public void add(QueueItem item) throws DataAccessException {
        sqlReader.insertItem(item);
    }

    @Override
    public void remove(String netId) throws DataAccessException {
        sqlReader.executeUpdate(
                """
                    DELETE FROM %s
                    WHERE net_id = ?
                    """.formatted(sqlReader.getTableName()),
                ps -> ps.setString(1, netId)
        );
    }

    @Override
    public Collection<QueueItem> getAll() throws DataAccessException {
        return sqlReader.executeQuery("ORDER BY time_added DESC");
    }

    @Override
    public boolean isAlreadyInQueue(String netId) throws DataAccessException {
        var results = sqlReader.executeQuery(
                "WHERE net_id = ?",
                ps -> ps.setString(1, netId));
        return !results.isEmpty();
    }

    @Override
    public void markStarted(String netId) throws DataAccessException {
        updatedStartedField(netId, true);
    }

    @Override
    public void markNotStarted(String netId) throws DataAccessException {
        updatedStartedField(netId, false);
    }

    private void updatedStartedField(String netId, boolean started) throws DataAccessException {
        sqlReader.executeUpdate(
                """
                     UPDATE %s
                     SET started = ?
                     WHERE net_id = ?
                     """.formatted(sqlReader.getTableName()),
                ps -> {
                    ps.setBoolean(1, started);
                    ps.setString(2, netId);
                }
        );
    }

    @Override
    public QueueItem get(String netId) throws DataAccessException {
        var results = sqlReader.executeQuery(
                "WHERE net_id = ?",
                ps -> ps.setString(1, netId));
        return sqlReader.expectOneItem(results);
    }
}
