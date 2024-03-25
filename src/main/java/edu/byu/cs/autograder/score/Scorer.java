package edu.byu.cs.autograder.score;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasUtils;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.*;
import edu.byu.cs.util.PhaseUtils;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class Scorer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Scorer.class);
    
    /**
     * The penalty to be applied per day to a late submission.
     * This is out of 1. So putting 0.1 would be a 10% deduction per day
     */
    private static final float PER_DAY_LATE_PENALTY = 0.1F;
    private final GradingContext gradingContext;

    public Scorer(GradingContext gradingContext) {
        this.gradingContext = gradingContext;
    }

    public Submission score(Rubric rubric, int numCommits) throws GradingException {
        gradingContext.observer().update("Grading...");

        rubric = CanvasUtils.decimalScoreToPoints(gradingContext.phase(), rubric);
        rubric = annotateRubric(rubric);

        int daysLate = new LateDayCalculator().calculateLateDays(gradingContext.phase(), gradingContext.netId());
        float thisScore = calculateScoreWithLatePenalty(rubric, daysLate);
        Submission thisSubmission;

        // prevent score from being saved to canvas if it will lower their score
        if(rubric.passed()) {
            float highestScore = getCanvasScore();

            // prevent score from being saved to canvas if it will lower their score
            if (thisScore <= highestScore && gradingContext.phase() != Phase.Phase5 && gradingContext.phase() != Phase.Phase6) {
                String notes = "Submission did not improve current score. (" + (highestScore * 100) +
                        "%) Score not saved to Canvas.\n";
                thisSubmission = saveResults(rubric, numCommits, daysLate, thisScore, notes);
            } else {
                thisSubmission = saveResults(rubric, numCommits, daysLate, thisScore, "");
                sendToCanvas(thisSubmission, 1 - (daysLate * PER_DAY_LATE_PENALTY));
            }
        }
        else {
            thisSubmission = saveResults(rubric, numCommits, daysLate, thisScore, "");
        }
        return thisSubmission;
    }

    /**
     * Annotates the rubric with notes and passed status
     *
     * @param rubric the rubric to annotate
     * @return the annotated rubric
     */
    private Rubric annotateRubric(Rubric rubric) {
        return new Rubric(
                rubric.passoffTests(),
                rubric.unitTests(),
                rubric.quality(),
                passed(rubric),
                rubric.notes()
        );
    }

    private boolean passed(Rubric rubric) {
        boolean passed = true;

        if (rubric.passoffTests() != null && rubric.passoffTests().results() != null)
            if (rubric.passoffTests().results().score() < rubric.passoffTests().results().possiblePoints())
                passed = false;

        return passed;
    }

    private float calculateScoreWithLatePenalty(Rubric rubric, int numDaysLate) throws GradingException {
        float score = getScore(rubric);
        score -= numDaysLate * PER_DAY_LATE_PENALTY;
        if (score < 0) score = 0;
        return score;
    }

    /**
     * Gets the score for the phase
     *
     * @return the score
     */
    protected float getScore(Rubric rubric) throws GradingException {
        int totalPossiblePoints = DaoService.getRubricConfigDao().getPhaseTotalPossiblePoints(gradingContext.phase());

        if (totalPossiblePoints == 0)
            throw new GradingException("Total possible points for phase " + gradingContext.phase() + " is 0");

        float score = 0;
        if (rubric.passoffTests() != null)
            score += rubric.passoffTests().results().score();

        if (rubric.unitTests() != null)
            score += rubric.unitTests().results().score();

        if (rubric.quality() != null)
            score += rubric.quality().results().score();

        return score / totalPossiblePoints;
    }

    /**
     * gets the score stored in canvas for the current user and phase
     * @return score. returns 1.0 for a score of 100%. returns 0.5 for a score of 50%.
     */
    private float getCanvasScore() throws GradingException {
        User user = DaoService.getUserDao().getUser(gradingContext.netId());

        int userId = user.canvasUserId();

        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(gradingContext.phase());
        try {
            CanvasSubmission submission =
                    CanvasIntegration.getCanvasIntegration().getSubmission(userId, assignmentNum);
            int totalPossiblePoints = DaoService.getRubricConfigDao().getPhaseTotalPossiblePoints(gradingContext.phase());
            return submission.score() == null ? 0 : submission.score() / totalPossiblePoints;
        } catch (CanvasException e) {
            throw new GradingException(e);
        }
    }

    /**
     * Saves the results of the grading to the database if the submission passed
     *
     * @param rubric the rubric for the phase
     */
    private Submission saveResults(Rubric rubric, int numCommits, int numDaysLate, float score, String notes)
            throws GradingException {
        String headHash = getHeadHash();

        if (numDaysLate > 0)
            notes += numDaysLate + " days late. -" + (numDaysLate * 10) + "%";

        // FIXME: this is code duplication from calculateLateDays()
        ZonedDateTime handInDate = DaoService.getQueueDao().get(gradingContext.netId()).timeAdded().atZone(ZoneId.of("America/Denver"));

        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        Submission submission = new Submission(
                gradingContext.netId(),
                gradingContext.repoUrl(),
                headHash,
                handInDate.toInstant(),
                gradingContext.phase(),
                rubric.passed(),
                score,
                numCommits,
                notes,
                rubric
        );

        submissionDao.insertSubmission(submission);
        return submission;
    }

    private String getHeadHash() throws GradingException {
        String headHash;
        try (Git git = Git.open(gradingContext.stageRepo())) {
            headHash = git.getRepository().findRef("HEAD").getObjectId().getName();
        } catch (IOException e) {
            throw new GradingException("Failed to get head hash: " + e.getMessage());
        }
        return headHash;
    }

    private void sendToCanvas(Submission submission, float lateAdjustment) throws GradingException {
        UserDao userDao = DaoService.getUserDao();
        User user = userDao.getUser(gradingContext.netId());

        int userId = user.canvasUserId();

        int assignmentNum = PhaseUtils.getPhaseAssignmentNumber(gradingContext.phase());

        RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(gradingContext.phase());
        Map<String, Float> scores = new HashMap<>();
        Map<String, String> comments = new HashMap<>();

        convertToCanvasFormat(submission.rubric().passoffTests(), lateAdjustment, rubricConfig.passoffTests(), scores, comments, Rubric.RubricType.PASSOFF_TESTS);
        convertToCanvasFormat(submission.rubric().unitTests(), lateAdjustment, rubricConfig.unitTests(), scores, comments, Rubric.RubricType.UNIT_TESTS);
        convertToCanvasFormat(submission.rubric().quality(), lateAdjustment, rubricConfig.quality(), scores, comments, Rubric.RubricType.QUALITY);

        try {
            CanvasIntegration.getCanvasIntegration().submitGrade(userId, assignmentNum, scores, comments, submission.notes());
        } catch (CanvasException e) {
            LOGGER.error("Error submitting to canvas for user " + submission.netId(), e);
            throw new GradingException("Error contacting canvas to record scores");
        }

    }

    private void convertToCanvasFormat(Rubric.RubricItem rubricItem, float lateAdjustment,
                                       RubricConfig.RubricConfigItem rubricConfigItem, Map<String, Float> scores,
                                       Map<String, String> comments, Rubric.RubricType rubricType)
            throws GradingException {
        if (rubricConfigItem != null && rubricConfigItem.points() > 0) {
            String id = PhaseUtils.getCanvasRubricId(rubricType, gradingContext.phase());
            Rubric.Results results = rubricItem.results();
            scores.put(id, results.score() * lateAdjustment);
            comments.put(id, results.notes());
        }
    }
}
