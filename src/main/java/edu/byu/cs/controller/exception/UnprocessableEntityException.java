package edu.byu.cs.controller.exception;

/**
 * Throws whenever a request is well-formed but cannot be processed
 */
public class UnprocessableEntityException extends Exception {
    public UnprocessableEntityException(String message) {
        super(message);
    }
}
