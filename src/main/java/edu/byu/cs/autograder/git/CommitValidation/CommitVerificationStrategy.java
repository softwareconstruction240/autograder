package edu.byu.cs.autograder.git.CommitValidation;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DataAccessException;

import java.util.Collection;

public interface CommitVerificationStrategy {
    /**
     * Evaluates the provided <pre>commitContext</pre> and returns a nullable set of Commit hashes to
     * signal a reevaluation without those particular hashes.
     *
     * @param commitContext Precompiled information relating to the passoff state of the commit history.
     * @param gradingContext Commonly used throughout the app. Represents the submission and student being graded.
     */
    void evaluate(CommitVerificationContext commitContext, GradingContext gradingContext) throws GradingException, DataAccessException;

    /**
     * Supplies new commit hashes which will be added to the existing exclude set.
     * <br>
     * If this is non-null and non-empty on a particular invocation, the commits
     * will be re-evaluated and recounted while honoring the new requirements.
     *
     * @return A nullable collection of commit hashes.
     */
    Collection<String> extendExcludeSet();

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
