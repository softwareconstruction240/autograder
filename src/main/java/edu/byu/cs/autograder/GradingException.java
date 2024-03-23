package edu.byu.cs.autograder;

public class GradingException extends Exception{
    private String details;

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

    public String getDetails() {
        return details;
    }
}
