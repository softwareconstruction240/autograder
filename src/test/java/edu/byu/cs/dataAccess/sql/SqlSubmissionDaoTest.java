package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.base.SubmissionDaoTest;
import edu.byu.cs.dataAccess.daoInterface.SubmissionDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import org.junit.jupiter.api.BeforeAll;

import java.sql.SQLException;

class SqlSubmissionDaoTest extends SubmissionDaoTest {

    @BeforeAll
    protected static void prepareDatabase() throws DataAccessException {
        SqlDaoTestUtils.prepareSQLDatabase();
    }
    @Override
    protected SubmissionDao newSubmissionDao() {
        return new SubmissionSqlDao();
    }

    @Override
    protected UserDao newUserDao() {
        return new UserSqlDao();
    }

    @Override
    protected void clearSubmissions() throws DataAccessException {
        try (var connection = SqlDb.getConnection();
             var statement = connection.prepareStatement("TRUNCATE submission")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not clear database", e);
        }
    }
}
