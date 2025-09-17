package edu.byu.cs.dataAccess.base;

import edu.byu.cs.autograder.test.PreviousPhasePassoffTestGrader;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.RepoUpdateDao;
import edu.byu.cs.model.RepoUpdate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;

public abstract class RepoUpdateDaoTest {
    protected RepoUpdateDao dao;
    protected abstract RepoUpdateDao getRepoUpdateDao();
    protected abstract void clearRepoUpdateItems() throws DataAccessException;
    private static Random random = new Random();

    String netId = "cosmo_cougar";
    String repoLink = "https://github.com/" + netId + "/chess";

    @BeforeEach
    public void setup() throws DataAccessException{
        dao = getRepoUpdateDao();
        clearRepoUpdateItems();
    }

    @Test
    public void insertAndGetOneValidRepoUpdate () throws DataAccessException{
        RepoUpdate update = generateRepoUpdate(netId, repoLink);
        dao.insertUpdate(update);
        Collection<RepoUpdate> updates = dao.getUpdatesForUser(netId);
        Assertions.assertEquals(1, updates.size());
        Assertions.assertTrue(updates.contains(update));
    }

    @Test
    public void insertNullRepoUpdate() {
        Assertions.assertThrows(DataAccessException.class, ()-> dao.insertUpdate(null));
    }

    @Test
    public void getRepoUpdatesForValidUser() throws DataAccessException{
        Collection<RepoUpdate> updates = generateManyRepoUpdates(3);
        insertAllRepoUpdates(updates);

        Collection<RepoUpdate> validUserUpdates = generateManyRepoUpdates(3, netId, null);
        insertAllRepoUpdates(validUserUpdates);

        Collection<RepoUpdate> obtainedUpdates = dao.getUpdatesForUser(netId);

        Assertions.assertEquals(validUserUpdates.size(), obtainedUpdates.size());
        Assertions.assertTrue(obtainedUpdates.containsAll(validUserUpdates));

        for (RepoUpdate update : updates) {
            Assertions.assertFalse(obtainedUpdates.contains(update));
        }
    }

    @Test
    public void getNoRepoUpdatesForInvalidUser() throws DataAccessException{
        Collection<RepoUpdate> updates = generateManyRepoUpdates(3);
        insertAllRepoUpdates(updates);

        Collection<RepoUpdate> obtainedUpdates = dao.getUpdatesForUser(null);
        Assertions.assertTrue(obtainedUpdates.isEmpty());

        obtainedUpdates = dao.getUpdatesForUser(netId);
        Assertions.assertTrue(obtainedUpdates.isEmpty());
    }

    @Test
    public void getRepoUpdatesForValidRepoUrl() throws DataAccessException{
        Collection<RepoUpdate> updates = generateManyRepoUpdates(3);
        insertAllRepoUpdates(updates);

        Collection<RepoUpdate> updatesWithSameRepo = generateManyRepoUpdates(3, null, repoLink);
        insertAllRepoUpdates(updatesWithSameRepo);

        Collection<RepoUpdate> obtainedUpdates = dao.getUpdatesForRepo(repoLink);
        Assertions.assertEquals(updatesWithSameRepo.size(), obtainedUpdates.size());
        Assertions.assertTrue(obtainedUpdates.containsAll(updatesWithSameRepo));

        for (RepoUpdate update : updates){
            Assertions.assertFalse(obtainedUpdates.contains(update));
        }
    }

    @Test
    public void getNoRepoUpdatesForInvalidRepoUrl(){

    }

    private void insertAllRepoUpdates(Collection<RepoUpdate> updates) throws DataAccessException{
        for (RepoUpdate update : updates){
            dao.insertUpdate(update);
        }
    }

    private RepoUpdate generateRepoUpdate(String netId, String repoLink){
        boolean admin = random.nextBoolean();
        return new RepoUpdate(
                //FIXME primary key for this table is time, and with this code there's roughly a 1/365 chance for a collision
                Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(random.nextInt(0,365), ChronoUnit.DAYS),
                netId,
                repoLink,
                admin,
                admin ? "Cosmo_Boss" : null
        );
    }

    private HashSet<RepoUpdate> generateManyRepoUpdates(int size){
        return generateManyRepoUpdates(size, null, null);
    }

    /**
     *
     * @param size
     * @param netId null if random is desired
     * @param repoLink null if it should be linked to netId
     * @return
     */
    private HashSet<RepoUpdate> generateManyRepoUpdates(int size, String netId, String repoLink){
        String user = netId == null ? "cosmo_" + random.nextInt() : netId;
        String url = repoLink == null ? "https://github.com/" + user + "/chess" : repoLink;
        var updates = new HashSet<RepoUpdate>();
        for (int i = 0; i < size; i++){
            updates.add(generateRepoUpdate(user, url));
        }
        return updates;
    }
}
