package edu.byu.cs.dataAccess.sql.helpers;

import edu.byu.cs.controller.SubmissionController;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.sql.SqlDb;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

import static java.sql.Types.NULL;

public class SqlReader <T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlReader.class);

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
     * @param columnDefinitions Identifies each of the columns in the table and provides a value accessor.
     *                          This list is used to <b>both</b> create insert items in the table <b>and</b>
     *                          to read out data from all columns.
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
        String valueWildcards = String.join(", ", Collections.nCopies(ALL_COLUMN_NAMES.length, "?"));
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
    public void insertItem(@NonNull T item) throws DataAccessException {
        // CONSIDER: We could prepare the statement a single time, and avoid rebuilding it.
        try (var connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertStatement)
        ) {
            int colIndex;
            for (var colDef : COLUMN_DEFINITIONS) {
                colIndex = insertWildCardIndexPositions.get(colDef.columnName());
                setValue(preparedStatement, colIndex, item, colDef);
            }

            preparedStatement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error inserting item into table " + TABLE_NAME, e);
        }
    }

    private void setValue(PreparedStatement ps, int wildcardIndex, T item, ColumnDefinition<T> columnDefinition) throws SQLException {
        Object value = columnDefinition.accessor().getValue(item);
        setValue(ps, wildcardIndex, value);
    }

    /**
     * Sets the value of a provided index query by dynamically evaluating the type of the value provided.
     * <br>
     * This method represents all the supported column types by the {@link SqlReader} class.
     *
     * @param ps A {@link PreparedStatement} that we will update
     * @param wildcardIndex The 1-indexed number indicating a specific wildcard to replace
     * @param value The object representing the value to save in the location
     * @throws SQLException When SQL throws an error.
     */
    public void setValue(@NonNull PreparedStatement ps, int wildcardIndex, @Nullable Object value) throws SQLException {
        // This represents all the supported column types
        switch (value) {
            case null -> ps.setNull(wildcardIndex, NULL);
            case String v -> ps.setString(wildcardIndex, v);
            case Integer v -> ps.setInt(wildcardIndex, v);
            case Float v -> ps.setFloat(wildcardIndex, v);
            case Boolean v -> ps.setBoolean(wildcardIndex, v);
            case Timestamp v -> ps.setTimestamp(wildcardIndex, v);
            case Object v -> ps.setObject(wildcardIndex, v);
        }
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
    public Collection<T> readItems(@NonNull PreparedStatement statement) throws SQLException {
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
            @NonNull PreparedStatement statement,
            @NonNull ItemBuilder<T1> itemBuilder,
            @NonNull Supplier<Collection<T1>> targetCollection
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
     * A specialized overload that doesn't require a StatementPreparer.
     *
     * @see SqlReader#executeQuery(String, StatementPreparer) for more details
     *
     * @param additionalStatementClauses Additional query clauses narrowing the results.
     * @return A collection of matching items.
     */
    public Collection<T> executeQuery(@Nullable String additionalStatementClauses) throws DataAccessException {
        return executeQuery(additionalStatementClauses, x -> {});
    }

    /**
     * Prepares a statement, provides hooks to adjust any parameters, and then reads the results
     * out with the default {@link SqlReader#readItems(PreparedStatement)} method.
     * <br>
     * It appends the provided SQL fragment with {@link SqlReader#selectAllStmt(String)},
     * and then executes the query.
     * <br>
     * This method assists in reading all the columns of the table for a given query.
     * More specialized queries can leverage the more general
     * {@link SqlReader#executeQuery(String, StatementPreparer, ResultSetProcessor)} method.
     *
     * @param additionalStatementClauses Additional query clauses narrowing the results.
     * @param statementPreparer A method that can modify the <code>PreparedStatement</code> before it is executed.
     * @return A collection of objects received as results
     */
    public Collection<T> executeQuery(
        @Nullable String additionalStatementClauses,
        @NonNull StatementPreparer statementPreparer
    ) throws DataAccessException {
        return doExecuteQuery(
                selectAllStmt(additionalStatementClauses),
                statementPreparer,
                this::readItems);
    }


    /**
     * A general helper method for executing a query. It:
     * - Requests a connection
     * - Prepares a statement,
     * - Provides hooks to adjust any parameters, and
     * - Returns the result of the provided query executor
     * <br>
     * This is a very general method responsible for handling errors and organizing resources.
     * The fewest number of assumptions possible are made here.
     *
     * @param statement The full SQL statement to prepare
     * @param statementPreparer A method that can modify the <code>PreparedStatement</code> before it is executed.
     * @param resultSetProcessor A method that receives a {@link ResultSet} and parses out and returns the requested information.
     * @return Any results returned by the result set processor
     */
    public <T1> T1 executeQuery(
            @NonNull String statement,
            @NonNull StatementPreparer statementPreparer,
            @NonNull ResultSetProcessor<T1> resultSetProcessor
    ) throws DataAccessException {
        return doExecuteQuery(
                statement,
                statementPreparer,
                ps -> {
                    try (var resultSet = ps.executeQuery()) {
                        return resultSetProcessor.process(resultSet);
                    }
                }
        );
    }

    private <T1> T1 doExecuteQuery(
            @NonNull String statement,
            @NonNull StatementPreparer statementPreparer,
            @NonNull StatementQueryExecutor<T1> queryExecutor
    ) throws DataAccessException {
        try (
                var connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(statement);
        ) {
            statementPreparer.prepare(ps);
            return queryExecutor.executeQuery(ps);
        } catch (Exception e) {
            LOGGER.error("Error executing query: " + statement, e);
            throw new DataAccessException("Error executing query", e);
        }
    }

    /**
     * Finishes preparing a statement and then executes it as an update.
     * <br>
     * Note that <i>unlike</i> the convenient {@link SqlReader#executeQuery(String)}
     * method which automatically prepends the clause with the table name and SQL query type,
     * this method does not do that. Include the entire SQL statement in this input.
     *
     * @param statement The string statement to prepare
     * @param statementPreparer A method that finishes preparing the statement (usually be filling wildcards)
     */
    public void executeUpdate(
            @NonNull String statement,
            @Nullable StatementPreparer statementPreparer
    ) throws DataAccessException {
        try (
                var connection = getConnection();
                PreparedStatement ps = connection.prepareStatement(statement)
        ) {
            if (statementPreparer != null) statementPreparer.prepare(ps);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error executing update", e);
        }
    }

    /**
     * A helper method returning a connection to the database.
     * This should be closed after use.
     * <br>
     * In the future, this would be the ideal place to change
     * the source of connections, should the need arise.
     *
     * @return A database connection.
     */
    private Connection getConnection() throws DataAccessException {
        return SqlDb.getConnection();
    }

    /**
     * Returns the first item in the collection as type {@link T},
     * or returns `null` if none exist.
     *
     * @param items A collection of items to search.
     * @return An item {@link T} or `null`.
     */
    @Nullable
    public T expectOneItem(@NonNull Collection<T> items) {
        return items.isEmpty() ? null : items.iterator().next();
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
    public String selectAllStmt(@Nullable String additionalClauses) {
        return additionalClauses == null ?
                selectAllColumnsStmt :
                selectAllColumnsStmt + additionalClauses;
    }

    /**
     * Returns the preconfigured table name. Using this pattern minimizes the number of locations
     * the table name needs to be manually defined.
     *
     * @return A string with the table name (configured upon construction).
     */
    public String getTableName() {
        return TABLE_NAME;
    }
}
