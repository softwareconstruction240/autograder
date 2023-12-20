package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;

import java.util.concurrent.ConcurrentHashMap;

public class UserMemoryDao implements UserDao {

    private static final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    @Override
    public void insertUser(User user) {
        if (users.containsKey(user.netId()))
            throw new IllegalArgumentException("User already exists");

        users.put(user.netId(), user);
    }

    @Override
    public User getUser(String netId) {
        return users.get(netId);
    }

    @Override
    public void setRepoUrl(String netId, String repoUrl) {
        if (!users.containsKey(netId))
            throw new IllegalArgumentException("User does not exist");

        User oldUser = users.get(netId);
        User newUser = new User(oldUser.netId(), oldUser.firstName(), oldUser.lastName(), repoUrl, oldUser.role());

        users.put(netId, newUser);
    }

    @Override
    public void setRole(String netId, User.Role role) {
        if (!users.containsKey(netId))
            throw new IllegalArgumentException("User does not exist");

        User oldUser = users.get(netId);
        User newUser = new User(oldUser.netId(), oldUser.firstName(), oldUser.lastName(), oldUser.repoUrl(), role);

        users.put(netId, newUser);
    }
}
