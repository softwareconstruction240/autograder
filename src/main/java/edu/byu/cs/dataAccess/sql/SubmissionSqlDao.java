package edu.byu.cs.dataAccess.sql;

import com.google.gson.Gson;
import edu.byu.cs.dataAccess.sql.helpers.ColumnDefinition;
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
import java.util.*;

import static java.sql.Types.NULL;

public class SubmissionSqlDao implements SubmissionDao {
    /** Represents the name of our SQL table */
    private static final String TABLE_NAME = "submission";
    /**
     * Represents all the columns in the table.
     * <br>
     * If this value changes, remember to <strong>both</strong>
     * the {@link SubmissionSqlDao#insertSubmission(Submission)} method <i>and</i>
     * the {@link SubmissionSqlDao#getSubmissionsFromQuery(PreparedStatement)}.
     * */
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

    /**
     * Represents a convenient beginning of most queries.
     * Usually, you will not want to use this alone, but will want to add
     * conditional <code>WHERE</code> clauses and other related
     * */
    private static final String[] ALL_COLUMN_NAMES = Arrays.stream(COLUMN_DEFINITIONS)
            .map(ColumnDefinition::columnName).toArray(String[]::new);
    private static final String ALL_COLUMN_NAMES_STMT = String.join(", ", ALL_COLUMN_NAMES);
    private static final String SELECT_ALL_COLUMNS_STMT = "SELECT " + ALL_COLUMN_NAMES_STMT + " FROM " + TABLE_NAME + " ";

    @Override
    public void insertSubmission(Submission submission) {
        String valueInsertWildcards = "?, ".repeat(ALL_COLUMN_NAMES.length);
        Map<String, Integer> colIndices = prepareWildcardIndices(COLUMN_DEFINITIONS);
        String statement = "INSERT INTO submission (%s) VALUES (%s)"
                .formatted(ALL_COLUMN_NAMES_STMT, valueInsertWildcards);

        try (var connection = SqlDb.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(statement)
        ) {
            int colIndex;
            for (var colDef : COLUMN_DEFINITIONS) {
                colIndex = colIndices.get(colDef.columnName());
                setValue(preparedStatement, colIndex, submission, colDef);
            }

            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error inserting value", e);
        }
    }
    private Map<String, Integer> prepareWildcardIndices(ColumnDefinition[] columnDefinitions) {
        Map<String, Integer> out = new HashMap<>();

        String colName;
        for (int i = 0; i < columnDefinitions.length; i++) {
            colName = columnDefinitions[i].columnName();
            out.put(colName, i + 1);
        }

        return out;
    }
    private <T> void setValue(PreparedStatement ps, int wildcardIndex, T item, ColumnDefinition<T> columnDefinition) throws SQLException {
        Object value = columnDefinition.accessor().getValue(item);

        if (value == null) ps.setNull(wildcardIndex, NULL);
        else if (value instanceof String v) ps.setString(wildcardIndex, v);
        else if (value instanceof Integer v) ps.setInt(wildcardIndex, v);
        else if (value instanceof Float v) ps.setFloat(wildcardIndex, v);
        else if (value instanceof Boolean v) ps.setBoolean(wildcardIndex, v);
        else if (value instanceof Timestamp v) ps.setTimestamp(wildcardIndex, v);
        else if (value instanceof Object v) ps.setObject(wildcardIndex, v);
        else throw new RuntimeException("Unsupported type of value: " + value);
    }

    @Override
    public Collection<Submission> getSubmissionsForPhase(String netId, Phase phase) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    SELECT_ALL_COLUMNS_STMT + "WHERE net_id = ? AND phase = ?");
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
                    SELECT_ALL_COLUMNS_STMT + "WHERE net_id = ?");
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
                    SELECT_ALL_COLUMNS_STMT +
                    """
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
                    SELECT_ALL_COLUMNS_STMT +
                    """
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
