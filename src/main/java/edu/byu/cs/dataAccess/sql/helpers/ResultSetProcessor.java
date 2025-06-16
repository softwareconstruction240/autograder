package edu.byu.cs.dataAccess.sql.helpers;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A {@link FunctionalInterface} that receives a {@link ResultSet}
 * and parses out and returns the requested information
 *
 * @param <T> the requested information
 */
@FunctionalInterface
public interface ResultSetProcessor <T> {
    T process(ResultSet resultSet) throws SQLException;
}
