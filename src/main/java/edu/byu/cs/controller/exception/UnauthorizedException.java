package edu.byu.cs.controller.exception;

/**
 * Throws whenever a user attempts to access resources without correct token or credentials
 */
public class UnauthorizedException extends Exception {
    public UnauthorizedException() {
        super();
    }

    public UnauthorizedException(String message) {
        super(message);
    }
}
