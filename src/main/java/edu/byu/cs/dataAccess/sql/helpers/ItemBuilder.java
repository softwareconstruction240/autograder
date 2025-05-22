package edu.byu.cs.dataAccess.sql.helpers;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A {@link FunctionalInterface} responsible for reading a row from a {@link ResultSet} and
 * building an item using that row
 *
 * @param <T> the item to be built
 */
@FunctionalInterface
public interface ItemBuilder <T> {
    /**
     * This method is responsible for reading one row of the result
     * set and returning a constructed item.
     *
     * @param resultSet A ResultSet with the iterator pointing at a valid row
     * @return A fully constructed item
     * @throws SQLException When SQL has an issue
     */
    T readItem(ResultSet resultSet) throws SQLException;
}
