package edu.byu.cs.dataAccess.sql;

import com.google.gson.Gson;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

public class SubmissionSqlDao implements SubmissionDao {
    @Override
    public void insertSubmission(Submission submission) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO submission (net_id, repo_url, timestamp, phase, passed, score, head_hash, num_commits, notes, results, rubric, admin)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
            statement.setString(1, submission.netId());
            statement.setString(2, submission.repoUrl());
            statement.setTimestamp(3, Timestamp.from(submission.timestamp()));
            statement.setString(4, submission.phase().toString());
            statement.setBoolean(5, submission.passed());
            statement.setFloat(6, submission.score());
            statement.setString(7, submission.headHash());
            statement.setInt(8, submission.numCommits());
            statement.setString(9, submission.notes());
            statement.setString(10, "{}");
            statement.setString(11, new Gson().toJson(submission.rubric()));
            statement.setBoolean(12, submission.admin());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error inserting submission", e);
        }
    }

    @Override
    public Collection<Submission> getSubmissionsForPhase(String netId, Phase phase) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    SELECT net_id, repo_url, timestamp, phase, passed, score, head_hash, num_commits, notes, rubric, admin
                    FROM submission
                    WHERE net_id = ? AND phase = ?
                    """);
            statement.setString(1, netId);
            statement.setString(2, phase.toString());
            return getSubmissionsFromQuery(statement);

        } catch (Exception e) {
            throw new DataAccessException("Error getting submissions", e);
        }
    }

    @Override
    public Collection<Submission> getSubmissionsForUser(String netId) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    SELECT net_id, repo_url, timestamp, phase, passed, score, head_hash, num_commits, notes, rubric, admin
                    FROM submission
                    WHERE net_id = ?
                    """);
            statement.setString(1, netId);
            return getSubmissionsFromQuery(statement);

        } catch (Exception e) {
            throw new DataAccessException("Error getting submissions", e);
        }
    }

    @Override
    public Collection<Submission> getAllLatestSubmissions() {
        return getAllLatestSubmissions(-1);
    }

    @Override
    public Collection<Submission> getAllLatestSubmissions(int batchSize) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT net_id, repo_url, timestamp, phase, passed, score, head_hash, num_commits, notes, rubric, admin
                            FROM submission
                            WHERE timestamp IN (
                                SELECT MAX(timestamp)
                                FROM submission
                                GROUP BY net_id, phase
                            )
                            ORDER BY timestamp DESC
                            """ +
                            (batchSize >= 0 ? "LIMIT ?" : ""));
            if (batchSize >= 0) {
                statement.setInt(1, batchSize);
            }
            return getSubmissionsFromQuery(statement);

        } catch (Exception e) {
            throw new DataAccessException("Error getting submissions", e);
        }
    }

    @Override
    public void removeSubmissionsByNetId(String netId) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            DELETE FROM submission
                            WHERE net_id = ?
                            """)) {
            statement.setString(1, netId);
            statement.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("Error removing submissions", e);
        }
    }

    @Override
    public Submission getFirstPassingSubmission(String netId, Phase phase) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT net_id, repo_url, timestamp, phase, passed, score, head_hash, num_commits, notes, rubric, admin
                            FROM submission
                            WHERE net_id = ? AND phase = ? AND passed = 1
                            ORDER BY timestamp
                            LIMIT 1
                            """);
            statement.setString(1, netId);
            statement.setString(2, phase.toString());
            Collection<Submission> submissions = getSubmissionsFromQuery(statement);
            return submissions.isEmpty() ? null : submissions.iterator().next();
        } catch (Exception e) {
            throw new DataAccessException("Error getting first passing submission", e);
        }
    }

    @Override
    public float getBestScoreForPhase(String netId, Phase phase) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT max(score) as highestScore
                            FROM submission
                            WHERE net_id = ? AND phase = ?
                            """)) {
            statement.setString(1, netId);
            statement.setString(2, phase.toString());
            try(ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getFloat("highestScore");
            }
        } catch (Exception e) {
            throw new DataAccessException("Error getting highest score", e);
        }
    }

    private Collection<Submission> getSubmissionsFromQuery(PreparedStatement statement) throws SQLException {
        try(ResultSet rows = statement.executeQuery()) {

            Collection<Submission> submissions = new ArrayList<>();
            while (rows.next()) {
                String netId = rows.getString("net_id");
                String repoUrl = rows.getString("repo_url");
                String headHash = rows.getString("head_hash");
                Instant timestamp = rows.getTimestamp("timestamp").toInstant();
                Phase phase = Phase.valueOf(rows.getString("phase"));
                Boolean passed = rows.getBoolean("passed");
                float score = rows.getFloat("score");
                Integer numCommits = rows.getInt("num_commits");
                String notes = rows.getString("notes");
                Rubric rubric = new Gson().fromJson(rows.getString("rubric"), Rubric.class);
                Boolean admin = rows.getBoolean("admin");

                submissions.add(
                        new Submission(netId, repoUrl, headHash, timestamp, phase, passed, score, numCommits, notes,
                                rubric, admin));
            }

            return submissions;
        }
    }
}
