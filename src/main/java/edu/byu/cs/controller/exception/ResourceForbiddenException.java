package edu.byu.cs.controller.exception;

/**
 * Throws whenever a non-admin user tries to access admin resources
 */
public class ResourceForbiddenException extends Exception {
    public ResourceForbiddenException() {
        super();
    }
}
