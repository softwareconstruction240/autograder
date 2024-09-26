package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.sql.RepoUpdateDao;
import edu.byu.cs.model.RepoUpdate;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

public class RepoUpdateMemoryDao implements RepoUpdateDao {
    private final Deque<RepoUpdate> repoUpdates = new LinkedList<>();

    @Override
    public void insertUpdate(RepoUpdate update) throws DataAccessException {
        repoUpdates.add(update);
    }

    @Override
    public Collection<RepoUpdate> getUpdatesForUser(String netId) throws DataAccessException {
        return repoUpdates
                .stream()
                .filter(update ->
                        update.netId().equals(netId))
                .toList();
    }

    @Override
    public Collection<RepoUpdate> getUpdatesForRepo(String repoUrl) throws DataAccessException {
        return repoUpdates
                .stream()
                .filter(update ->
                        update.repoUrl().equals(repoUrl))
                .toList();
    }
}
