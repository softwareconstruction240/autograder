package edu.byu.cs.autograder.score;

import edu.byu.cs.canvas.FakeCanvasIntegration;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.Phase;

import java.time.ZonedDateTime;

public class MockLateDayCalculator extends LateDayCalculator {
    private final LateDayContext fixedContext;

    public MockLateDayCalculator() {
        this(0, 0, DaoService.createMemoryDAOs());
    }

    public MockLateDayCalculator(int daysBeforeDueDate, int maxLateDaysToPenalize, DaoService daoService) {
        super(new FakeCanvasIntegration(daoService.getUserDao()), null, daoService.getConfigurationDao(),
                daoService.getUserDao(), daoService.getQueueDao());
        initializePublicHolidays(); // Initialize as blank; without reading default values
        var now = ZonedDateTime.now();
        fixedContext = new LateDayContext(now, now.minusDays(daysBeforeDueDate), maxLateDaysToPenalize);
    }

    @Override
    protected LateDayContext doFetchLateDayContext(Phase phase, String netId) {
        return fixedContext;
    }

    @Override
    protected void assertFutureHolidaysConfigured() throws RuntimeException {
        // Disable these checks.
        // Do not require tests to be chronologically up-to-date.
    }
}
