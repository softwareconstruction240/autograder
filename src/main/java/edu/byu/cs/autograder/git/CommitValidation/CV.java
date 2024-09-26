package edu.byu.cs.autograder.git.CommitValidation;

import java.util.Collection;

/** CommitValidation. Represents a pre-evaluated condition with information.
 *
 * @param fails Reports if this condition has failed. Failed conditions are reported to the user.
 * @param commitsAffected The number of commits to subtract from the total valid commits if this fails.
 * @param errorMsg An error string to display to the user if this fails.
 */
public record CV(
        boolean fails,
        int commitsAffected,
        String errorMsg
) {

    // Overload the constructor
    public CV(
            boolean fails,
            String errorMsg
    ) {
        this(fails, 0, errorMsg);
    }

}
