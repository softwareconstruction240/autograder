package edu.byu.cs.dataAccess.sql.helpers;

import java.sql.ResultSet;

@FunctionalInterface
public interface ResultReader <T> {
    T read(ResultSet resultSet);
}
