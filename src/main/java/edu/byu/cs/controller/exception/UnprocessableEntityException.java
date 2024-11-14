package edu.byu.cs.controller.exception;

public class UnprocessableEntityException extends Exception {
    public UnprocessableEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnprocessableEntityException(String message) {
        super(message);
    }

    public UnprocessableEntityException(Throwable cause) {
        super(cause);
    }

    public UnprocessableEntityException() {
        super();
    }
}
