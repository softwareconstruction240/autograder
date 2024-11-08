package edu.byu.cs.controller.httpexception;

public class InternalServerException extends Exception {
    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
