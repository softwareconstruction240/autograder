package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.RepoUpdateDao;
import edu.byu.cs.dataAccess.sql.helpers.ColumnDefinition;
import edu.byu.cs.dataAccess.sql.helpers.SqlReader;
import edu.byu.cs.model.RepoUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;

public class RepoUpdateSqlDao implements RepoUpdateDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepoUpdateSqlDao.class);

    private final SqlReader<RepoUpdate> sqlReader = new SqlReader<RepoUpdate>(
            "repo_update", COLUMN_DEFINITIONS, RepoUpdateSqlDao::readUpdate);
    private static final ColumnDefinition[] COLUMN_DEFINITIONS = {
            new ColumnDefinition<RepoUpdate>("timestamp", s -> Timestamp.from(s.timestamp())),
            new ColumnDefinition<RepoUpdate>("net_id", RepoUpdate::netId),
            new ColumnDefinition<RepoUpdate>("repo_url", RepoUpdate::repoUrl),
            new ColumnDefinition<RepoUpdate>("admin_update", RepoUpdate::adminUpdate),
            new ColumnDefinition<RepoUpdate>("admin_net_id", RepoUpdate::adminNetId),
    };

    private static RepoUpdate readUpdate(ResultSet rs) throws SQLException {
        Instant timestamp = rs.getTimestamp("timestamp").toInstant();
        String netId = rs.getString("net_id");
        String repoUrl = rs.getString("repo_url");
        Boolean passed = rs.getBoolean("admin_update");
        String headHash = rs.getString("admin_net_id");

        return new RepoUpdate(timestamp, netId, repoUrl, passed, headHash);
    }


    @Override
    public void insertUpdate(RepoUpdate update) throws DataAccessException {
        sqlReader.insertItem(update);
    }

    @Override
    public Collection<RepoUpdate> getUpdatesForUser(String netId) throws DataAccessException {
        return sqlReader.executeQuery(
                "WHERE net_id = ?",
                ps -> ps.setString(1, netId)
        );
    }

    @Override
    public Collection<RepoUpdate> getUpdatesForRepo(String repoUrl) throws DataAccessException {
        return sqlReader.executeQuery(
                "WHERE repo_url = ?",
                ps -> ps.setString(1, repoUrl)
        );
    }
}
