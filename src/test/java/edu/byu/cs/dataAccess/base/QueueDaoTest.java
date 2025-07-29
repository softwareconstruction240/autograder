package edu.byu.cs.dataAccess.base;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.model.QueueItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

public abstract class QueueDaoTest {
    protected QueueDao dao;
    protected abstract QueueDao getQueueDao();
    protected abstract void clearQueueItems() throws DataAccessException;
    static Random random = new Random();

    @BeforeEach
    void setup() throws DataAccessException{
        dao = getQueueDao();
        clearQueueItems();
    }

    @Test
    void add() throws DataAccessException {
        QueueItem expected = generateQueueItem();
        Assertions.assertDoesNotThrow(() -> dao.add(expected));
        QueueItem obtained = dao.get(expected.netId());
        Assertions.assertEquals(expected, obtained);
    }

    @Test
    void remove() throws DataAccessException {
        Collection<QueueItem> queue = generateQueue(3);
        QueueItem removeMe = generateQueueItem();
        dao.add(removeMe);
        for (QueueItem item : queue){
            dao.add(item);
        }
        Collection<QueueItem> obtainedQueue = dao.getAll();
        Assertions.assertEquals(queue.size()+1, obtainedQueue.size());
        Assertions.assertTrue(obtainedQueue.containsAll(queue));
        Assertions.assertTrue(obtainedQueue.contains(removeMe));
        dao.remove(removeMe.netId());
        obtainedQueue = dao.getAll();
        Assertions.assertEquals(queue.size(), obtainedQueue.size());
        Assertions.assertTrue(obtainedQueue.containsAll(queue));
        Assertions.assertFalse(obtainedQueue.contains(removeMe));
    }

    @Test
    void getAll() throws DataAccessException {
        Collection<QueueItem> queue = generateQueue(3);
        for (QueueItem item : queue){
            dao.add(item);
        }
        Collection<QueueItem> obtainedQueue = dao.getAll();
        Assertions.assertEquals(queue.size(), obtainedQueue.size());
        Assertions.assertTrue(obtainedQueue.containsAll(queue));
    }

    @Test
    void isAlreadyInQueue() throws DataAccessException {
        QueueItem item = generateQueueItem();
        Assertions.assertFalse(dao.isAlreadyInQueue(item.netId()));
        dao.add(item);
        Assertions.assertTrue(dao.isAlreadyInQueue(item.netId()));
    }

    @Test
    void markStarted() throws DataAccessException {
        QueueItem item = generateQueueItem();
        dao.add(item);
        QueueItem expected = new QueueItem(
                item.netId(),
                item.phase(),
                item.timeAdded(),
                true
        );
        dao.markStarted(item.netId());
        QueueItem obtained = dao.get(item.netId());
        Assertions.assertEquals(expected, obtained);
    }

    @Test
    void markNotStarted() throws DataAccessException {
        QueueItem item = generateQueueItem();
        dao.add(item);
        QueueItem expected = new QueueItem(
                item.netId(),
                item.phase(),
                item.timeAdded(),
                true
        );
        dao.markStarted(item.netId());
        QueueItem obtained = dao.get(item.netId());
        Assertions.assertEquals(expected, obtained);
        dao.markNotStarted(item.netId());
        obtained = dao.get(item.netId());
        Assertions.assertEquals(item, obtained);
    }

    @Test
    void get() throws DataAccessException {
        Collection<QueueItem> queue = generateQueue(3);
        for (QueueItem item : queue){
            dao.add(item);
            QueueItem obtained = dao.get(item.netId());
            Assertions.assertEquals(item, obtained);
        }
    }

    QueueItem generateQueueItem(){
        return new QueueItem(
                DaoTestUtils.generateNetID(DaoTestUtils.generateID()),
                DaoTestUtils.getRandomPhase(),
                //it seems that mysql doesn't support milliseconds...
                Instant.now().minusSeconds(random.nextLong(1,86399)).truncatedTo(ChronoUnit.SECONDS),
                false
        );
    }

    Collection<QueueItem> generateQueue(int numItems){
        ArrayList<QueueItem> list = new ArrayList<>();
        for (int i = 0; i < numItems; i++){
            list.add(generateQueueItem());
        }
        return list;
    }
}
