package edu.byu.cs.controller.exception;

/**
 * Throws whenever an internal server error occurs
 */
public class InternalServerException extends Exception {
    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
