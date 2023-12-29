package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;

public class UserSqlDao implements UserDao {
    @Override
    public void insertUser(User user) {

    }

    @Override
    public User getUser(String netId) {
        return null;
    }

    @Override
    public void setRepoUrl(String netId, String repoUrl) {

    }

    @Override
    public void setRole(String netId, User.Role role) {

    }
}
