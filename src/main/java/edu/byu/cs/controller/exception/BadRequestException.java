package edu.byu.cs.controller.exception;

/**
 * Throws whenever a user sends an invalid request to the AutoGrader
 */
public class BadRequestException extends Exception {
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(String message) {
        super(message);
    }
}
