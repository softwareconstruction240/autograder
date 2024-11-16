package edu.byu.cs.model;

public record User(
        String netId,
        int canvasUserId,
        String firstName,
        String lastName,
        String repoUrl,
        Role role

) {
    public enum Role {
        STUDENT,
        ADMIN;

        public static Role parse(String roleString) throws IllegalArgumentException {
            return Role.valueOf(roleString.toUpperCase());
        }
    }
}
