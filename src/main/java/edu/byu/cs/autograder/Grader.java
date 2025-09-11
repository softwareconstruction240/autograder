package edu.byu.cs.autograder;

import edu.byu.cs.autograder.compile.CompileHelper;
import edu.byu.cs.autograder.database.DatabaseHelper;
import edu.byu.cs.autograder.git.CommitValidation.DefaultGitVerificationStrategy;
import edu.byu.cs.autograder.git.CommitValidation.CommitVerificationConfig;
import edu.byu.cs.autograder.git.CommitVerificationReport;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.autograder.git.GitHelper;
import edu.byu.cs.autograder.score.LateDayCalculator;
import edu.byu.cs.autograder.quality.QualityGrader;
import edu.byu.cs.autograder.score.Scorer;
import edu.byu.cs.autograder.test.ExtraCreditGrader;
import edu.byu.cs.autograder.test.PassoffTestGrader;
import edu.byu.cs.autograder.test.PreviousPhasePassoffTestGrader;
import edu.byu.cs.autograder.test.UnitTestGrader;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.model.Submission;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.PhaseUtils;
import edu.byu.cs.util.RepoUrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.EnumMap;

/**
 * A template for fetching, compiling, and running student code
 */
public class Grader implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Grader.class);

    /** DEV ONLY. Default: true. Skips compilation and evaluation of student projects. */
    private final boolean RUN_COMPILATION = ApplicationProperties.runCompilation();

    private final DatabaseHelper dbHelper;

    private final GitHelper gitHelper;
    private final CompileHelper compileHelper;

    protected final GradingContext gradingContext;

    protected GradingObserver observer;
    private final Scorer scorer;

    /**
     * Creates a new grader
     *
     * @param repoUrl  the url of the student repo
     * @param netId    the netId of the student
     * @param observer the observer to notify of updates
     * @param phase    the phase to grade
     */
    public Grader(String repoUrl, String netId, GradingObserver observer, Phase phase, boolean admin) throws IOException, GradingException {
        // Init files
        if (!admin) {
            repoUrl = RepoUrlValidator.clean(repoUrl);
        }
        String phasesPath = new File("./phases").getCanonicalPath();
        long salt = Instant.now().getEpochSecond();
        String stagePath = new File("./tmp-" + repoUrl.hashCode() + "-" + salt).getCanonicalPath();
        File stageRepo = new File(stagePath, "repo");

        // Init Grading Context
        CommitVerificationConfig cvConfig = PhaseUtils.shouldVerifyCommits(phase) ?
                PhaseUtils.verificationConfig(phase) : null;
        this.observer = observer;
        this.gradingContext = new GradingContext(
                    netId, phase, phasesPath, stagePath, repoUrl, stageRepo,
                    cvConfig, observer, admin);

        // Init helpers
        LateDayCalculator lateDayCalculator = new LateDayCalculator();
        this.dbHelper = new DatabaseHelper(salt, gradingContext);
        this.gitHelper = new GitHelper(gradingContext, new DefaultGitVerificationStrategy(lateDayCalculator));
        this.compileHelper = new CompileHelper(gradingContext);

        this.scorer = new Scorer(gradingContext, lateDayCalculator);
    }

    /**
     * The main entry point for the {@code Grader} and the high-level grading process of the AutoGrader
     */
    public void run() {
        observer.notifyStarted();
        CommitVerificationReport commitVerificationReport = null;
        try {
            // FIXME: remove this sleep. currently the grader is too quick for the client to keep up
            Thread.sleep(1000);
            commitVerificationReport = gitHelper.setUpAndVerifyHistory();
            dbHelper.setUp();
            if (RUN_COMPILATION && gradingContext.phase() != Phase.GitHub) {
                compileHelper.compile();
                new PreviousPhasePassoffTestGrader(gradingContext).runTests();
            }

            RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
            Rubric rubric = evaluateProject(RUN_COMPILATION ? rubricConfig : null, commitVerificationReport.result());

            Submission submission = scorer.score(rubric, commitVerificationReport);
            DaoService.getSubmissionDao().insertSubmission(submission);

            observer.notifyDone(submission);
        } catch (Exception e) {
            GradingException ge = e instanceof GradingException ? (GradingException) e : new GradingException(e);
            handleException(ge, commitVerificationReport);
            LOGGER.error("Error running grader for user {} and repository {}", gradingContext.netId(),
                    gradingContext.repoUrl(), e);
        } finally {
            dbHelper.cleanUp();
            FileUtils.removeDirectory(new File(gradingContext.stagePath()));
        }
    }

    private Rubric evaluateProject(RubricConfig rubricConfig, CommitVerificationResult commitVerificationResult) throws GradingException, DataAccessException {
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = new EnumMap<>(Rubric.RubricType.class);
        if (rubricConfig == null) {
            return new Rubric(new EnumMap<>(Rubric.RubricType.class), false, "No Rubric Config");
        }

        for(Rubric.RubricType type : Rubric.RubricType.values()) {
            RubricConfig.RubricConfigItem configItem = rubricConfig.items().get(type);
            if(configItem != null) {
                Rubric.Results results = switch (type) {
                    // TODO: How can we fully remove this switch statement and rely on passed-in definitions instead
                    // This code is violating the open-closed principle.
                    case PASSOFF_TESTS -> new PassoffTestGrader(gradingContext).runTests();
                    case UNIT_TESTS -> new UnitTestGrader(gradingContext).runTests();
                    case EXTRA_CREDIT -> new ExtraCreditGrader(gradingContext).runTests();
                    case QUALITY -> new QualityGrader(gradingContext).runQualityChecks();
                    case GITHUB_REPO -> new GitHubAssignmentGrader().grade(commitVerificationResult);
                    case GIT_COMMITS, GRADING_ISSUE -> null;
                    // TODO: (end) This is the end of what we want to remove.
                };
                if (results != null) {
                    rubricItems.put(type, new Rubric.RubricItem(configItem.category(), results, configItem.criteria()));
                }
            }
        }

        return new Rubric(rubricItems, false, "");
    }

    private void handleException(GradingException ge, CommitVerificationReport cvr) {
        if(cvr == null) {
            observer.notifyError(ge.getMessage());
            return;
        }
        try {
            Submission submission = scorer.generateSubmissionObject(ge.asRubric(), cvr, 0, new Scorer.ScorePair(0f, 0f), ge.getMessage());
            DaoService.getSubmissionDao().insertSubmission(submission);
            observer.notifyError(ge.getMessage(), submission);
        } catch (Exception ex) {
            observer.notifyError(ex.getMessage() + "\n" + ge.getMessage());
        }
    }

}
