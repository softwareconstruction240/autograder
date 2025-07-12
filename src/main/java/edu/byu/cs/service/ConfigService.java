package edu.byu.cs.service;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegrationImpl;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao.Configuration;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.model.request.ConfigPenaltyUpdateRequest;
import edu.byu.cs.util.PhaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static edu.byu.cs.util.PhaseUtils.isPhaseEnabled;
import static edu.byu.cs.util.PhaseUtils.isPhaseGraded;

/**
 * Contains service logic for the {@link edu.byu.cs.controller.ConfigController}.
 * <br><br>
 * The {@code ConfigService} provides many of the features relating to getting, generating,
 * and updating several configurable items. Such items include:
 * <ul>
 *     <li>A banner message to show users when they access the AutoGrader</li>
 *     <li>When to shutdown the AutoGrader</li>
 *     <li>What days the AutoGrader should regard as holidays</li>
 *     <li>The live phases students are allowed to submit to</li>
 *     <li>The penalties that should be applied for insufficient commits and late submissions</li>
 *     <li>The course id and Canvas assignments to allow the AutoGrader to access Canvas
 *     and update grades</li>
 * </ul>
 */
public class ConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);
    private static final ConfigurationDao dao = DaoService.getConfigurationDao();

    /**
     * Gets the configuration values that can be read by any user. See {@link PublicConfig}
     * for more information on the configuration values.
     *
     * @return a {@link PublicConfig} with the configuration values
     * @throws DataAccessException if an issue arises getting the configuration values in the database
     */
    public static PublicConfig getPublicConfig() throws DataAccessException {
        String phasesString = dao.getConfiguration(Configuration.STUDENT_SUBMISSIONS_ENABLED, String.class);

        List<Phase> phases;
        if (phasesString != null && !phasesString.isEmpty() && !phasesString.equals("[]")) {
            String cleanString = phasesString
                    .replace("[", "")
                    .replace("]", "");

            if (!cleanString.isEmpty()) {
                phases = Arrays.stream(cleanString.split(","))
                        .map(String::trim)
                        .map(Phase::valueOf)
                        .toList();
            } else {
                phases = new ArrayList<>();
            }
        } else {
            phases = new ArrayList<>();
        }

        return new PublicConfig(
                generateBannerConfig(),
                generateShutdownConfig(),
                phases,
                dao.getConfiguration(Configuration.SLACK_LINK, String.class)
        );
    }

    /**
     * Gets the configuration values that only admins should see. See {@link PrivateConfig}
     * for more information on the configuration values.
     *
     * @return a {@link PrivateConfig} with the configuration values
     * @throws DataAccessException if an issue arises getting the configuration values in the database
     */
    public static PrivateConfig getPrivateConfig() throws DataAccessException {
        return new PrivateConfig(
                generatePenaltyConfig(),
                dao.getConfiguration(Configuration.COURSE_NUMBER, Integer.class),
                generateAssignmentsConfig(),
                generateHolidayConfig()
        );
    }

    //
    // PUBLIC CONFIG GENERATORS
    //
    private static PublicConfig.BannerConfig generateBannerConfig() throws DataAccessException {
        Instant bannerExpiration = dao.getConfiguration(ConfigurationDao.Configuration.BANNER_EXPIRATION, Instant.class);
        if (bannerExpiration.isBefore(Instant.now())) { //Banner has expired
            clearBannerConfig();
        }

        return new PublicConfig.BannerConfig(
                dao.getConfiguration(Configuration.BANNER_MESSAGE, String.class),
                dao.getConfiguration(Configuration.BANNER_LINK, String.class),
                dao.getConfiguration(Configuration.BANNER_COLOR, String.class),
                translateInstantForFrontEnd(bannerExpiration)
        );
    }

    private static PublicConfig.ShutdownConfig generateShutdownConfig() throws DataAccessException {
        checkForShutdown();

        return new PublicConfig.ShutdownConfig(
                translateInstantForFrontEnd(dao.getConfiguration(Configuration.GRADER_SHUTDOWN_DATE, Instant.class)),
                dao.getConfiguration(Configuration.GRADER_SHUTDOWN_WARNING_MILLISECONDS, Integer.class)
        );
    }

    //
    // PRIVATE CONFIG GENERATORS
    //
    private static PrivateConfig.PenaltyConfig generatePenaltyConfig() throws DataAccessException {
        return new PrivateConfig.PenaltyConfig(
                dao.getConfiguration(Configuration.PER_DAY_LATE_PENALTY, Float.class),
                dao.getConfiguration(Configuration.GIT_COMMIT_PENALTY, Float.class),
                dao.getConfiguration(Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class),
                dao.getConfiguration(Configuration.LINES_PER_COMMIT_REQUIRED, Integer.class),
                dao.getConfiguration(Configuration.CLOCK_FORGIVENESS_MINUTES, Integer.class)
        );
    }

    public static ArrayList<PrivateConfig.AssignmentConfig> generateAssignmentsConfig() throws DataAccessException {
        ArrayList<PrivateConfig.AssignmentConfig> assignments = new ArrayList<>();
        for (Phase phase : Phase.values()) {
            if (!isPhaseGraded(phase)) continue;

            int assignmentId = PhaseUtils.getPhaseAssignmentNumber(phase);
            EnumMap<Rubric.RubricType, RubricConfig.RubricConfigItem> rubricConfigItems = DaoService.getRubricConfigDao().getRubricConfig(phase).items();

            assignments.add(new PrivateConfig.AssignmentConfig(phase, assignmentId, rubricConfigItems));
        }
        return assignments;
    }

    public static String[] generateHolidayConfig() throws DataAccessException {
        String encodedDates = dao.getConfiguration(Configuration.HOLIDAY_LIST, String.class);

        if (Objects.equals(encodedDates, "")) {
            return new String[0];
        }
        return encodedDates.split(";");
    }

    //
    // CONFIG SETTERS
    //
    private static void clearBannerConfig() throws DataAccessException {
        dao.setConfiguration(Configuration.BANNER_MESSAGE, "", String.class);
        dao.setConfiguration(Configuration.BANNER_LINK, "", String.class);
        dao.setConfiguration(Configuration.BANNER_COLOR, "", String.class);
        dao.setConfiguration(Configuration.BANNER_EXPIRATION, Instant.MAX, Instant.class);

        logAutomaticConfigChange("Banner message has expired");
    }

    /**
     * Update what phases students are able to submit for
     *
     * @param phasesArray the list of phases that should be counted as live
     * @param user the user who updated the list of live phases
     * @throws DataAccessException if there was an issue updating the live phases in the database
     */
    public static void updateLivePhases(ArrayList phasesArray, User user) throws DataAccessException {
        dao.setConfiguration(Configuration.STUDENT_SUBMISSIONS_ENABLED, phasesArray, ArrayList.class);

        logConfigChange("set the following phases as live: %s".formatted(phasesArray), user.netId());
    }

    /**
     * Set the amount of time the AutoGrader should warn students before shutting down
     *
     * @param user the user who set the shutdown warning duration
     * @param warningMilliseconds the number of milliseconds to set the shutdown warning duration
     * @throws DataAccessException if there was an issue setting the shutdown warning duration
     * in the database
     */
    public static void setShutdownWarningDuration(User user, Integer warningMilliseconds) throws DataAccessException {
        if (warningMilliseconds < 0) {
            throw new IllegalArgumentException("warningMilliseconds must be non-negative");
        }

        dao.setConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_WARNING_MILLISECONDS, warningMilliseconds, Integer.class);

        logConfigChange("set the shutdown warning duration to %s milliseconds".formatted(warningMilliseconds), user.netId());
    }

    /**
     * Schedule the time the AutoGrader will shut down
     *
     * @param user the user who scheduled the shut-down time
     * @param shutdownTimestampString the timestamp at which the AutoGrader will shut down
     * @throws DataAccessException if an issue arises clearing the shutdown schedule or
     * setting the shutdown date in the database
     */
    public static void scheduleShutdown(User user, String shutdownTimestampString) throws DataAccessException {
        if (shutdownTimestampString.isEmpty()) {
            clearShutdownSchedule(user);
            return;
        }

        Instant shutdownTimestamp;
        try {
            shutdownTimestamp = getInstantFromUnzonedTime(shutdownTimestampString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Incomplete timestamp. Send a full timestamp {YYYY-MM-DDTHH:MM:SS} or send none at all", e);
        }

        if (shutdownTimestamp.isBefore(Instant.now())) {
            throw new IllegalArgumentException("You tried to schedule the shutdown in the past");
        }

        dao.setConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_DATE, shutdownTimestamp, Instant.class);
        logConfigChange("scheduled a grader shutdown for %s".formatted(shutdownTimestampString), user.netId());
    }

    /**
     * Shuts down the AutoGrader so students aren't able to submit for grades anymore
     */
    public static void triggerShutdown() {
        try {
            ArrayList<Phase> phases = new ArrayList<>();
            for (Phase phase: Phase.values()) {
                if (!isPhaseGraded(phase) && isPhaseEnabled(phase)) {
                    phases.add(phase);
                }
            }
            dao.setConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, phases, ArrayList.class);
            logAutomaticConfigChange("Student submissions have shutdown. These phases remain active: " + phases);
        } catch (DataAccessException e) {
            LOGGER.error("Something went wrong while shutting down graded phases: " + e.getMessage());
        }
    }

    public static void clearShutdownSchedule() throws DataAccessException {
        clearShutdownSchedule(null);
    }

    /**
     * Clears the shutdown schedule for the AutoGrader
     *
     * @param user the user who is clearing the shutdown schedule
     * @throws DataAccessException if an error occurs clearing the schedule in the database
     */
    public static void clearShutdownSchedule(User user) throws DataAccessException {
        dao.setConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_DATE, Instant.MAX, Instant.class);
        if (user == null) {
            logAutomaticConfigChange("Grader Shutdown Schedule was cleared");
        } else {
            logConfigChange("cleared the Grader Shutdown Schedule", user.netId() );
        }
    }

    /**
     * Update the banner message users can see when they use the AutoGrader
     *
     * @param user the user who created the banner message
     * @param expirationString the timestamp the banner message will expire at
     * @param message the message itself
     * @param link the url the user will be taken to if they click on the banner
     * @param color the color of the background of the banner message
     * @throws DataAccessException if an issue arises updating the banner message in the database
     */
    public static void updateBannerMessage(User user, String expirationString, String message, String link, String color) throws DataAccessException {
        Instant expirationTimestamp = Instant.MAX;
        if (!expirationString.isEmpty()) {
            try {
                expirationTimestamp = getInstantFromUnzonedTime(expirationString);
            } catch (Exception e) {
                throw new IllegalArgumentException("Incomplete timestamp. Send a full timestamp {YYYY-MM-DDTHH:MM:SS} or send none at all", e);
            }
        }

        if (expirationTimestamp.isBefore(Instant.now())) {
            throw new IllegalArgumentException("You tried to set the banner to expire in the past");
        }

        // If they give us a color, and it's not long enough or is missing #
        if (!color.isEmpty() && ((color.length() != 7) || !color.startsWith("#"))) {
            throw new IllegalArgumentException("Invalid hex color code. Must provide a hex code starting with a # symbol, followed by 6 hex digits");
        }

        dao.setConfiguration(Configuration.BANNER_MESSAGE, message, String.class);
        dao.setConfiguration(Configuration.BANNER_LINK, link, String.class);
        dao.setConfiguration(Configuration.BANNER_COLOR, color, String.class);
        dao.setConfiguration(Configuration.BANNER_EXPIRATION, expirationTimestamp, Instant.class);

        if (message.isEmpty()) {
            logConfigChange("cleared the banner message", user.netId());
        } else {
            expirationString = !expirationString.isEmpty() ? expirationString : "never";
            logConfigChange("set the banner message to: '%s' with link: {%s} to expire at %s".formatted(message, link, expirationString), user.netId());
        }

    }

    /**
     * Update the Canvas course id in the database and retrieve the course information from Canvas.
     * If the AutoGrader is unable to retrieve the course information, it will revert
     * to the previous Canvas course id.
     *
     * @param user the user who set the new course id
     * @param newCourseId the new Canvas course id
     * @throws DataAccessException if there is an issue updating the course id or course
     * information in the database
     * @throws CanvasException if an error occurs retrieving course information from Canvas
     */
    public static void setCourseId(User user, Integer newCourseId) throws DataAccessException, CanvasException {
        Integer oldCourseJustInCase = dao.getConfiguration(Configuration.COURSE_NUMBER, Integer.class);
        dao.setConfiguration(Configuration.COURSE_NUMBER, newCourseId, Integer.class);
        logConfigChange("changed the Canvas course ID to %d".formatted(newCourseId), user.netId());

        try {
            // if this line fails, we need to revert back to the previous course id
            updateCourseIdsUsingCanvas(user);
        } catch (CanvasException e) {
            dao.setConfiguration(Configuration.COURSE_NUMBER, oldCourseJustInCase, Integer.class);
            logAutomaticConfigChange("While fetching new course assignment info for course %d, an error occurred. Reverting course id to %d".formatted(newCourseId, oldCourseJustInCase));
            throw e;
        }
    }

    /**
     * Retrieve and store course information in the database from Canvas
     *
     * @param user the user who sent the request
     * @throws CanvasException If an error occurs retrieving course information from Canvas
     * @throws DataAccessException if there is an issue updating course information in the database
     */
    public static void updateCourseIdsUsingCanvas(User user) throws CanvasException, DataAccessException {
        var retriever = new CanvasIntegrationImpl.CourseInfoRetriever();
        retriever.useCourseRelatedInfoFromCanvas();

        logConfigChange(
                "updated course info (assignment IDs, rubric IDs, rubric points) " +
                        "in the database using Canvas",
                user.netId()
        );
    }

    /**
     * Update the commit & late penalties in the database
     *
     * @param user the user making the penalty update request
     * @param request the penalty update request
     * @throws DataAccessException if an error occurs updating the commit penalties in the database
     */
    public static void processPenaltyUpdates(User user, ConfigPenaltyUpdateRequest request) throws DataAccessException {
        validateValidPercentFloat(request.gitCommitPenalty(), "Git Commit Penalty");
        validateValidPercentFloat(request.perDayLatePenalty(), "Per Day Late Penalty");
        validateNonNegativeInt(request.clockForgivenessMinutes(), "Clock Forgiveness Minutes");
        validateNonNegativeInt(request.maxLateDaysPenalized(), "Max Late Days Penalized");
        validateNonNegativeInt(request.linesChangedPerCommit(), "Lines Changed Per Commit");

        setConfigItem(user, Configuration.GIT_COMMIT_PENALTY, request.gitCommitPenalty(), Float.class);
        setConfigItem(user, Configuration.PER_DAY_LATE_PENALTY, request.perDayLatePenalty(), Float.class);
        setConfigItem(user, Configuration.CLOCK_FORGIVENESS_MINUTES, request.clockForgivenessMinutes(), Integer.class);
        setConfigItem(user, Configuration.MAX_LATE_DAYS_TO_PENALIZE, request.maxLateDaysPenalized(), Integer.class);
        setConfigItem(user, Configuration.LINES_PER_COMMIT_REQUIRED, request.linesChangedPerCommit(), Integer.class);
    }

    /**
     * Update the list of holidays the AutoGrader won't count toward the late penalty
     *
     * @param user the user who updated the list of holidays
     * @param holidays the list of holidays
     * @throws DataAccessException if an issue arises updating the list of holidays in the database
     */
    public static void updateHolidays(User user, List<LocalDate> holidays) throws DataAccessException {
        StringBuilder stringBuilder = new StringBuilder();

        holidays.sort(LocalDate::compareTo);

        for (LocalDate holiday : holidays) {
            stringBuilder.append(holiday).append(";");
        }

        String encodedHolidays = stringBuilder.toString();

        setConfigItem(user, Configuration.HOLIDAY_LIST, encodedHolidays, String.class);
    }

    /**
     * Update the invitation link to the Slack page of the current semester/term
     *
     * @param user the user who updated the invitation link
     * @param slackLink the invitation link to the Slack page
     * @throws DataAccessException if an issue arises updating the invitation link
     */
    public static void updateSlackLink(User user, String slackLink) throws DataAccessException {
        setConfigItem(user, Configuration.SLACK_LINK, slackLink, String.class);
    }

    public static String getSlackLink() throws DataAccessException {
        return dao.getConfiguration(Configuration.SLACK_LINK, String.class);
    }

    //
    // GENERAL HELPER FUNCTIONS
    //
    private static String translateInstantForFrontEnd(Instant timestamp) {
        String translatedTime;
        if (timestamp.equals(Instant.MAX)) {
            translatedTime = "never";
        } else {
            translatedTime = timestamp.toString();
        }
        return translatedTime;
    }
    /**
     * Checks that the date for a scheduled shutdown has passed. If it has, it triggers the shutdown method
     */
    public static void checkForShutdown() {
        try {
            Instant shutdownInstant = dao.getConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_DATE, Instant.class);
            if (shutdownInstant.isBefore(Instant.now())) {
                triggerShutdown();
            }
        } catch (DataAccessException e) {
            LOGGER.error("Something went wrong while checking for shutdown: " + e.getMessage());
        }
    }
    private static Instant getInstantFromUnzonedTime(String timestampString) throws DateTimeParseException {
        ZoneId utahZone = ZoneId.of("America/Denver");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime localDateTime = LocalDateTime.parse(timestampString, formatter);
        ZonedDateTime utahTime = localDateTime.atZone(utahZone);

        return utahTime.toInstant();
    }
    /**
     * throws IllegalArgumentException if percent is not valid
     * @param percent a float that should be between 0 and 1 (inclusive)
     * @param name the name of the value, only used when throwing the exception
     */
    private static void validateValidPercentFloat(float percent, String name) {
        if ((percent < 0) || (percent > 1)) {
            throw new IllegalArgumentException(name + " must be 0-1");
        }
    }

    /**
     * throws IllegalArgumentException if value is negative
     * @param value number we're checking is >= 0
     * @param name name of the value, used when throwing the exception
     */
    private static void validateNonNegativeInt(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be non-negative");
        }
    }

    private static <T> void setConfigItem(User admin, Configuration configKey, T value, Class<T> type) throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        T current = dao.getConfiguration(configKey, type);
        if (current.equals(value)) return;

        dao.setConfiguration(configKey, value, type);
        logConfigChange("changed %s to [%s]".formatted(configKey.name(), value.toString()), admin.netId());
    }


    //
    // LOGGING
    //
    private static void logConfigChange(String changeMessage, String adminNetId) {
        LOGGER.info("[CONFIG] Admin {} has {}", adminNetId, changeMessage);
    }

    private static void logAutomaticConfigChange(String changeMessage) {
        LOGGER.info("[CONFIG] Automatic change: {}", changeMessage);
    }
}
