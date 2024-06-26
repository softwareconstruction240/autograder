package edu.byu.cs.autograder;

import edu.byu.cs.model.Rubric;

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

    public GradingException(Throwable cause) {
        super(cause);
    }

    public GradingException(String message, Rubric.Results results) {
        super(message);
        this.results = results;
    }

    public Rubric asRubric() {
        EnumMap<Rubric.RubricType, Rubric.RubricItem> items = (results == null) ? null :
                new EnumMap<>(Map.of(Rubric.RubricType.GRADING_ISSUE, new Rubric.RubricItem(CATEGORY, results, CRITERIA)));
        return new Rubric(items, false, getMessage());
    }
}
