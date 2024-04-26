package edu.byu.cs.autograder;

import edu.byu.cs.autograder.test.TestAnalyzer;

public class GradingException extends Exception{
    private String details;
    private TestAnalyzer.TestAnalysis analysis;

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
        this.details = details;
    }

    public GradingException(String message, String details, Throwable cause) {
        super(message, cause);
        this.details = details;
    }

    public GradingException(Throwable cause) {
        super(cause);
    }

    public GradingException(String message, TestAnalyzer.TestAnalysis analysis) {
        super(message);
        this.analysis = analysis;
    }

    public String getDetails() {
        return details;
    }

    public TestAnalyzer.TestAnalysis getAnalysis() {
        return analysis;
    }
}
