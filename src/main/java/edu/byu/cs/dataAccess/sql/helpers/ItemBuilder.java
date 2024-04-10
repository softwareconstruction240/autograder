package edu.byu.cs.dataAccess.sql.helpers;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ItemBuilder <T> {
    /**
     * This method is responsible for reading one row of the result
     * set and returning a constructed item.
     *
     * @param resultSet A ResultSet with the iterator pointing at a valid row
     * @return A fully constructed item
     * @throws SQLException When SQL has an issu
     */
    T readItem(ResultSet resultSet) throws SQLException;
}
