package edu.byu.cs.autograder.score.latepenalties;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Rubric;

public interface LatePenaltyCalculator {
    Rubric applyLatePenalty(Rubric rubric, int daysLate, GradingContext gradingContext) throws DataAccessException;
    String makeLatePenaltyNotes(int numDaysLate, int maxLateDays, String origNotes);
}
