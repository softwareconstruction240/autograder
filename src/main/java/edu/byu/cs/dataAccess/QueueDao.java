package edu.byu.cs.dataAccess;

import edu.byu.cs.model.QueueItem;

public interface QueueDao {
    /**
     * Adds an item to the queue
     *
     * @param item the item to add
     */
    void add(QueueItem item);

    /**
     * Removes the next item from the queue
     *
     * @return the next item in the queue
     */
    QueueItem pop();
}
