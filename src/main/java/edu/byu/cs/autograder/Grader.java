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
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.dataAccess.daoInterface.SubmissionDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
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
    private final ConfigurationDao configurationDao;
    private final RubricConfigDao rubricConfigDao;
    private final SubmissionDao submissionDao;
    private final UserDao userDao;
    private final QueueDao queueDao;

    /**
     * Creates a new grader
     *
     * @param repoUrl  the url of the student repo
     * @param netId    the netId of the student
     * @param observer the observer to notify of updates
     * @param phase    the phase to grade
     */
    public Grader(String repoUrl, String netId, GradingObserver observer, Phase phase, boolean admin,
                  CanvasIntegration canvasIntegration, ConfigurationDao configurationDao,
                  RubricConfigDao rubricConfigDao, SubmissionDao submissionDao, UserDao userDao,
                  QueueDao queueDao) throws IOException, GradingException {
        this.configurationDao = configurationDao;
        this.rubricConfigDao = rubricConfigDao;
        this.submissionDao = submissionDao;
        this.userDao = userDao;
        this.queueDao = queueDao;
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
                PhaseUtils.verificationConfig(phase, configurationDao) : null;
        this.observer = observer;
        this.gradingContext = new GradingContext(
                    netId, phase, phasesPath, stagePath, repoUrl, stageRepo,
                    cvConfig, observer, admin);

        // Init helpers
        LateDayCalculator lateDayCalculator = new LateDayCalculator(canvasIntegration,
                configurationDao, userDao, queueDao);
        this.dbHelper = new DatabaseHelper(salt, gradingContext);
        this.gitHelper = new GitHelper(gradingContext, new DefaultGitVerificationStrategy(lateDayCalculator),
                submissionDao, queueDao);
        this.compileHelper = new CompileHelper(gradingContext);

        this.scorer = new Scorer(gradingContext, lateDayCalculator, canvasIntegration,
                configurationDao, rubricConfigDao, submissionDao, userDao, queueDao);
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
                new PreviousPhasePassoffTestGrader(gradingContext, configurationDao, rubricConfigDao).runTests();
            }

            RubricConfig rubricConfig = rubricConfigDao.getRubricConfig(gradingContext.phase());
            Rubric rubric = evaluateProject(RUN_COMPILATION ? rubricConfig : null, commitVerificationReport.result());

            Submission submission = scorer.score(rubric, commitVerificationReport);
            submissionDao.insertSubmission(submission);

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
                    case PASSOFF_TESTS -> new PassoffTestGrader(gradingContext, configurationDao, rubricConfigDao).runTests();
                    case UNIT_TESTS -> new UnitTestGrader(gradingContext, configurationDao, rubricConfigDao).runTests();
                    case EXTRA_CREDIT -> new ExtraCreditGrader(gradingContext, configurationDao, rubricConfigDao).runTests();
                    case QUALITY -> new QualityGrader(gradingContext, rubricConfigDao).runQualityChecks();
                    case GITHUB_REPO -> new GitHubAssignmentGrader(rubricConfigDao).grade(commitVerificationResult);
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
            Submission submission = scorer.generateSubmissionObject(ge.asRubric(), cvr, 0, new Rubric.ScorePair(0f, 0f), ge.getMessage());
            submissionDao.insertSubmission(submission);
            observer.notifyError(ge.getMessage(), submission);
        } catch (Exception ex) {
            observer.notifyError(ex.getMessage() + "\n" + ge.getMessage());
        }
    }

}
