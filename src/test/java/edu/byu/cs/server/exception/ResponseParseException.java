package edu.byu.cs.server.exception;

public class ResponseParseException extends Exception {
    public ResponseParseException(String message, Exception cause) {
        super(message, cause);
    }
}
