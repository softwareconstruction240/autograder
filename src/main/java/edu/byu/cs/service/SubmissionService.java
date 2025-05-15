package edu.byu.cs.service;

import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.GradingObserver;
import edu.byu.cs.autograder.GradingObserverImpl;
import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.exception.InternalServerException;
import edu.byu.cs.controller.TrafficController;
import edu.byu.cs.controller.netmodel.ApprovalRequest;
import edu.byu.cs.controller.netmodel.GradeRequest;
import edu.byu.cs.dataAccess.*;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.QueueItem;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import edu.byu.cs.util.SubmissionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

import static edu.byu.cs.util.PhaseUtils.isPhaseEnabled;

/**
 * Contains service logic for the {@link edu.byu.cs.controller.SubmissionController}
 * <br><br>
 * The {@code SubmissionService} provides the following features:
 * <ul>
 *     <li>Submitting a request for grading from either a student or an admin</li>
 *     <li>Determining whether a user is in the queue waiting to be graded</li>
 *     <li>Retrieves the latest submissions</li>
 *     <li>Gets the last submission for a user</li>
 *     <li>Gets all submission from a user (for a phase if specified)</li>
 *     <li>Gets all submissions those in the queue currently being graded as well as those
 *     in the queue waiting to be graded</li>
 *     <li>Approve a submission blocked because of insufficient commits</li>
 *     <li>Rerun submissions currently in the queue</li>
 * </ul>
 */
