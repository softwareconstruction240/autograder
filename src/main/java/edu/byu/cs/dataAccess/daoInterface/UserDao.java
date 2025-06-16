package edu.byu.cs.dataAccess.daoInterface;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.User;

import java.util.Collection;

/**
 * A data access object interface for users.
 * This data source holds information about students and admins
 */
public interface UserDao {

    /**
     * Inserts a new user into the database
     *
     * @param user the user to insert
     */
    void insertUser(User user) throws DataAccessException;

    /**
     * Gets the user with the given netId
     *
     * @param netId the netId of the user to get
     * @return the user with the given netId
     */
    User getUser(String netId) throws DataAccessException;

    /**
     * Sets the first name for the given netId
     *
     * @param netId
     * @param firstName
     */
    void setFirstName(String netId, String firstName) throws DataAccessException;

    /**
     * Sets the last name for the given netId
     *
     * @param netId
     * @param lastName
     */
    void setLastName(String netId, String lastName) throws DataAccessException;

    /**
     * Sets the repoUrl for the given netId
     *
     * @param netId   the netId to set the repoUrl for
     * @param repoUrl the repoUrl to set for the given netId
     */
    void setRepoUrl(String netId, String repoUrl) throws DataAccessException;

    /**
     * Sets the role for the given netId
     *
     * @param netId the netId to set the role for
     * @param role  the role to set for the given netId
     */
    void setRole(String netId, User.Role role) throws DataAccessException;

    /**
     * Sets the canvas user id for the given netId.
     * <br/><strong>Note: this will likely only be used for the test student</strong>
     *
     * @param netId        the netId to set the canvas user id for
     * @param canvasUserId the canvas user id to set for the given netId
     */
    void setCanvasUserId(String netId, int canvasUserId) throws DataAccessException;

    /**
     * Gets all users
     *
     * @return all users
     */
    Collection<User> getUsers() throws DataAccessException;

    /**
     * Checks if the given repoUrl is claimed by a user
     *
     * @param repoUrl the repoUrl to check
     * @return true if the repoUrl is claimed by a user, false otherwise
     */
    boolean repoUrlClaimed(String repoUrl) throws DataAccessException;
}
