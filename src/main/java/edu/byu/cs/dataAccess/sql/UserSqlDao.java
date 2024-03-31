package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.dataAccess.sql.helpers.ColumnDefinition;
import edu.byu.cs.dataAccess.sql.helpers.SqlReader;
import edu.byu.cs.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    public void insertUser(User user) {
        sqlReader.insertItem(user);
    }

    @Override
    public User getUser(String netId) {
        var results = sqlReader.executeQuery(
                "WHERE net_id = ?",
                ps -> ps.setString(1, netId));
        return results.isEmpty() ? null : results.iterator().next();
    }

    @Override
    public void setFirstName(String netId, String firstName) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            UPDATE user
                            SET first_name = ?
                            WHERE net_id = ?
                            """)) {
            statement.setString(1, firstName);
            statement.setString(2, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error setting first name", e);
        }
    }

    @Override
    public void setLastName(String netId, String lastName) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            UPDATE user
                            SET last_name = ?
                            WHERE net_id = ?
                            """)) {
            statement.setString(1, lastName);
            statement.setString(2, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error setting last name", e);
        }
    }

    @Override
    public void setRepoUrl(String netId, String repoUrl) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            UPDATE user
                            SET repo_url = ?
                            WHERE net_id = ?
                            """)) {
            statement.setString(1, repoUrl);
            statement.setString(2, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error setting repo url", e);
        }
    }

    @Override
    public void setRole(String netId, User.Role role) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            UPDATE user
                            SET role = ?
                            WHERE net_id = ?
                            """)) {
            statement.setString(1, role.toString());
            statement.setString(2, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error setting role", e);
        }
    }

    @Override
    public void setCanvasUserId(String netId, int canvasUserId) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            UPDATE user
                            SET canvas_user_id = ?
                            WHERE net_id = ?
                            """)) {
            statement.setInt(1, canvasUserId);
            statement.setString(2, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error setting canvas user id", e);
        }
    }

    @Override
    public Collection<User> getUsers() {
        return sqlReader.executeQuery("");
    }

    @Override
    public boolean repoUrlClaimed(String repoUrl) {
        var results = sqlReader.executeQuery(
                "WHERE repo_url = ?",
                ps -> ps.setString(1, repoUrl));
        return !results.isEmpty();
    }

}
