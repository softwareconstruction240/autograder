package edu.byu.cs.autograder.git.CommitValidation;

import edu.byu.cs.autograder.GradingContext;

import java.util.Collection;

public interface CommitVerificationStrategy {
    /**
     * Evaluates the provided <pre>commitContext</pre> and returns a nullable set of Commit hashes to
     * signal a reevaluation without those particular hashes.
     *
     * @param commitContext Precompiled information relating to the passoff state of the commit history.
     * @param gradingContext Commonly used throughout the app. Represents the submission and student being graded.
     * @return A nullable collection of commit hashes. If defined and non-empty,
     * the history will be re-evaluated, but those commits will be excluded.
     */
    Collection<String> evaluate(CommitVerificationContext commitContext, GradingContext gradingContext);

    /**
     * Supplies any warnings that were generated during the evaluation the history.
     * @return A nullable {@link Result} class.
     */
    Result getWarnings();

    /** Supplies any errors that wre generated during the evaluation of the history.
     * @return A nullable {@link Result} class.
     */
    Result getErrors();

}
