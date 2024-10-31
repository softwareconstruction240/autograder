package edu.byu.cs.controller;

public class InternalServerException extends Exception {
    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
