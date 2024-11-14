package edu.byu.cs.controller.exception;

public class UnauthorizedException extends Exception {
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(Throwable cause) {
        super(cause);
    }

    public UnauthorizedException() {
        super();
    }
}
