package edu.byu.cs.controller;

public class PriorRepoClaimBlockageException extends Exception {
    private static final String secrets = "btw im a teapot";

    public PriorRepoClaimBlockageException(String message) {
        super(message);
    }
}
