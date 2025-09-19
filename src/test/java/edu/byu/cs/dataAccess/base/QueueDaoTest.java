package edu.byu.cs.dataAccess.base;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.model.QueueItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public abstract class QueueDaoTest {
    protected QueueDao dao;
    protected abstract QueueDao getQueueDao();
    protected abstract void clearQueueItems() throws DataAccessException;
    static Random random = new Random();

    static IntStream queueSizeRange() {
        return IntStream.of(0,1,2,3,300);
    }

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

    @ParameterizedTest
    @MethodSource ("queueSizeRange")
    void remove(int queueSize) throws DataAccessException {
        Collection<QueueItem> queue = generateQueue(queueSize);
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

    @ParameterizedTest
    @MethodSource ("quietlyFailingFunctions")
    void executeOnItemThatDoesNotExist(NamedAction namedAction) throws DataAccessException{
        QueueItem item = generateQueueItem();
        Assertions.assertTrue(dao.getAll().isEmpty());
        Assertions.assertDoesNotThrow(() -> namedAction.action.execute(dao, item));
        Assertions.assertTrue(dao.getAll().isEmpty());
    }

    static Stream<NamedAction> quietlyFailingFunctions(){
        return Stream.of(
                new NamedAction("remove", (dao, item) -> dao.remove(item.netId())),
                new NamedAction("markStarted", (dao, item) -> dao.markStarted(item.netId())),
                new NamedAction("markNotStarted", (dao, item) -> dao.markNotStarted(item.netId())),
                new NamedAction("get", (dao, item) -> dao.get(item.netId()))
        );
    }

    @FunctionalInterface
    interface QueueDaoAction {
        void execute (QueueDao dao, QueueItem item) throws DataAccessException;
    }

    record NamedAction(String name, QueueDaoAction action){
        @Override
        public String toString(){
            return name;
        }
    }

    @ParameterizedTest
    @MethodSource ("queueSizeRange")
    void getAll(int queueSize) throws DataAccessException {
        Collection<QueueItem> queue = generateQueue(queueSize);
        for (QueueItem item : queue){
            dao.add(item);
        }
        Collection<QueueItem> obtainedQueue = dao.getAll();
        Assertions.assertEquals(queue.size(), obtainedQueue.size());
        Assertions.assertTrue(obtainedQueue.containsAll(queue));
        QueueItem popped = null;
        for (QueueItem item : obtainedQueue){
            if (popped != null && !popped.timeAdded().equals(item.timeAdded())) {
                Assertions.assertTrue(item.timeAdded().isAfter(popped.timeAdded()));
            }
            popped = item;
        }
    }

    @Test
    void isAlreadyInQueue() throws DataAccessException {
        QueueItem item = generateQueueItem();
        Assertions.assertFalse(dao.isAlreadyInQueue(item.netId()));
        dao.add(item);
        Assertions.assertTrue(dao.isAlreadyInQueue(item.netId()));
    }

    @ParameterizedTest
    @MethodSource ("queueSizeRange")
    void markStarted(int queueSize) throws DataAccessException {
        Collection<QueueItem> queueNoise = generateQueue(queueSize);
        for (QueueItem item : queueNoise){
            dao.add(item);
        }
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

    @ParameterizedTest
    @MethodSource ("queueSizeRange")
    void markNotStarted(int queueSize) throws DataAccessException {
        Collection<QueueItem> queueNoise = generateQueue(queueSize);
        for (QueueItem item : queueNoise){
            dao.add(item);
        }
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

    @ParameterizedTest
    @MethodSource ("queueSizeRange")
    void get(int queueSize) throws DataAccessException {
        Collection<QueueItem> queue = generateQueue(queueSize);
        for (QueueItem item : queue){
            dao.add(item);
            QueueItem obtained = dao.get(item.netId());
            Assertions.assertEquals(item, obtained);
        }
    }

    @Test
    void getNonExistentItemReturnsNull() throws DataAccessException{
        QueueItem item = generateQueueItem();
        Assertions.assertNull((dao.get(item.netId())));
    }

    QueueItem generateQueueItem(){
        return new QueueItem(
                DaoTestUtils.generateNetID(DaoTestUtils.generateID()),
                DaoTestUtils.getRandomPhase(),
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
