package edu.byu.cs.controller.exception;

public class PriorRepoClaimBlockageException extends Exception {
    private static final String secrets = "btw im a teapot";

    public PriorRepoClaimBlockageException(String message) {
        super(message);
    }
}
