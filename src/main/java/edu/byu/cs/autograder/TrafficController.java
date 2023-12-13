package edu.byu.cs.autograder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrafficController {
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private static final TrafficController trafficController = new TrafficController();

    public static TrafficController getInstance() {
        return trafficController;
    }

    /**
     * Adds a grader to the queue. The grader will be run when there is an available thread
     * @param grader the grader to add
     */
    public void addGrader(Grader grader) {
        executorService.submit(grader);
    }
}
