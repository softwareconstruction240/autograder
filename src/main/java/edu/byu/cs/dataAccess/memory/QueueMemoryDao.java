package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.QueueDao;
import edu.byu.cs.model.QueueItem;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

public class QueueMemoryDao implements QueueDao {

    private static final LinkedList<MutableQueueItem> queueItems = new LinkedList<>();
    private static final Object lock = new Object();

    /**
     * Adds an item to the queue
     *
     * @param item the item to add
     */
    @Override
    public void add(QueueItem item) {
        synchronized (lock) {
            int i = 0;
            while (i < queueItems.size() && queueItems.get(i).get().timeAdded().isBefore(item.timeAdded())) {
                i++;
            }
            if (i >= queueItems.size()) {
                queueItems.add(new MutableQueueItem(item));
            } else {
                queueItems.add(i, new MutableQueueItem(item));
            }
        }
    }

    /**
     * Removes the next item from the queue
     *
     * @return the next item in the queue
     */
    @Override
    public QueueItem pop() {
        synchronized (lock) {
            return queueItems.remove().get();
        }
    }

    /**
     * Removes an item from the queue
     *
     * @param netId the netId of the item to remove
     */
    @Override
    public void remove(String netId) {
        synchronized (lock) {
            queueItems.removeIf(item -> Objects.equals(item.get().netId(), netId));
        }
    }

    /**
     * Gets all items in the queue
     *
     * @return all items in the queue
     */
    @Override
    public Collection<QueueItem> getAll() {
        synchronized (lock) {
            return queueItems.stream().map(MutableQueueItem::get).toList();
        }
    }

    /**
     * Returns true if the item is in the queue
     *
     * @param netId the netId of the item to check
     * @return true if the item is in the queue, false otherwise
     */
    @Override
    public boolean isAlreadyInQueue(String netId) {
        synchronized (lock) {
            return queueItems.stream().anyMatch(item -> Objects.equals(item.get().netId(), netId));
        }
    }

    /**
     * Marks an item as being started to be graded
     *
     * @param netId the netId of the item to mark
     */
    @Override
    public void markStarted(String netId) {
        setStarted(netId, true);
    }

    @Override
    public void markNotStarted(String netId) {
        setStarted(netId, false);
    }

    private void setStarted(String netId, boolean started) {
        synchronized (lock) {
            for (MutableQueueItem mItem : queueItems) {
                if (Objects.equals(mItem.get().netId(), netId)) {
                    mItem.setStarted(started);
                }
            }
        }
    }

    /**
     * Gets an item from the queue
     *
     * @param netId the netId of the item to get
     * @return the item with the given netId
     */
    @Override
    public QueueItem get(String netId) {
        synchronized (lock) {
            for (MutableQueueItem mItem : queueItems) {
                if (Objects.equals(mItem.get().netId(), netId)) {
                    return mItem.get();
                }
            }
        }
        return null;
    }


    static class MutableQueueItem {
        QueueItem item;

        MutableQueueItem(QueueItem item) {
            this.item = item;
        }

        void setStarted(boolean started) {
            this.item = new QueueItem(item.netId(), item.phase(), item.timeAdded(), started);
        }

        public QueueItem get() {
            return item;
        }
    }
}
