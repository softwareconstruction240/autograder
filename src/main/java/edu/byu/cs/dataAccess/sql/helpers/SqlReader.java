package edu.byu.cs.dataAccess.sql.helpers;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.sql.SqlDb;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Supplier;

import static java.sql.Types.NULL;

public class SqlReader <T> {
    /** Represents the name of our SQL table */
    private final String TABLE_NAME;
    /** Represents all the columns in the table. */
    private final ColumnDefinition<T>[] COLUMN_DEFINITIONS;
    private final String[] ALL_COLUMN_NAMES;
    private final ItemBuilder<T> ITEM_BUILDER;

    private final String allColumnNamesStmt;
    private final String selectAllColumnsStmt;


    private final String insertStatement;
    private final Map<String, Integer> insertWildCardIndexPositions;

    /**
     * <h1>{@link SqlReader} Class</h1>
     * This helper class can be used by DAO's to handle the most repetitive parts
     * of interacting with tables.
     * <h2>Provides</h2>
     * <ul>
     *     <li>Helpful segments of SQL queries that can be easily appended with additional clauses.</li>
     *     <li>Methods to easily execute a query and read out all results.</li>
     * </ul>
     * <h2>Usage Notes</h2>
     * <p>
     *     This class can be configured once upon construction
     *     of the DAO, and then efficiently prepares the information for repeated use later on.
     * </p>
     * <p>
     *     Constructing the array of {@link ColumnDefinition}'s is difficult because java
     *     complains about "Generic Array Construction." To resolve this issue, remove the type
     *     parameter from the <code>ColumnDefinition</code> to make it look like the example below.
     *     This results in a "Raw use of parameterized class" warning, but it won't cause an error.
     * </p>
     * <p>
     *     We wish we could handle the writing and the reading of the values auto-magically
     *     for you, but the authors of this class lacked the understanding of Java's <code>Reflection</code>
     *     system to make the type safety work properly. Ideally, the {@link ItemBuilder<T>} method
     *     would not be required and we would be able to figured it out based on the other information
     *     provided. If you are able to solve this issue, <b>please do!</b>
     * </p>
     *
     * @param tableName The case-sensitive name of the table to interact with.
     * @param columnDefinitions Identifies each of the columns in the table and provides a value accessor
     * @param itemBuilder Responsible for reading a single row of the result set and constructing an item
     */
    public SqlReader(String tableName, ColumnDefinition<T>[] columnDefinitions, ItemBuilder<T> itemBuilder) {
        this.TABLE_NAME = tableName;
        this.COLUMN_DEFINITIONS = columnDefinitions;
        this.ALL_COLUMN_NAMES = Arrays.stream(columnDefinitions)
                .map(ColumnDefinition::columnName).toArray(String[]::new);
        this.ITEM_BUILDER = itemBuilder;

        // Several pre-constructed statement fragments
        this.allColumnNamesStmt = String.join(", ", ALL_COLUMN_NAMES);
        this.selectAllColumnsStmt = "SELECT " + allColumnNamesStmt + " FROM " + TABLE_NAME + " ";

        this.insertStatement = buildInsertStatement();
        this.insertWildCardIndexPositions = this.prepareWildcardIndices(columnDefinitions);
    }

    private String buildInsertStatement() {
        String valueWildcards =  "?, ".repeat(ALL_COLUMN_NAMES.length);
        return "INSERT INTO %s (%s) VALUES (%s)"
                .formatted(TABLE_NAME, allColumnNamesStmt, valueWildcards);
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


    /**
     * Will insert an item into the database using the settings configured
     * when constructing this {@link SqlReader}.
     *
     * @param item The item to add to the table.
     */
    public void insertItem(T item) {
        // CONSIDER: We could prepare the statement a single time, and avoid rebuilding it.
        try (var connection = SqlDb.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertStatement)
        ) {
            int colIndex;
            for (var colDef : COLUMN_DEFINITIONS) {
                colIndex = insertWildCardIndexPositions.get(colDef.columnName());
                setValue(preparedStatement, colIndex, item, colDef);
            }

            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error inserting value", e);
        }
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


    /**
     * Executes the provided {@link PreparedStatement} and returns the results in a collection.
     * This overload uses the preconfigured {@link ItemBuilder<T>} and a default collection.
     *
     * @see SqlReader#readItems(PreparedStatement, ItemBuilder, Supplier) for more dynamic customizability.
     *
     * @param statement The statement to execute
     * @return A collection of items
     * @throws SQLException When SQL has an issue.
     */
    public Collection<T> readItems(PreparedStatement statement) throws SQLException {
        return readItems(statement, ITEM_BUILDER, ArrayList::new);
    }
    /**
     * Executes a given {@link PreparedStatement},
     * and then reads out the results and returns them in a collection.
     *
     * @param statement A prepared statement to execute
     * @param itemBuilder The method that will read each item individually
     * @param targetCollection The constructor of a {@link Collection} which will hold the items.
     * @return A collection containing all the constructed items from the results
     * @throws SQLException When SQL throws an error
     */
    public <T1> Collection<T1> readItems(
            PreparedStatement statement,
            ItemBuilder<T1> itemBuilder,
            Supplier<Collection<T1>> targetCollection
            ) throws SQLException {
        try(ResultSet resultSet = statement.executeQuery()) {
            Collection<T1> items = targetCollection.get();
            while (resultSet.next()) {
                items.add(itemBuilder.readItem(resultSet));
            }
            return items;
        }
    }


    /**
     * Represents a convenient beginning of most queries.
     * Usually, you will not want to use this alone, but will want to add
     * conditional <code>WHERE</code> clauses and other related
     * */
    public String selectAllStmt() {
        return selectAllColumnsStmt;
    }

    /**
     * @see SqlReader#selectAllStmt()
     *
     * @param additionalClauses Additional SQL statements to add to the result
     * @return A joined SQL statement ready for preparation.
     */
    public String selectAllStmt(String additionalClauses) {
        return selectAllColumnsStmt + additionalClauses;
    }
}
