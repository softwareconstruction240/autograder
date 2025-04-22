package edu.byu.cs.dataAccess.memory;

import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.model.QueueItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class QueueMemoryDao implements QueueDao {
    private final List<QueueItem> queue = new ArrayList<>();
    @Override
    public void add(QueueItem item) {
        queue.add(item);
    }

    @Override
    public QueueItem pop() {
        if (queue.isEmpty()) {
            return null;
        }
        QueueItem item = queue.getFirst();
        queue.removeFirst();
        return item;
    }

    @Override
    public void remove(String netId) {
        queue.removeIf(item -> item.netId().equals(netId));
    }

    @Override
    public Collection<QueueItem> getAll() {
        return queue;
    }

    @Override
    public boolean isAlreadyInQueue(String netId) {
        return queue.stream().anyMatch(item -> item.netId().equals(netId));
    }

    @Override
    public void markStarted(String netId) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).netId().equals(netId)) {
                QueueItem oldItem = queue.get(i);
                QueueItem newItem = new QueueItem(oldItem.netId(), oldItem.phase(), oldItem.timeAdded(), true);
                queue.set(i, newItem);
                return;
            }
        }
    }

    @Override
    public void markNotStarted(String netId) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).netId().equals(netId)) {
                QueueItem oldItem = queue.get(i);
                QueueItem newItem = new QueueItem(oldItem.netId(), oldItem.phase(), oldItem.timeAdded(), false);
                queue.set(i, newItem);
                return;
            }
        }
    }

    @Override
    public QueueItem get(String netId) {
        return queue.stream().filter(item -> item.netId().equals(netId)).findFirst().orElse(null);
    }
}
