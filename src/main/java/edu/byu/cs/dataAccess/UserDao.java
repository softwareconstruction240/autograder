package edu.byu.cs.dataAccess;

import edu.byu.cs.model.User;

import java.util.Collection;

/**
 * A data access object for users.
 * This data source holds information about students and admins
 */
public interface UserDao {

    /**
     * Inserts a new user into the database
     *
     * @param user the user to insert
     */
    void insertUser(User user);

    /**
     * Gets the user with the given netId
     *
     * @param netId the netId of the user to get
     * @return the user with the given netId
     */
    User getUser(String netId);

    /**
     * Sets the first name for the given netId
     *
     * @param netId
     * @param firstName
     */
    void setFirstName(String netId, String firstName);

    /**
     * Sets the last name for the given netId
     *
     * @param netId
     * @param lastName
     */
    void setLastName(String netId, String lastName);

    /**
     * Sets the repoUrl for the given netId
     *
     * @param netId   the netId to set the repoUrl for
     * @param repoUrl the repoUrl to set for the given netId
     */
    void setRepoUrl(String netId, String repoUrl);

    /**
     * Sets the role for the given netId
     *
     * @param netId the netId to set the role for
     * @param role  the role to set for the given netId
     */
    void setRole(String netId, User.Role role);

    /**
     * Gets all users
     *
     * @return all users
     */
    Collection<User> getUsers();

    /**
     * Checks if the given repoUrl is claimed by a user
     *
     * @param repoUrl the repoUrl to check
     * @return true if the repoUrl is claimed by a user, false otherwise
     */
    boolean repoUrlClaimed(String repoUrl);
}
