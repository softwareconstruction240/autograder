package edu.byu.cs.autograder;

import java.util.concurrent.*;

public class TrafficController {
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);

    private static final TrafficController trafficController = new TrafficController();

    public static TrafficController getInstance() {
        return trafficController;
    }

    public void addGrader(Grader grader) {
        executorService.submit(grader);
    }
}
