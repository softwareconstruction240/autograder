package edu.byu.cs.dataAccess.sql.helpers;

/**
 * A {@link FunctionalInterface} responsible for accessing the value from an object to be
 * stored in a column. Note that only some types are recognized.
 *
 * @see ColumnDefinition
 *
 * @param <T> the type of object the value will be pulled from
 * @param <V> the type of value that will be stored in the column
 */
@FunctionalInterface
public interface ValueAccessor <T, V> {
    V getValue(T item);
}
