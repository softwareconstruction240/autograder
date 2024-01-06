package edu.byu.cs.dataAccess.sql;

import com.google.gson.Gson;
import edu.byu.cs.autograder.TestAnalyzer;
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
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                    INSERT INTO submission (net_id, repo_url, timestamp, phase, score, head_hash, results)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """);
            statement.setString(1, submission.netId());
            statement.setString(2, submission.repoUrl());
            statement.setTimestamp(3, Timestamp.from(submission.timestamp()));
            statement.setString(4, submission.phase().toString());
            statement.setFloat(5, submission.score());
            statement.setString(6, submission.headHash());
            statement.setString(7, new Gson().toJson(submission.testResults()));
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
                    SELECT net_id, repo_url, timestamp, phase, score, head_hash, results
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
                    SELECT net_id, repo_url, timestamp, phase, score, head_hash, results
                    FROM submission
                    WHERE net_id = ?
                    """);
            statement.setString(1, netId);
            return getSubmissionsFromQuery(statement);

        } catch (Exception e) {
            throw new DataAccessException("Error getting submissions", e);
        }
    }

    private Collection<Submission> getSubmissionsFromQuery(PreparedStatement statement) throws SQLException {
        ResultSet rows = statement.executeQuery();

        Collection<Submission> submissions = new ArrayList<>();
        while (rows.next()) {
            String netId = rows.getString("net_id");
            String repoUrl = rows.getString("repo_url");
            String headHash = rows.getString("head_hash");
            Instant timestamp = rows.getTimestamp("timestamp").toInstant();
            Phase phase = Phase.valueOf(rows.getString("phase"));
            float score = rows.getFloat("score");
            TestAnalyzer.TestNode results = new Gson().fromJson(rows.getString("results"), TestAnalyzer.TestNode.class);

            submissions.add(new Submission(
                    netId,
                    repoUrl,
                    headHash,
                    timestamp,
                    phase,
                    score,
                    results
            ));
        }

        return submissions;
    }
}
