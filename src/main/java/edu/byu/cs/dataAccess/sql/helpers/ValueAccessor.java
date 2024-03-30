package edu.byu.cs.dataAccess.sql.helpers;

@FunctionalInterface
public interface ValueAccessor <T, V> {
    V getValue(T item);
}
