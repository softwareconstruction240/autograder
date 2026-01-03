package edu.byu.cs.dataAccess.base;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.RepoUpdateDao;
import edu.byu.cs.model.RepoUpdate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.stream.IntStream;

public abstract class RepoUpdateDaoTest {
    protected RepoUpdateDao dao;
    protected abstract RepoUpdateDao getRepoUpdateDao();
    protected abstract void clearRepoUpdateItems() throws DataAccessException;
    private static Random random = new Random();

    static IntStream getNoiseStream() {
        return IntStream.of(1,2,3,300);
    }

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

    @ParameterizedTest
    @MethodSource("getNoiseStream")
    public void getRepoUpdatesForValidUser(int noise) throws DataAccessException{
        Collection<RepoUpdate> updates = generateManyRepoUpdates(noise);
        insertAllRepoUpdates(updates);

        Collection<RepoUpdate> validUserUpdates = generateManyRepoUpdates(noise, netId, null);
        insertAllRepoUpdates(validUserUpdates);

        Collection<RepoUpdate> obtainedUpdates = dao.getUpdatesForUser(netId);

        Assertions.assertEquals(validUserUpdates.size(), obtainedUpdates.size());
        Assertions.assertTrue(obtainedUpdates.containsAll(validUserUpdates));

        for (RepoUpdate update : updates) {
            Assertions.assertFalse(obtainedUpdates.contains(update));
        }
    }

    @ParameterizedTest
    @MethodSource("getNoiseStream")
    public void getNoRepoUpdatesForInvalidUser(int noise) throws DataAccessException{
        insertAllRepoUpdates(generateManyRepoUpdates(noise));

        Collection<RepoUpdate> obtainedUpdates = dao.getUpdatesForUser(null);
        Assertions.assertTrue(obtainedUpdates.isEmpty());

        obtainedUpdates = dao.getUpdatesForUser(netId);
        Assertions.assertTrue(obtainedUpdates.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("getNoiseStream")
    public void getRepoUpdatesForValidRepoUrl(int noise) throws DataAccessException{
        Collection<RepoUpdate> updates = generateManyRepoUpdates(noise);
        insertAllRepoUpdates(updates);

        Collection<RepoUpdate> updatesWithSameRepo = generateManyRepoUpdates(noise, null, repoLink);
        insertAllRepoUpdates(updatesWithSameRepo);

        Collection<RepoUpdate> obtainedUpdates = dao.getUpdatesForRepo(repoLink);
        Assertions.assertEquals(updatesWithSameRepo.size(), obtainedUpdates.size());
        Assertions.assertTrue(obtainedUpdates.containsAll(updatesWithSameRepo));

        for (RepoUpdate update : updates){
            Assertions.assertFalse(obtainedUpdates.contains(update));
        }
    }

    @ParameterizedTest
    @MethodSource("getNoiseStream")
    public void getNoRepoUpdatesForInvalidRepoUrl(int noise) throws DataAccessException{
        insertAllRepoUpdates(generateManyRepoUpdates(noise));

        Collection<RepoUpdate> obtainedUpdates = dao.getUpdatesForRepo(null);
        Assertions.assertTrue(obtainedUpdates.isEmpty());

        obtainedUpdates = dao.getUpdatesForRepo(repoLink);
        Assertions.assertTrue(obtainedUpdates.isEmpty());
    }

    private void insertAllRepoUpdates(Collection<RepoUpdate> updates) throws DataAccessException{
        for (RepoUpdate update : updates){
            dao.insertUpdate(update);
        }
    }

    private RepoUpdate generateRepoUpdate(String netId, String repoLink){
        boolean admin = random.nextBoolean();
        return new RepoUpdate(
                Instant.now().truncatedTo(ChronoUnit.SECONDS)
                        .minus(random.nextInt(0,365), ChronoUnit.DAYS)
                        .minusSeconds(random.nextLong(1, 86399)),
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
        String user = netId == null ? DaoTestUtils.generateNetID(random.nextInt()) : netId;
        String url = repoLink == null ? "https://github.com/" + user + "/chess" : repoLink;
        var updates = new HashSet<RepoUpdate>();
        for (int i = 0; i < size; i++){
            updates.add(generateRepoUpdate(user, url));
        }
        return updates;
    }
}
