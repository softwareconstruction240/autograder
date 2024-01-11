package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;

public class UserSqlDao implements UserDao {
    @Override
    public void insertUser(User user) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO user (net_id, first_name, last_name, repo_url, role)
                            VALUES (?, ?, ?, ?, ?)
                            """);
            statement.setString(1, user.netId());
            statement.setString(2, user.firstName());
            statement.setString(3, user.lastName());
            statement.setString(4, user.repoUrl());
            statement.setString(5, user.role().toString());
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error inserting user", e);
        }
    }

    @Override
    public User getUser(String netId) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT net_id, first_name, last_name, repo_url, role
                            FROM user
                            WHERE net_id = ?
                            """);
            statement.setString(1, netId);
            ResultSet results = statement.executeQuery();
            if (results.next()) {
                return new User(
                        results.getString("net_id"),
                        results.getString("first_name"),
                        results.getString("last_name"),
                        results.getString("repo_url"),
                        User.Role.valueOf(results.getString("role"))
                );
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error getting user", e);
        }
    }

    @Override
    public void setRepoUrl(String netId, String repoUrl) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            UPDATE user
                            SET repo_url = ?
                            WHERE net_id = ?
                            """);
            statement.setString(1, repoUrl);
            statement.setString(2, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error setting repo url", e);
        }
    }

    @Override
    public void setRole(String netId, User.Role role) {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            UPDATE user
                            SET role = ?
                            WHERE net_id = ?
                            """);
            statement.setString(1, role.toString());
            statement.setString(2, netId);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error setting role", e);
        }
    }

    @Override
    public Collection<User> getUsers() {
        try (var connection = SqlDb.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            SELECT net_id, first_name, last_name, repo_url, role
                            FROM user
                            """);
            ResultSet results = statement.executeQuery();

            ArrayList<User> users = new ArrayList<>();
            while (results.next()) {
                users.add(new User(
                        results.getString("net_id"),
                        results.getString("first_name"),
                        results.getString("last_name"),
                        results.getString("repo_url"),
                        User.Role.valueOf(results.getString("role"))
                ));
            }

            return users;
        } catch (Exception e) {
            throw new RuntimeException("Error getting users", e);
        }
    }
}
