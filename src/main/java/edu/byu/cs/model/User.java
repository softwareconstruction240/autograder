package edu.byu.cs.model;

public record User(
        String netId,
        String repoUrl,
        Role role

) {
    public enum Role {
        STUDENT,
        ADMIN
    }
}
