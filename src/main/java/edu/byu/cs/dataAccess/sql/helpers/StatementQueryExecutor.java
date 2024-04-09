package edu.byu.cs.dataAccess.sql.helpers;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface StatementQueryExecutor <T> {
    T executeQuery(PreparedStatement preparedStatement) throws SQLException;
}
