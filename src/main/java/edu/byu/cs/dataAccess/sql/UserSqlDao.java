package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

public class UserSqlDao implements UserDao {
    @Override
    public void insertUser(User user) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO user (net_id, canvas_user_id, first_name, last_name, repo_url, role)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """)) {
            statement.setString(1, user.netId());
            statement.setInt(2, user.canvasUserId());
            statement.setString(3, user.firstName());
            statement.setString(4, user.lastName());
            statement.setString(5, user.repoUrl());
            statement.setString(6, user.role().toString());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new DataAccessException("Error inserting user", e);
        }
    }

    @Override
    public User getUser(String netId) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT net_id, canvas_user_id, first_name, last_name, repo_url, role
                            FROM user
                            WHERE net_id = ?
                            """)) {
            statement.setString(1, netId);
            try(ResultSet results = statement.executeQuery()) {
                if (results.next()) {
                    return new User(results.getString("net_id"), results.getInt("canvas_user_id"),
                            results.getString("first_name"), results.getString("last_name"),
                            results.getString("repo_url"), User.Role.valueOf(results.getString("role")));
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("Error getting user", e);
        }
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
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT net_id, canvas_user_id, first_name, last_name, repo_url, role
                            FROM user
                            """)) {
            try (ResultSet results = statement.executeQuery()) {

                ArrayList<User> users = new ArrayList<>();
                while (results.next()) {
                    users.add(new User(results.getString("net_id"), results.getInt("canvas_user_id"),
                            results.getString("first_name"), results.getString("last_name"),
                            results.getString("repo_url"), User.Role.valueOf(results.getString("role"))));
                }

                return users;
            }
        } catch (Exception e) {
            throw new DataAccessException("Error getting users", e);
        }
    }

    @Override
    public boolean repoUrlClaimed(String repoUrl) {
        try (var connection = SqlDb.getConnection();
            PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT net_id
                            FROM user
                            WHERE repo_url = ?
                            """)) {
            statement.setString(1, repoUrl);
            try(ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        } catch (Exception e) {
            throw new DataAccessException("Error checking if repo url is claimed", e);
        }
    }


}
