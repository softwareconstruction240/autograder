package edu.byu.cs.autograder;

import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

/**
 * Grades the GitHub assignment. If a student passes the commit verification,
 * then this grader will give them full points.
 */
public class GitHubAssignmentGrader {
    private static final String SUCCESS_MESSAGE = "Successfully fetched your repository and verified commits.";
    private static final String RESUBMIT_PROMPT = " Push another commit to your repository and re-request grading to receive points for this assignment.";
    private static final String FAILURE_MESSAGE = "Successfully fetched your repository, but the number of commits is insufficient." + RESUBMIT_PROMPT;

    public Rubric.Results grade(CommitVerificationResult commitVerificationResult) throws DataAccessException {
        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(Phase.GitHub);
        RubricConfig.RubricConfigItem configItem = rubricConfig.items().get(Rubric.RubricType.GITHUB_REPO);

        if(commitVerificationResult.verified()) {
            return new Rubric.Results(SUCCESS_MESSAGE, 1f, configItem.points(), null, null);
        }
        else {
            return new Rubric.Results(FAILURE_MESSAGE, 0f, configItem.points(), null,
                    commitVerificationResult.failureMessage() + RESUBMIT_PROMPT);
        }
    }
}
