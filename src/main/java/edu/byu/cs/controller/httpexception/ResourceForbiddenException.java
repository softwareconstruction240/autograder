package edu.byu.cs.controller.httpexception;

public class ResourceForbiddenException extends Exception {
    public ResourceForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceForbiddenException(String message) {
        super(message);
    }

    public ResourceForbiddenException(Throwable cause) {
        super(cause);
    }

    public ResourceForbiddenException() {
        super();
    }
}
