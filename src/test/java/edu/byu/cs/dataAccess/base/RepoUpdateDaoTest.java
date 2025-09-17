package edu.byu.cs.dataAccess.base;

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

    @BeforeEach
    public void setup() throws DataAccessException{
        dao = getRepoUpdateDao();
        clearRepoUpdateItems();
    }

    @Test
    public void insertAndGetOneValidRepoUpdate () throws DataAccessException{
        String netId = "cosmo_cougar";
        String repoUrl = "https://github.com/comso_cougar/chess";
        RepoUpdate update = generateRepoUpdate(netId, repoUrl);
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
    public void getRepoUpdatesForValidUser(){
        Collection<RepoUpdate> updates = generateManyRepoUpdates(3);
        for (RepoUpdate update : updates){
            dao.
        }
    }

    @Test
    public void getNoRepoUpdatesForInvalidUser(){

    }

    @Test
    public void getRepoUpdatesForValidRepoUrl(){

    }

    @Test
    public void getNoRepoUpdatesForInvalidRepoUrl(){

    }

    public RepoUpdate generateRepoUpdate(String netId, String repoUrl){
        boolean admin = random.nextBoolean();
        return new RepoUpdate(
                Instant.now().truncatedTo(ChronoUnit.SECONDS),
                netId,
                repoUrl,
                admin,
                admin ? "Cosmo_Boss" : null
        );
    }

    public HashSet<RepoUpdate> generateManyRepoUpdates(int size){
        var updates = new HashSet<RepoUpdate>();
        for (int i = 0; i < size; i++){
            int id = random.nextInt();
            updates.add(generateRepoUpdate(
                    "cosmo_" + id,
                    String.format("https://github.com/comso_%d/chess", id)));
        }
        return updates;
    }
}
