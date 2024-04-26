package edu.byu.cs.dataAccess;

import edu.byu.cs.model.QueueItem;

import java.util.Collection;

public interface QueueDao {
    /**
     * Adds an item to the queue
     *
     * @param item the item to add
     */
    void add(QueueItem item) throws DataAccessException;

    /**
     * Removes the next item from the queue
     *
     * @return the next item in the queue
     */
    QueueItem pop() throws DataAccessException;

    /**
     * Removes an item from the queue
     *
     * @param netId the netId of the item to remove
     */
    void remove(String netId) throws DataAccessException;

    /**
     * Gets all items in the queue
     *
     * @return all items in the queue
     */
    Collection<QueueItem> getAll() throws DataAccessException;

    /**
     * Gets the number of items in the queue
     *
     * @param netId the netId of the item to check
     * @return true if the item is in the queue, false otherwise
     */
    boolean isAlreadyInQueue(String netId) throws DataAccessException;

    /**
     * Marks an item as being started to be graded
     *
     * @param netId the netId of the item to mark
     */
    void markStarted(String netId) throws DataAccessException;

    void markNotStarted(String netId) throws DataAccessException;

    /**
     * Gets an item from the queue
     *
     * @param netId the netId of the item to get
     * @return the item with the given netId
     */
    QueueItem get(String netId) throws DataAccessException;
}
