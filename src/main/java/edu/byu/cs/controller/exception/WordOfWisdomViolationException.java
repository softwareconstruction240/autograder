package edu.byu.cs.controller.exception;

/**
 * Throws whenever a user tries claiming a repo url already claimed by another user
 */
public class WordOfWisdomViolationException extends Exception {
    private static final String shortAndStout = "yo im a teapot";

    public WordOfWisdomViolationException(String message) {
        super(message);
    }
}
