package edu.byu.cs.autograder.score;

import edu.byu.cs.model.Phase;

import java.time.ZonedDateTime;

public class MockLateDayCalculator extends LateDayCalculator {
    private final LateDayContext fixedContext;

    public MockLateDayCalculator(int daysBeforeDueDate) {
        super();
        var now = ZonedDateTime.now();
        fixedContext = new LateDayContext(now, now.plusDays(daysBeforeDueDate), 0);
    }

    @Override
    protected LateDayContext doFetchLateDayContext(Phase phase, String netId) {
        return fixedContext;
    }
}
