package edu.byu.cs.autograder.test;

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

    public Rubric.Results runTests() throws DataAccessException {
        RubricConfig rubricConfig =
                DaoService.getRubricConfigDao().getRubricConfig(Phase.GitHub);
        RubricConfig.RubricConfigItem configItem =
                rubricConfig.items().get(Rubric.RubricType.GITHUB_REPO);

        return new Rubric.Results(
                "Successfully fetched your repository. If you passed the required commit count, " +
                        "your grade should have been updated in Canvas. You should see this comment " +
                        "in Canvas as well.",
                1f,
                configItem.points(),
                null,
                "If you have any errors in your code or code quality, " +
                        "you will see the details here."
        );
    }

}
