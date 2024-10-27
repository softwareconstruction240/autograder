package edu.byu.cs.controller.httpexception;

public class BadRequestException extends Exception {
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    public BadRequestException() {
        super();
    }
}
