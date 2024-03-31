package edu.byu.cs.dataAccess.sql.helpers;

/**
 * Represents a single column of this table.
 * This information will be used to automatically serialize and store
 * the data when inserting a new item.
 *
 * @param columnName The case-sensitive column name in the table
 * @param accessor A method that returns the value belonging in this column.
 *                 Note that only some types are recognized.
 * @param <T> The type of object that will be stored in the column.
 */
public record ColumnDefinition <T> (
    String columnName,
    ValueAccessor<T, Object> accessor
) { }
