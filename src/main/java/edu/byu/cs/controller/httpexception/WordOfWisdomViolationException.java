package edu.byu.cs.controller.httpexception;

public class WordOfWisdomViolationException extends Exception {
    private static final String shortAndStout = "yo im a teapot";

    public WordOfWisdomViolationException(String message, Throwable cause) {
        super(message, cause);
    }

    public WordOfWisdomViolationException(String message) {
        super(message);
    }

    public WordOfWisdomViolationException(Throwable cause) {
        super(cause);
    }

    public WordOfWisdomViolationException() {
        super();
    }
}
