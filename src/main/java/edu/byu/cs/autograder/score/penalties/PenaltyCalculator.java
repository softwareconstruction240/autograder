package edu.byu.cs.autograder.score.penalties;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Rubric;

public interface PenaltyCalculator {
    Rubric applyPenalty(Rubric rubric, int daysLate, GradingContext gradingContext) throws DataAccessException;
    String makePenaltyNotes(int numDaysLate, int maxLateDays, String origNotes);
}
