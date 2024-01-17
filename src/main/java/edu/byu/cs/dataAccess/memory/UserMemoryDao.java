package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;

import java.util.Collection;
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
    public void setFirstName(String netId, String firstName) {
        if (!users.containsKey(netId))
            throw new IllegalArgumentException("User does not exist");

        User oldUser = users.get(netId);
        User newUser = new User(oldUser.netId(), oldUser.canvasUserId(), firstName, oldUser.lastName(), oldUser.repoUrl(), oldUser.role());

        users.put(netId, newUser);
    }

    @Override
    public void setLastName(String netId, String lastName) {
        if (!users.containsKey(netId))
            throw new IllegalArgumentException("User does not exist");

        User oldUser = users.get(netId);
        User newUser = new User(oldUser.netId(), oldUser.canvasUserId(), oldUser.firstName(), lastName, oldUser.repoUrl(), oldUser.role());

        users.put(netId, newUser);
    }

    @Override
    public void setRepoUrl(String netId, String repoUrl) {
        if (!users.containsKey(netId))
            throw new IllegalArgumentException("User does not exist");

        User oldUser = users.get(netId);
        User newUser = new User(oldUser.netId(), oldUser.canvasUserId(), oldUser.firstName(), oldUser.lastName(), repoUrl, oldUser.role());

        users.put(netId, newUser);
    }

    @Override
    public void setRole(String netId, User.Role role) {
        if (!users.containsKey(netId))
            throw new IllegalArgumentException("User does not exist");

        User oldUser = users.get(netId);
        User newUser = new User(oldUser.netId(), 0, oldUser.firstName(), oldUser.lastName(), oldUser.repoUrl(), role);

        users.put(netId, newUser);
    }

    @Override
    public void setCanvasUserId(String netId, int canvasUserId) {
        if (!users.containsKey(netId))
            throw new IllegalArgumentException("User does not exist");

        User oldUser = users.get(netId);
        User newUser = new User(oldUser.netId(), canvasUserId, oldUser.firstName(), oldUser.lastName(), oldUser.repoUrl(), oldUser.role());

        users.put(netId, newUser);
    }

    @Override
    public Collection<User> getUsers() {
        return users.values();
    }

    @Override
    public boolean repoUrlClaimed(String repoUrl) {
        for (User user : users.values()) {
            if (user.repoUrl() != null && user.repoUrl().equalsIgnoreCase(repoUrl))
                return true;
        }

        return false;
    }
}
