package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.dataAccess.sql.helpers.ColumnDefinition;
import edu.byu.cs.dataAccess.sql.helpers.SqlReader;
import edu.byu.cs.model.User;
import org.eclipse.jgit.annotations.NonNull;
import org.intellij.lang.annotations.Language;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

public class UserSqlDao implements UserDao {
    private static final ColumnDefinition[] COLUMN_DEFINITIONS = {
            new ColumnDefinition<User>("net_id", User::netId),
            new ColumnDefinition<User>("canvas_user_id", User::canvasUserId),
            new ColumnDefinition<User>("first_name", User::firstName),
            new ColumnDefinition<User>("last_name", User::lastName),
            new ColumnDefinition<User>("repo_url", User::repoUrl),
            new ColumnDefinition<User>("role", user -> user.role().toString()),
    };
    private static User readUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("net_id"),
                rs.getInt("canvas_user_id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("repo_url"),
                User.Role.valueOf(rs.getString("role"))
        );
    }

    private final SqlReader<User> sqlReader = new SqlReader<User>(
            "user", COLUMN_DEFINITIONS, UserSqlDao::readUser);


    @Override
    public void insertUser(User user) throws DataAccessException {
        sqlReader.insertItem(user);
    }

    @Override
    public User getUser(String netId) throws DataAccessException {
        var results = sqlReader.executeQuery(
                "WHERE net_id = ?",
                ps -> ps.setString(1, netId));
        return sqlReader.expectOneItem(results);
    }

    @Override
    public void setFirstName(String netId, String firstName) throws DataAccessException {
        setFieldValue(netId, "first_name", firstName);
    }

    @Override
    public void setLastName(String netId, String lastName) throws DataAccessException {
        setFieldValue(netId, "last_name", lastName);
    }

    @Override
    public void setRepoUrl(String netId, String repoUrl) throws DataAccessException {
        setFieldValue(netId, "repo_url", repoUrl);
    }

    @Override
    public void setRole(String netId, User.Role role) throws DataAccessException {
        setFieldValue(netId, "role", role.toString());
    }

    @Override
    public void setCanvasUserId(String netId, int canvasUserId) throws DataAccessException {
        setFieldValue(netId, "canvas_user_id", canvasUserId);
    }

    private void setFieldValue(@NonNull String netId, @NonNull @Language("SQL") String columnName, @NonNull Object columnValue) throws DataAccessException {
        sqlReader.executeUpdate(
                """
                    UPDATE user
                    SET %s = ?
                    WHERE net_id = ?
                    """.formatted(columnName),
                ps -> {
                    sqlReader.setValue(ps, 1, columnValue);
                    ps.setString(2, netId);
                }
        );
    }

    @Override
    public Collection<User> getUsers() throws DataAccessException {
        return sqlReader.executeQuery("");
    }

    @Override
    public boolean repoUrlClaimed(String repoUrl) throws DataAccessException {
        var results = sqlReader.executeQuery(
                "WHERE repo_url = ?",
                ps -> ps.setString(1, repoUrl));
        return !results.isEmpty();
    }

}
