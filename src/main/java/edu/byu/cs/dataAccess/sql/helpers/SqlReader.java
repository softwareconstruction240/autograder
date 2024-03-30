package edu.byu.cs.dataAccess.sql.helpers;

import com.google.gson.Gson;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.sql.SqlDb;
import edu.byu.cs.dataAccess.sql.SubmissionSqlDao;
import edu.byu.cs.model.Submission;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.sql.Types.NULL;

public class SqlReader <T> {
    /** Represents the name of our SQL table */
    private final String TABLE_NAME;
    /**
     * Represents all the columns in the table.
     * <br>
     * If this value changes, remember to <strong>both</strong>
     * the {@link SqlReader#insertItem(Object)} method <i>and</i>
     * the {@link SqlReader#getSubmissionsFromQuery(PreparedStatement)}.
     * */
    private final ColumnDefinition<T>[] COLUMN_DEFINITIONS;

    private final String[] ALL_COLUMN_NAMES;
    private final String ALL_COLUMN_NAMES_STMT;
    private final String VALUE_INSERT_WILDCARDS;

    /**
     * Represents a convenient beginning of most queries.
     * Usually, you will not want to use this alone, but will want to add
     * conditional <code>WHERE</code> clauses and other related
     * */
    private final String SELECT_ALL_COLUMNS_STMT;

    public SqlReader(String tableName, ColumnDefinition<T>[] columnDefinitions) {
        this.TABLE_NAME = tableName;
        this.COLUMN_DEFINITIONS = columnDefinitions;
        this.ALL_COLUMN_NAMES = Arrays.stream(columnDefinitions)
                .map(ColumnDefinition::columnName).toArray(String[]::new);

        // Several pre-constructed statement fragments
        this.ALL_COLUMN_NAMES_STMT = String.join(", ", ALL_COLUMN_NAMES);
        this.VALUE_INSERT_WILDCARDS = "?, ".repeat(ALL_COLUMN_NAMES.length);
        this.SELECT_ALL_COLUMNS_STMT = "SELECT " + ALL_COLUMN_NAMES_STMT + " FROM " + TABLE_NAME + " ";
    }

    public void insertItem(T item) {

        Map<String, Integer> colIndices = prepareWildcardIndices(COLUMN_DEFINITIONS);
        String statement = "INSERT INTO %s (%s) VALUES (%s)"
                .formatted(TABLE_NAME, ALL_COLUMN_NAMES_STMT, VALUE_INSERT_WILDCARDS);

        try (var connection = SqlDb.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(statement)
        ) {
            int colIndex;
            for (var colDef : COLUMN_DEFINITIONS) {
                colIndex = colIndices.get(colDef.columnName());
                setValue(preparedStatement, colIndex, item, colDef);
            }

            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error inserting value", e);
        }
    }


    private Map<String, Integer> prepareWildcardIndices(ColumnDefinition<T>[] columnDefinitions) {
        Map<String, Integer> out = new HashMap<>();

        String colName;
        for (int i = 0; i < columnDefinitions.length; i++) {
            colName = columnDefinitions[i].columnName();
            out.put(colName, i + 1); // SQL is 1-index, but java is 0-indexed
        }

        return out;
    }

    private void setValue(PreparedStatement ps, int wildcardIndex, T item, ColumnDefinition<T> columnDefinition) throws SQLException {
        Object value = columnDefinition.accessor().getValue(item);

        // This represents all the supported column types
        if (value == null) ps.setNull(wildcardIndex, NULL);
        else if (value instanceof String v) ps.setString(wildcardIndex, v);
        else if (value instanceof Integer v) ps.setInt(wildcardIndex, v);
        else if (value instanceof Float v) ps.setFloat(wildcardIndex, v);
        else if (value instanceof Boolean v) ps.setBoolean(wildcardIndex, v);
        else if (value instanceof Timestamp v) ps.setTimestamp(wildcardIndex, v);
        else if (value instanceof Object v) ps.setObject(wildcardIndex, v);
        else throw new RuntimeException("Unsupported type of value: " + value);
    }

    public String selectAllStmt() {
        return SELECT_ALL_COLUMNS_STMT;
    }
    public String selectAllStmt(String additionalClauses) {
        return SELECT_ALL_COLUMNS_STMT + additionalClauses;
    }
}
