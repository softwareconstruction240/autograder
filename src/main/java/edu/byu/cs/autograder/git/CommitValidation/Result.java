package edu.byu.cs.autograder.git.CommitValidation;

import java.util.ArrayList;
import java.util.Collection;

public record Result(
        Collection<String> messages,
        int commitsAffected
) {

    @FunctionalInterface
    public interface MessageTerminatedVisitor {
        void finish(Collection<String> producedMessages);
    }

    /**
     * Evaluates multiple {@link CV} CommitVerification records, and
     * adds only the strings corresponding to failed tests to a resulting Collection.
     *
     * @param assertedConditions A list of pre-evaluated, pre-populated strings and conditions.
     * @param visitor Will be given the opportunity to modify the messages collection <b>only</b> if it is non-empty at the end.
     * @return A {@link Collection<String>} of messages that failed evaluations which can be shown to the user.
     */
    public static Result evaluateConditions(CV[] assertedConditions, MessageTerminatedVisitor visitor) {
        ArrayList<String> messages = new ArrayList<>();
        int commitsAffected = 0;
        for (CV assertedCondition : assertedConditions) {
            if (!assertedCondition.fails()) continue;
            messages.add(assertedCondition.errorMsg());
            commitsAffected += assertedCondition.commitsAffected();
        }

        if (!messages.isEmpty()) {
            visitor.finish(messages);
        }
        return new Result(messages, commitsAffected);
    }

    public boolean isEmpty() {
        return messages().isEmpty();
    }

}
