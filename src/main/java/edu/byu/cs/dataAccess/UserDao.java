package edu.byu.cs.dataAccess;

import edu.byu.cs.model.User;

/**
 * A data access object for users.
 * This data source holds information about students and admins
 */
public interface UserDao {

    /**
     * Inserts a new user into the database
     * @param user the user to insert
     * @return the repoUrl of the user inserted
     */
    String insertUser(User user);

    /**
     * Gets the user with the given netId
     * @param netId the netId of the user to get
     * @return the user with the given netId
     */
    User getUser(String netId);

    /**
     * Sets the repoUrl for the given netId
     * @param netId the netId to set the repoUrl for
     * @param repoUrl the repoUrl to set for the given netId
     */
    void setRepoUrl(String netId, String repoUrl);

    /**
     * Sets the role for the given netId
     * @param netId the netId to set the role for
     * @param role the role to set for the given netId
     */
    void setRole(String netId, User.Role role);
}
