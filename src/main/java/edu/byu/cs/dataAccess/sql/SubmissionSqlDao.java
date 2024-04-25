package edu.byu.cs.dataAccess.sql;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.byu.cs.dataAccess.ItemNotFoundException;
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
            new ColumnDefinition<Submission>("head_hash", Submission::headHash),
            new ColumnDefinition<Submission>("notes", Submission::notes),
            new ColumnDefinition<Submission>("rubric", s -> new Gson().toJson(s.rubric())),
            new ColumnDefinition<Submission>("admin", Submission::admin),
            new ColumnDefinition<Submission>("verified_status", Submission::serializeVerifiedStatus),
            new ColumnDefinition<Submission>("verification", Submission::serializeScoreVerification)
    };

    private static Submission readSubmission(ResultSet rs) throws SQLException {
        var gson = new Gson();

        String netId = rs.getString("net_id");
        String repoUrl = rs.getString("repo_url");
        String headHash = rs.getString("head_hash");
        Instant timestamp = rs.getTimestamp("timestamp").toInstant();
        Phase phase = Phase.valueOf(rs.getString("phase"));
        Boolean passed = rs.getBoolean("passed");
        float score = rs.getFloat("score");
        String notes = rs.getString("notes");
        Rubric rubric = gson.fromJson(rs.getString("rubric"), Rubric.class);
        Boolean admin = rs.getBoolean("admin");

        String verifiedStatusStr = rs.getString("verified_status");
        Submission.VerifiedStatus verifiedStatus = verifiedStatusStr == null ? null :
                Submission.VerifiedStatus.valueOf(verifiedStatusStr);
        String verificationJson = rs.getString("verification");
        Submission.ScoreVerification scoreVerification = verificationJson == null ? null :
                gson.fromJson(verificationJson, Submission.ScoreVerification.class);

        return new Submission(
                netId, repoUrl, headHash, timestamp, phase,
                passed, score, notes, rubric,
                admin, verifiedStatus, scoreVerification);
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
                    float highestScore = rs.getFloat("highestScore");
                    return rs.wasNull() ? -1.0f : highestScore;
                }
        );
    }

    @Override
    public Collection<Submission> getAllPassingSubmissions(String netId) throws DataAccessException {
        return sqlReader.executeQuery(
                "WHERE net_id = ? AND passed = ?",
                ps -> {
                    ps.setString(1, netId);
                    ps.setBoolean(2, true);
                });
    }

    @Override
    public void manuallyApproveSubmission(Submission submission, Float newScore, Submission.ScoreVerification scoreVerification)
            throws ItemNotFoundException, DataAccessException {
        // Identify a submission by its: head_hash, net_id, and phase.
        // We could try to identify it by more items, but that touches on being too brittle.

        String netId = submission.netId();
        String headHash = submission.headHash();
        String phase = submission.phase().name();

        String whereClause = "WHERE net_id = ? AND head_hash = ? AND phase = ?";
        String verifiedStatusStr = Submission.serializeVerifiedStatus(Submission.VerifiedStatus.ApprovedManually);
        String verificationStr = Submission.serializeScoreVerification(scoreVerification);

        // First verify that we can identify it
        var matchingSubmissions = sqlReader.executeQuery(
                whereClause,
                ps -> {
                    // Careful! This code can't be shared since the parameter indices are different below
                    ps.setString(1, netId);
                    ps.setString(2, headHash);
                    ps.setString(3, phase);
                }
        );
        if (matchingSubmissions.size() != 1) {
            throw new ItemNotFoundException(
                    "Submission could not be identified. Found %s matches. Searched with the following information:\n  "
                            .formatted(matchingSubmissions.size())
                    + "  net_id: %s\n  phase: %s\n  head_hash: %s\n  "
                            .formatted(netId, headHash, phase)
                    + matchingSubmissions.toString()
            );
        }

        // Then update it
        sqlReader.executeUpdate(
                """
                        UPDATE %s
                        SET score = ?, verified_status = ?, verification = ?
                        %s
                        """.formatted(sqlReader.getTableName(), whereClause),
                ps -> {
                    ps.setFloat(1, newScore);
                    ps.setString(2, verifiedStatusStr);
                    ps.setString(3, verificationStr);

                    // Careful! This code is used first up above
                    ps.setString(4, netId);
                    ps.setString(5, headHash);
                    ps.setString(6, phase);
                }
        );
    }

}
