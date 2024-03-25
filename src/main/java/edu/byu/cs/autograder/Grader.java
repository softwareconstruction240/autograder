package edu.byu.cs.autograder;

import edu.byu.cs.autograder.compile.CompileHelper;
import edu.byu.cs.autograder.database.DatabaseHelper;
import edu.byu.cs.autograder.git.GitHelper;
import edu.byu.cs.autograder.quality.QualityGrader;
import edu.byu.cs.autograder.score.Scorer;
import edu.byu.cs.autograder.test.PassoffTestGrader;
import edu.byu.cs.autograder.test.UnitTestGrader;
import edu.byu.cs.model.*;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Instant;

/**
 * A template for fetching, compiling, and running student code
 */
public class Grader implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Grader.class);

    private final DatabaseHelper dbHelper;

    private final GitHelper gitHelper;

    private final CompileHelper compileHelper;

    protected final GradingContext gradingContext;

    protected Observer observer;

    /**
     * Creates a new grader
     *
     * @param repoUrl  the url of the student repo
     * @param netId    the netId of the student
     * @param observer the observer to notify of updates
     * @param phase    the phase to grade
     */
    public Grader(String repoUrl, String netId, Observer observer, Phase phase, boolean admin) throws IOException {
        String phasesPath = new File("./phases").getCanonicalPath();
        long salt = Instant.now().getEpochSecond();
        String stagePath = new File("./tmp-" + repoUrl.hashCode() + "-" + salt).getCanonicalPath();
        File stageRepo = new File(stagePath, "repo");
        int requiredCommits = 10;
        this.observer = observer;
        this.gradingContext =
                new GradingContext(netId, phase, phasesPath, stagePath, repoUrl, stageRepo, requiredCommits, observer, admin);
        this.dbHelper = new DatabaseHelper(salt, gradingContext);
        this.gitHelper = new GitHelper(gradingContext);
        this.compileHelper = new CompileHelper(gradingContext);
    }

    public void run() {
        observer.notifyStarted();
        try {
            // FIXME: remove this sleep. currently the grader is too quick for the client to keep up
            Thread.sleep(1000);
            int numCommits = gitHelper.setUp();
            dbHelper.setUp();
            compileHelper.compile();

            RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
            Rubric.Results qualityResults = null;
            if(rubricConfig.quality() != null) {
                qualityResults = new QualityGrader(gradingContext).runQualityChecks();
            }

            Rubric.Results passoffResults = null;
            if(rubricConfig.passoffTests() != null) {
                passoffResults = new PassoffTestGrader(gradingContext).runTests();
            }
            Rubric.Results customTestsResults = null;
            if(rubricConfig.unitTests() != null) {
                customTestsResults = new UnitTestGrader(gradingContext).runTests();
            }

            dbHelper.finish();

            Rubric.RubricItem qualityItem = null;
            Rubric.RubricItem passoffItem = null;
            Rubric.RubricItem customTestsItem = null;

            if (qualityResults != null)
                qualityItem = new Rubric.RubricItem(rubricConfig.quality().category(), qualityResults, rubricConfig.quality().criteria());
            if (passoffResults != null)
                passoffItem = new Rubric.RubricItem(rubricConfig.passoffTests().category(), passoffResults, rubricConfig.passoffTests().criteria());
            if (customTestsResults != null)
                customTestsItem = new Rubric.RubricItem(rubricConfig.unitTests().category(), customTestsResults, rubricConfig.unitTests().criteria());

            Rubric rubric = new Rubric(passoffItem, customTestsItem, qualityItem, false, "");

            Submission submission = new Scorer(gradingContext).score(rubric, numCommits);

            observer.notifyDone(submission);

        } catch (GradingException ge) {
            if(ge.getDetails() == null) observer.notifyError(ge.getMessage());
            else observer.notifyError(ge.getMessage(), ge.getDetails());
            String notification =
                    "Error running grader for user " + gradingContext.netId() + " and repository " + gradingContext.repoUrl();
            if(ge.getDetails() != null) notification += ". Details:\n" + ge.getDetails();
            LOGGER.error(notification, ge);
        }
        catch (Exception e) {
            observer.notifyError(e.getMessage());
            LOGGER.error("Error running grader for user " + gradingContext.netId() + " and repository " + gradingContext.repoUrl(), e);
        } finally {
            dbHelper.cleanUp();
            FileUtils.removeDirectory(new File(gradingContext.stagePath()));
        }
    }


    public interface Observer {
        void notifyStarted();

        void update(String message);

        void notifyError(String message);

        void notifyError(String message, String details);

        void notifyDone(Submission submission);
    }

}
