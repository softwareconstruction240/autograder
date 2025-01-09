package edu.byu.cs.autograder.git.CommitValidation;

import java.util.Collection;
import java.util.List;

/** CommitValidation. Represents a pre-evaluated condition with information.
 *
 * @param fails Reports if this condition has failed. Failed conditions are reported to the user.
 * @param commitsAffected A non-null collection of CommitHashes to exclude from the result set if the condition fails.
 * @param errorMsg An error string to display to the user if this fails.
 */
public record CV(
        boolean fails,
        Collection<String> commitsAffected,
        String errorMsg
) {

    // Overload the constructor
    public CV(
            boolean fails,
            String errorMsg
    ) {
        this(fails, List.of(), errorMsg);
    }

}
