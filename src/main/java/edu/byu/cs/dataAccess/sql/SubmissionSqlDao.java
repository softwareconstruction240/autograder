package edu.byu.cs.dataAccess.sql;

import com.google.gson.Gson;
import edu.byu.cs.dataAccess.sql.helpers.ColumnDefinition;
import edu.byu.cs.dataAccess.sql.helpers.SqlReader;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class SubmissionSqlDao implements SubmissionDao {
    private static final ColumnDefinition[] COLUMN_DEFINITIONS = {
            new ColumnDefinition<Submission>("net_id", Submission::netId),
            new ColumnDefinition<Submission>("repo_url", Submission::repoUrl),
            new ColumnDefinition<Submission>("timestamp", s -> Timestamp.from(s.timestamp())),
            new ColumnDefinition<Submission>("phase", s -> s.phase().toString()),
            new ColumnDefinition<Submission>("passed", Submission::passed),
            new ColumnDefinition<Submission>("score", Submission::score),
            new ColumnDefinition<Submission>("num_commits", Submission::numCommits),
            new ColumnDefinition<Submission>("head_hash", Submission::headHash),
            new ColumnDefinition<Submission>("notes", Submission::notes),
            new ColumnDefinition<Submission>("rubric", s -> new Gson().toJson(s.rubric())),
            new ColumnDefinition<Submission>("admin", Submission::admin)
    };

    private static Submission readSubmission(ResultSet rs) throws SQLException {
        String netId = rs.getString("net_id");
        String repoUrl = rs.getString("repo_url");
        String headHash = rs.getString("head_hash");
        Instant timestamp = rs.getTimestamp("timestamp").toInstant();
        Phase phase = Phase.valueOf(rs.getString("phase"));
        Boolean passed = rs.getBoolean("passed");
        float score = rs.getFloat("score");
        Integer numCommits = rs.getInt("num_commits");
        String notes = rs.getString("notes");
        Rubric rubric = new Gson().fromJson(rs.getString("rubric"), Rubric.class);
        Boolean admin = rs.getBoolean("admin");

        return new Submission(netId, repoUrl, headHash, timestamp, phase, passed, score, numCommits, notes, rubric, admin);
    }

    private final SqlReader<Submission> sqlReader = new SqlReader<Submission>(
            "submission", COLUMN_DEFINITIONS, SubmissionSqlDao::readSubmission);

    @Override
    public void insertSubmission(Submission submission) throws DataAccessException {
        sqlReader.insertItem(submission);
    }

    @Override
    public Collection<Submission> getSubmissionsForPhase(String netId, Phase phase) throws DataAccessException {
        return sqlReader.executeQuery(
                "WHERE net_id = ? AND phase = ?",
                ps -> {
                    ps.setString(1, netId);
                    ps.setString(2, phase.toString());
                }
        );
    }

    @Override
    public Collection<Submission> getSubmissionsForUser(String netId) throws DataAccessException {
        return sqlReader.executeQuery(
                "WHERE net_id = ?",
                ps -> ps.setString(1, netId));
    }

    @Override
    public Collection<Submission> getAllLatestSubmissions() throws DataAccessException {
        return getAllLatestSubmissions(-1);
    }

    @Override
    public Collection<Submission> getAllLatestSubmissions(int batchSize) throws DataAccessException {
        try (var connection = SqlDb.getConnection()) {
            var statement = connection.prepareStatement(
                    """
                            SELECT s.net_id, s.repo_url, s.timestamp, s.phase, s.passed, s.score, s.num_commits, s.head_hash, s.notes, s.rubric, s.admin
                            FROM submission s
                            INNER JOIN (
                                SELECT net_id, phase, MAX(timestamp) AS max_timestamp
                                FROM submission
                                GROUP BY net_id, phase
                            ) s2 ON s.net_id = s2.net_id AND s.phase = s2.phase AND s.timestamp = s2.max_timestamp
                            ORDER BY s2.max_timestamp DESC
                            """ +
                            (batchSize >= 0 ? "LIMIT ?" : "")
            );
            if (batchSize >= 0) {
                statement.setInt(1, batchSize);
            }
            try (var results = statement.executeQuery()) {
                List<Submission> submissions = new ArrayList<>();
                while (results.next()) {
                    Submission submission = readSubmission(results);
                    submissions.add(submission);
                }
                return submissions;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Error getting latest submissions", e);
        }
    }

    @Override
    public void removeSubmissionsByNetId(String netId) throws DataAccessException {
        sqlReader.executeUpdate(
                """
                        DELETE FROM %s
                        WHERE net_id = ?
                        """.formatted(sqlReader.getTableName()),
                ps -> ps.setString(1, netId)
        );
    }

    @Override
    public Submission getFirstPassingSubmission(String netId, Phase phase) throws DataAccessException {
        var submissions = sqlReader.executeQuery(
                """
                        WHERE net_id = ? AND phase = ? AND passed = 1
                        ORDER BY timestamp
                        LIMIT 1
                        """,
                ps -> {
                    ps.setString(1, netId);
                    ps.setString(2, phase.toString());
                }
        );
        return sqlReader.expectOneItem(submissions);
    }

    @Override
    public float getBestScoreForPhase(String netId, Phase phase) throws DataAccessException {
        return sqlReader.executeQuery(
                """
                        SELECT max(score) as highestScore
                        FROM %s
                        WHERE net_id = ? AND phase = ?
                        """.formatted(sqlReader.getTableName()),
                ps -> {
                    ps.setString(1, netId);
                    ps.setString(2, phase.toString());
                },
                rs -> {
                    rs.next();
                    return rs.getFloat("highestScore");
                }
        );
    }

}
