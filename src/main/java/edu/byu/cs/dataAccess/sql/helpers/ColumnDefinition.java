package edu.byu.cs.dataAccess.sql.helpers;

public record ColumnDefinition <T> (
    String columnName,
    ValueAccessor<T, Object> accessor
) { }
