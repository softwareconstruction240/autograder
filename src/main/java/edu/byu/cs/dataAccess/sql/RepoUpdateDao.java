package edu.byu.cs.dataAccess.sql;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.RepoUpdate;

import java.util.Collection;

public interface RepoUpdateDao {
    /**
     * Inserts an update into the database
     * @param update
     */
    void insertUpdate(RepoUpdate update) throws DataAccessException;

    /**
     * Gets all updates to a user's repo
     * @param netId user to get update history for
     * @return all updates to their repo
     */
    Collection<RepoUpdate> getUpdatesForUser(String netId) throws DataAccessException;

    /**
     * Gets all updates with a specific repo
     * @param repoUrl the repo to get all updates for
     * @return
     */
    Collection<RepoUpdate> getUpdatesForRepo(String repoUrl) throws DataAccessException;
}
