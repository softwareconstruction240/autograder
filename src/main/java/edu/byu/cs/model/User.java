package edu.byu.cs.model;

/**
 * Represents a user using the AutoGrader
 *
 * @param netId the netId of the user
 * @param canvasUserId the id of the user in Canvas
 * @param firstName the first name of the user
 * @param lastName the last name of the user
 * @param repoUrl the url for that user's repository stored in the AutoGrader
 * @param role the role of the user (student or admin)
 */
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