public class SubmissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionService.class);

    /**
     * Submits a request for grading from a student
     *
     * @param user the student who submitted the request
     * @param request A {@link GradeRequest} containing the student's repo url and the phase
     *                they are submitting for grading
     * @throws BadRequestException if the phase isn't enabled for grading
     * @throws DataAccessException if an error occurred accessing the database
     * @throws InternalServerException if an error occurred starting the grader
     */
    public static void submit(User user, GradeRequest request) throws BadRequestException, DataAccessException, InternalServerException {
        ConfigService.checkForShutdown();

        if (!isPhaseEnabled(request.phase())) {
            throw new BadRequestException("Student submission is disabled for " + request.phase());
        }

        assertHasNewCommits(user, request.phase());

        LOGGER.info("User {} submitted phase {} for grading", user.netId(), request.phase());

        startGrader(user.netId(), request.phase(), user.repoUrl(), false);
    }

    /**
     * Submits a request for grading from an admin
     *
     * @param netId the netId of the admin
     * @param request A {@link GradeRequest} containing a repo url and the phase submitted for grading
     * @throws BadRequestException if the phase isn't enabled for grading
     * @throws DataAccessException if an error occurred accessing the database
     * @throws InternalServerException if an error occurred starting the grader
     */
    public static void adminRepoSubmit(String netId, GradeRequest request) throws DataAccessException, InternalServerException, BadRequestException {
        LOGGER.info("Admin {} submitted phase {} on repo {} for test grading", netId, request.phase(),
                request.repoUrl());

        DaoService.getSubmissionDao().removeSubmissionsByNetId(netId, 3);

        startGrader(netId, request.phase(), request.repoUrl(), true);
    }

    private static void startGrader(String netId, Phase phase, String repoUrl, boolean adminSubmission) throws DataAccessException, BadRequestException, InternalServerException {
        QueueItem qItem = new QueueItem(netId, phase, Instant.now(), false);
        DaoService.getQueueDao().add(qItem);

        try {
            Grader grader = getGrader(netId, phase, repoUrl, adminSubmission);

            TrafficController.getInstance().addGrader(grader);

        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid phase", e);
            throw new BadRequestException("Invalid phase", e);
        } catch (Exception e) {
            LOGGER.error("Error starting grader", e);
            throw new InternalServerException("Error starting grader", e);
        }
    }

    private static void assertHasNewCommits(User user, Phase phase) throws DataAccessException, BadRequestException {
        String headHash;
        try {
            headHash = SubmissionUtils.getRemoteHeadHash(user.repoUrl());
        } catch (DataAccessException e) {
            LOGGER.error("Error getting remote head hash", e);
            throw new BadRequestException("Invalid repo url", e);
        }

        Submission submission = getMostRecentSubmission(user.netId(), phase);
        if (submission != null && submission.headHash().equals(headHash)) {
            throw new BadRequestException("You have already submitted this version of your code for this phase. Make a new commit before submitting again");
        }
    }

    /**
     * Gets the most recent submission for the specified user in the specified phase
     *
     * @param netId the netID of the student to get a submission for
     * @param phase the phase of the project to get
     * @return the most recent submission, or null if there are no submissions for this student in this phase
     * @throws DataAccessException if an error occurs accessing the database
     */
    private static Submission getMostRecentSubmission(String netId, Phase phase) throws DataAccessException {
        Collection<Submission> submissions = DaoService.getSubmissionDao().getSubmissionsForPhase(netId, phase);
        Submission mostRecent = null;

        for (Submission submission : submissions) {
            if (mostRecent == null || mostRecent.timestamp().isBefore(submission.timestamp())) {
                mostRecent = submission;
            }
        }
        return mostRecent;
    }

    /**
     * Returns {@code true} if the requested user is in the queue waiting to be graded
     *
     * @param netId the netId of the user
     * @return {@code true} if the user is in the queue waiting to be graded
     * @throws DataAccessException if an error occurs accessing the database
     */
    public static boolean isAlreadyInQueue(String netId) throws DataAccessException {
        return DaoService.getQueueDao().isAlreadyInQueue(netId);
    }

    /**
     * Gets the last submission for a user
     *
     * @param netId the netId of the user
     * @return the {@link Submission}
     * @throws DataAccessException if an error occurs accessing the database
     */
    public static Submission getLastSubmissionForUser(String netId) throws DataAccessException {
        try {
            return DaoService.getSubmissionDao().getLastSubmissionForUser(netId);
        } catch (DataAccessException e) {
            LOGGER.error("Error getting submissions for user {}", netId, e);
            throw e;
        }
    }

    /**
     * Gets all submissions from a user for a given phase. If {@code phase} is null, then all
     * submission from the user is returned regardless of the phase.
     *
     * @param netId the netId of the user
     * @param phase the phase to get submission from
     * @return all submissions from the user (for a phase specified)
     * @throws DataAccessException if an error occurred accessing the database
     */
    public static Collection<Submission> getXSubmissionsForUser(String netId, Phase phase) throws DataAccessException {
        Collection<Submission> submissions;
        try {
            if (phase == null) {
                submissions = DaoService.getSubmissionDao().getSubmissionsForUser(netId);
            } else {
                submissions = DaoService.getSubmissionDao().getSubmissionsForPhase(netId, phase);
            }
        } catch (DataAccessException e) {
            LOGGER.error("Error getting submissions for user {}", netId, e);
            throw e;
        }
        return submissions;
    }

    /**
     * Gets the latest submissions graded by the AutoGrader
     *
     * @param count the number of submissions to get
     * @return a collection of the latest submissions
     * @throws DataAccessException if an error occurred accessing the database
     */
    public static Collection<Submission> getLatestSubmissions(int count) throws DataAccessException {
        Collection<Submission> submissions;
        try {
            submissions = DaoService.getSubmissionDao().getAllLatestSubmissions(count);
        } catch (DataAccessException e) {
            LOGGER.error("Error getting latest submissions", e);
            throw e;
        }
        return submissions;
    }

    /**
     * Gets all submissions in the queue currently NOT being graded (waiting to be graded)
     *
     * @return a list of netIds of users who submitted and is NOT currently being graded
     * @throws DataAccessException if an error occurs accessing the database
     */
    public static List<String> getActiveInQueue() throws DataAccessException {
        List<String> inQueue;
        try {
            inQueue = DaoService.getQueueDao().getAll().stream().filter((queueItem) -> !queueItem.started()).map(QueueItem::netId).toList();
        } catch (DataAccessException e) {
            LOGGER.error("Error getting active submissions", e);
            throw e;
        }
        return inQueue;
    }

    /**
     * Gets all submissions in the queue currently being graded
     *
     * @return a list of netIds of users who submitted and is currently being graded
     * @throws DataAccessException if an error occurs accessing the database
     */
    public static List<String> getCurrentlyGrading() throws DataAccessException {
        List<String> currentlyGrading;
        try {
            currentlyGrading = DaoService.getQueueDao().getAll().stream().filter(QueueItem::started).map(QueueItem::netId).toList();
        } catch (DataAccessException e) {
            LOGGER.error("Error getting active submissions", e);
            throw e;
        }
        return currentlyGrading;
    }

    /**
     * Gets all submissions from a user
     *
     * @param netId the netId of the user
     * @return all submissions from the user
     * @throws DataAccessException if an error occurs accessing the database
     */
    public static Collection<Submission> getSubmissionsForUser(String netId) throws DataAccessException {
        return getXSubmissionsForUser(netId, null);
    }

    /**
     * Approves a submission for a phase blocked by the AutoGrader due to insufficient commits
     *
     * @param adminNetId the netId of the admin who approved the submission
     * @param request a request containing the information needed to approve the submission.
     *                See {@link ApprovalRequest} for more information.
     * @throws GradingException if no submission was found for the requested phase to be approved
     * or there was an issue submitting the grade to Canvas
     * @throws DataAccessException if an error occurs accessing or modifying submission details
     * in the database
     */
    public static void approveSubmission(String adminNetId, ApprovalRequest request) throws GradingException, DataAccessException {
        int penalty = 0;
        if (request.penalize()) {
            penalty = Math.round((DaoService.getConfigurationDao().getConfiguration(ConfigurationDao.Configuration.GIT_COMMIT_PENALTY, Float.class) * 100));
        }

        SubmissionUtils.approveSubmission(request.netId(), request.phase(), adminNetId, penalty);
    }

    /**
     * Creates a grader for the given request with an observer that sends messages to the subscribed sessions
     *
     * @param netId           the netId of the user
     * @param phase           the phase to grade
     * @param adminSubmission if the grader should run in admin mode
     * @return the grader
     * @throws IOException if there is an error creating the grader
     */
    private static Grader getGrader(String netId, Phase phase, String repoUrl, boolean adminSubmission) throws IOException, GradingException {
        GradingObserver observer = new GradingObserverImpl(netId);
        return new Grader(repoUrl, netId, observer, phase, adminSubmission);
    }

    /**
     * Takes any submissions currently in the queue and reruns them through the grader.
     * Used if the queue got stuck or if the server crashed while submissions were
     * waiting in the queue.
     */
    public static void reRunSubmissionsInQueue() throws IOException, DataAccessException, GradingException {
        QueueDao queueDao = DaoService.getQueueDao();
        UserDao userDao = DaoService.getUserDao();
        Collection<QueueItem> inQueue = queueDao.getAll();

        for (QueueItem queueItem : inQueue) {
            User currentUser = userDao.getUser(queueItem.netId());
            if (currentUser.repoUrl() != null) {
                queueDao.markNotStarted(queueItem.netId());

                TrafficController.getInstance().addGrader(
                        getGrader(queueItem.netId(),
                                queueItem.phase(),
                                currentUser.repoUrl(),
                                currentUser.role() == User.Role.ADMIN));
            } else {
                queueDao.remove(queueItem.netId());
            }
        }
    }

}
