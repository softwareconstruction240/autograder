package edu.byu.cs.dataAccess.sql.helpers;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A {@link FunctionalInterface} that receives a SQL query {@link PreparedStatement}
 * and returns the results
 *
 * @param <T> the returned results
 */
@FunctionalInterface
public interface StatementQueryExecutor <T> {
    T executeQuery(PreparedStatement preparedStatement) throws SQLException;
}
