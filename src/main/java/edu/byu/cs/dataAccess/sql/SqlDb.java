package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.properties.DbProperties;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SqlDb {

    private static final String DB_URL = DbProperties.dbUrl();
    private static final String DB_USER = DbProperties.dbUser();
    private static final String DB_PASSWORD = DbProperties.dbPassword();

    private static final String CONNECTION_STRING = "jdbc:mysql://" + DB_URL ;
    static Connection getConnection() {
        try {
            return DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.err.println("Error connecting to database");
            e.printStackTrace();

            throw new DataAccessException("Error connecting to database", e);
        }
    }
}
