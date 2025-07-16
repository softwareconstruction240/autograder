package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.UserDaoTest;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import org.junit.jupiter.api.BeforeAll;

import java.sql.Connection;
import java.sql.SQLException;

public class UserSqlDaoTest extends UserDaoTest {
    @BeforeAll
    protected static void prepareDatabase() throws DataAccessException {
        SqlDaoTestUtils.prepareSQLDatabase();
    }

    @Override
    protected UserDao newUserDao() {
        return new UserSqlDao();
    }

    @Override
    protected void clearUsers() throws DataAccessException{
        try(var connection = SqlDb.getConnection()){
            var statement = connection.prepareStatement("DELETE FROM user");
            statement.executeUpdate();
        }catch (SQLException e){
            throw new DataAccessException("Could not clear users", e);
        }
    }
}
