package edu.byu.cs.model;

public record User(
        String netId,
        String firstName,
        String lastName,
        String repoUrl,
        Role role

) {
    public enum Role {
        STUDENT,
        ADMIN
    }
}
