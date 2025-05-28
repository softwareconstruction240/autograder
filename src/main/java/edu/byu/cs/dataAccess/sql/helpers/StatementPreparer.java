package edu.byu.cs.dataAccess.sql.helpers;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A {@link FunctionalInterface} that can modify the {@link PreparedStatement}
 * before it is executed
 */
@FunctionalInterface
public interface StatementPreparer {
    void prepare(PreparedStatement ps) throws SQLException;
}
