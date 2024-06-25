package edu.byu.cs.autograder;

import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.TestAnalysis;

import java.util.EnumMap;
import java.util.Map;

public class GradingException extends Exception{
    private static final String CATEGORY = "Grading Issue";
    private static final String CRITERIA = "An issue arose while grading this submission";

    private Rubric.Results results;

    public GradingException() {
        super();
    }

    public GradingException(String message) {
        super(message);
    }

    public GradingException(String message, Throwable cause) {
        super(message, cause);
    }

    public GradingException(String message, String details) {
        super(message);
        setDetails(message, details);
    }

    public GradingException(String message, String details, Throwable cause) {
        super(message, cause);
        setDetails(message, details);
    }

    public GradingException(Throwable cause) {
        super(cause);
    }

    public GradingException(String message, TestAnalysis analysis) {
        super(message);
        this.results = new Rubric.Results(message, 0f, 0, analysis, null);
    }

    public Rubric asRubric() {
        EnumMap<Rubric.RubricType, Rubric.RubricItem> items = (results == null) ? null :
                new EnumMap<>(Map.of(Rubric.RubricType.GRADING_ISSUE, new Rubric.RubricItem(CATEGORY, results, CRITERIA)));
        return new Rubric(items, false, getMessage());
    }

    private void setDetails(String message, String details) {
        this.results = new Rubric.Results(message, 0f, 0, null, details);
    }
}
