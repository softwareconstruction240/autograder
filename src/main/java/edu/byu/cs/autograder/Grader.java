package edu.byu.cs.autograder;

import edu.byu.cs.autograder.compile.CompileHelper;
import edu.byu.cs.autograder.database.DatabaseHelper;
import edu.byu.cs.autograder.git.CommitVerificationConfig;
import edu.byu.cs.autograder.git.CommitVerificationResult;
import edu.byu.cs.autograder.git.GitHelper;
import edu.byu.cs.autograder.git.GitHubAssignmentGrader;
import edu.byu.cs.autograder.quality.QualityGrader;
import edu.byu.cs.autograder.score.Scorer;
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
import org.eclipse.jgit.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.EnumMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            repoUrl = cleanRepoUrl(repoUrl);
        }
        String phasesPath = new File("./phases").getCanonicalPath();
        long salt = Instant.now().getEpochSecond();
        String stagePath = new File("./tmp-" + repoUrl.hashCode() + "-" + salt).getCanonicalPath();
        File stageRepo = new File(stagePath, "repo");

        // Init Grading Context
        CommitVerificationConfig cvConfig = PhaseUtils.requiresTAPassoffForCommits(phase) ?
                PhaseUtils.verificationConfig(phase) : null;
        this.observer = observer;
        this.gradingContext = new GradingContext(
                    netId, phase, phasesPath, stagePath, repoUrl, stageRepo,
                    cvConfig, observer, admin);

        // Init helpers
        this.dbHelper = new DatabaseHelper(salt, gradingContext);
        this.gitHelper = new GitHelper(gradingContext);
        this.compileHelper = new CompileHelper(gradingContext);
    }

    public void run() {
        observer.notifyStarted();
        CommitVerificationResult commitVerificationResult = null;
        try {
            // FIXME: remove this sleep. currently the grader is too quick for the client to keep up
            Thread.sleep(1000);
            commitVerificationResult = gitHelper.setUpAndVerifyHistory();
            dbHelper.setUp();
            if (RUN_COMPILATION && gradingContext.phase() != Phase.GitHub) {
                compileHelper.compile();
                new PreviousPhasePassoffTestGrader(gradingContext).runTests();
            }

            RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
            Rubric rubric = evaluateProject(RUN_COMPILATION ? rubricConfig : null);

            Submission submission = new Scorer(gradingContext).score(rubric, commitVerificationResult);
            DaoService.getSubmissionDao().insertSubmission(submission);

            observer.notifyDone(submission);
        } catch (Exception e) {
            GradingException ge = e instanceof GradingException ? (GradingException) e : new GradingException(e);
            handleException(ge, commitVerificationResult);
            LOGGER.error("Error running grader for user {} and repository {}", gradingContext.netId(),
                    gradingContext.repoUrl(), e);
        } finally {
            dbHelper.cleanUp();
            FileUtils.removeDirectory(new File(gradingContext.stagePath()));
        }
    }

    private Rubric evaluateProject(RubricConfig rubricConfig) throws GradingException, DataAccessException {
        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = new EnumMap<>(Rubric.RubricType.class);
        if (rubricConfig == null) {
            return new Rubric(new EnumMap<>(Rubric.RubricType.class), false, "No Rubric Config");
        }

        for(Rubric.RubricType type : Rubric.RubricType.values()) {
            RubricConfig.RubricConfigItem configItem = rubricConfig.items().get(type);
            if(configItem != null) {
                Rubric.Results results = switch (type) {
                    case PASSOFF_TESTS -> new PassoffTestGrader(gradingContext).runTests();
                    case UNIT_TESTS -> new UnitTestGrader(gradingContext).runTests();
                    case QUALITY -> new QualityGrader(gradingContext).runQualityChecks();
                    case GITHUB_REPO -> new GitHubAssignmentGrader().grade();
                    case GIT_COMMITS, GRADING_ISSUE -> null;
                };
                if (results != null) {
                    rubricItems.put(type, new Rubric.RubricItem(configItem.category(), results, configItem.criteria()));
                }
            }
        }

        return new Rubric(rubricItems, false, "");
    }

    /**
     * Cleans the student's by removing trailing characters after the repo name,
     * unless it ends in `.git`.
     *
     * @param repoUrl The student's repository URL.
     * @return Cleaned URL with everything after the repo name stripped off.
     * @throws GradingException Throws IOException if repoUrl does not follow expected format
     */
    public static String cleanRepoUrl(@Nullable  String repoUrl) throws GradingException {
        if (repoUrl == null) {
            throw new GradingException("NULL is not a valid repo URL.");
        }
        String trimmedRepoUrl = repoUrl.trim();

        Pattern pattern;
        Matcher matcher;

        String domainName;
        String githubUsername;
        String repositoryName;
        for (String regexPattern : getRepoRegexPatterns()) {
            pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(trimmedRepoUrl);
            if (matcher.find()) {
                domainName = matcher.group(1).toLowerCase();
                githubUsername = matcher.group(2);
                repositoryName = matcher.group(3);
                return String.format("https://%s/%s/%s", domainName, githubUsername, repositoryName);
            }
        }

        throw new GradingException("Could not validate repo url given '" + repoUrl + "'.");
    }

    /**
     * Generates an array of Strings representing Regexes for url matching purposes.
     * The array allows for matches from any of multiple formats, and the first match will proceed as a success.
     * The regexes will be evaluated in case-insensitive mode on a whitespace-trimmed string.
     * <br>
     * The regexes should be formed to extract the minimum amount of information while accepting the largest breadth
     * of possible urls. This allows the URL to be constructed into a consistent format for uniqueness checking
     * while also being easy for students to use; they should be able to submit any of the URLs from their repo.
     * <br>
     * The regex should contain exactly three <i>capturing groups</i> which will be interpreted as:
     * <ol>
     *     <li>The domain name of the link. This will be converted to lower case for consistency.</li>
     *     <li>The username. Case sensitivity will be preserved.</li>
     *     <li>The repo name. Case sensitivity will be preserved.</li>
     * </ol>
     * @return An array of strings representing regexes.
     */
    private static String[] getRepoRegexPatterns() {
        // NOTE: This is the place where we require GitHub links. The rest of the grading system behind this wall
        // will function properly with any link that can be run with `git clone`, but we have arbitrarily chosen
        // to require "github.com" links for now.
        // NOTE: Acknowledging the current decision, this system has been set up to be domain name agnostic.
        // Depending on how similarly another provider formats their links, it can be simple to accept BitBucket/GitLabs
        // any other repo link in addition to GitHub links.

        // Strictly match the beginning of the URL to ensure it's a github.com domain (and not fake-github.com)
        // GitHub Username may only contain alphanumeric characters or single hyphens, and cannot begin or end with a hyphen.
        // GitHub repository name can only contain ASCII letters, digits, and the characters ., -, and _. The .git extension will be removed.

        // View the playground regex: https://regex101.com/r/r3dDAW/8

        String domainName = "(github\\.com)";
        String userName = "([a-zA-z0-9-]+)";
        String repoName = "([\\w.-]+?)";
        return new String[]{
                "^(?:https?://)?(?:www\\.)?"+domainName+"/"+userName+"/"+repoName+"(?:\\.git|/|\\?|#|$)", // https
                "^git@"+domainName+":"+userName+"/"+repoName+"\\.git$" // ssh
        };
    }


    private void handleException(GradingException ge, CommitVerificationResult cvr) {
        if(cvr == null) {
            observer.notifyError(ge.getMessage());
            return;
        }
        try {
            Submission submission = new Scorer(gradingContext).generateSubmissionObject(ge.asRubric(), cvr, 0, 0f, ge.getMessage());
            DaoService.getSubmissionDao().insertSubmission(submission);
            observer.notifyError(ge.getMessage(), submission);
        } catch (Exception ex) {
            observer.notifyError(ex.getMessage() + "\n" + ge.getMessage());
        }
    }

}
