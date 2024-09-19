package edu.byu.cs.model;

import org.eclipse.jgit.annotations.Nullable;

import java.time.Instant;

/**
 * Represents an update to a student's repo url so that changes can be logged in the database
 * @param timestamp When the repo url was updated
 * @param netId The netId of the student whose repo url was changed
 * @param repoUrl The new repoUrl
 * @param adminUpdate Flags if the url was updated by an admin
 * @param adminNetId Null if update was done by a student, will contain the admin's netId if an admin updated it
 */
public record RepoUpdate(
        Instant timestamp,
        String netId,
        String repoUrl,
        boolean adminUpdate,
        @Nullable String adminNetId
        ) {

        @Override
        public String toString() {
                String adminInfo = adminUpdate ? "\nadminNetId='" + adminNetId + '\'' : "";
                return "RepoUpdate{" +
                        "Timestamp:" + timestamp +
                        "\nnetId='" + netId + '\'' +
                        "\nrepoUrl='" + repoUrl + '\'' +
                        adminInfo +
                        '}';
        }
}
