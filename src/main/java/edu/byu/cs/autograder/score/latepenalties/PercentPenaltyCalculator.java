package edu.byu.cs.autograder.score.latepenalties;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.Submission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class PercentPenaltyCalculator implements LatePenaltyCalculator{

    private static final Logger LOGGER = LoggerFactory.getLogger(PercentPenaltyCalculator.class);

    /**
     * The penalty to be applied per day to a late submission.
     * This is out of 1. So putting 0.1 would be a 10% deduction per day
     */
    private final float PER_DAY_LATE_PENALTY;

    public PercentPenaltyCalculator(){
        try {
            ConfigurationDao dao = DaoService.getConfigurationDao();
            PER_DAY_LATE_PENALTY = dao.getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);
        } catch (DataAccessException e) {
            LOGGER.error("Error while getting Per Day Late Penalty for Scorer.");
            throw new RuntimeException(e);
        }
    }

    public String makeLatePenaltyNotes(int numDaysLate, int maxLateDays, String origNotes) {
        if (numDaysLate <= 0) {
            return origNotes;
        }

        String penaltyPercentage = String.format("-%d%%", (int)(numDaysLate * PER_DAY_LATE_PENALTY * 100));
        String lateNotes;
        if (numDaysLate >= maxLateDays) {
            lateNotes = "Late penalty maxed out: " + penaltyPercentage;
        } else {
            lateNotes = String.format("%d days late: %s", numDaysLate, penaltyPercentage);
        }

        if (origNotes == null || origNotes.isBlank()) {
            return lateNotes;
        }
        return String.format("%s\n%s", origNotes, lateNotes);
    }

    public Rubric applyLatePenalty(Rubric rubric, int daysLate, GradingContext gradingContext) throws DataAccessException {
        Collection<Submission> previousSubmissions = DaoService.getSubmissionDao().getSubmissionsForPhase(gradingContext.netId(), gradingContext.phase());
        EnumMap<Rubric.RubricType, Rubric.RubricItem> items = new EnumMap<>(Rubric.RubricType.class);
        float lateScoreMultiplier = 1 - (daysLate * PER_DAY_LATE_PENALTY);
        Integer maxLateDays = DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);
        for (Map.Entry<Rubric.RubricType, Rubric.RubricItem> entry : rubric.items().entrySet()) {
            Rubric.RubricType rubricType = entry.getKey();
            Rubric.RubricItem rubricItem = entry.getValue();
            rubricItem = addLateNotesToRubricItem(rubricItem, daysLate, maxLateDays);
            Rubric.Results results = mergeResultsWithPrevious(rubricType, rubricItem, previousSubmissions, lateScoreMultiplier);
            rubricItem = new Rubric.RubricItem(rubricItem.category(), results, rubricItem.criteria());
            items.put(rubricType, rubricItem);
        }
        return new Rubric(items, rubric.passed(), rubric.notes());
    }

    private Rubric.RubricItem addLateNotesToRubricItem(Rubric.RubricItem rubricItem, int daysLate, int maxLateDays){
        Rubric.Results results = rubricItem.results();
        results = new Rubric.Results(
                makeLatePenaltyNotes(daysLate, maxLateDays, results.notes()),
                results.score(),
                results.rawScore(),
                results.possiblePoints(),
                results.testResults(),
                results.textResults());
        return new Rubric.RubricItem(rubricItem.category(), results, rubricItem.criteria());
    }

    private Rubric.Results mergeResultsWithPrevious(Rubric.RubricType rubricType, Rubric.RubricItem rubricItem,
                                                    Collection<Submission> previousSubmissions, float scoreMultiplier) {
        Rubric.Results results = rubricItem.results();

        String notes = results.notes();
        float startingScore = results.score() * scoreMultiplier;
        float score = startingScore;

        for (Submission previousSubmission : previousSubmissions) {
            if(previousSubmission.passed()) {
                Rubric.RubricItem previousItem = previousSubmission.rubric().items().get(rubricType);
                if (previousItem != null && previousItem.results().rawScore() <= results.rawScore()) {
                    score = Math.max(score, previousItem.results().score());
                }
            }
        }

        if(score > startingScore) {
            notes = String.format("Deferring to less-penalized prior score of %s/%d\n%s",
                    Math.round(score * 100) / 100.0, rubricItem.results().possiblePoints(), notes);
        }

        return new Rubric.Results(notes,
                score,
                results.score(),
                results.possiblePoints(),
                results.testResults(),
                results.textResults());
    }
}
