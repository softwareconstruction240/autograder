package edu.byu.cs.service;

import com.google.gson.JsonObject;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegrationImpl;
import edu.byu.cs.canvas.model.CanvasAssignment;
import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.util.PhaseUtils;
import edu.byu.cs.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import static edu.byu.cs.util.PhaseUtils.isPhaseEnabled;
import static edu.byu.cs.util.PhaseUtils.isPhaseGraded;

public class ConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);
    private static final ConfigurationDao dao = DaoService.getConfigurationDao();


    private static void logConfigChange(String changeMessage, String adminNetId) {
        LOGGER.info("[CONFIG] Admin {} has {}", adminNetId, changeMessage);
    }

    private static void logAutomaticConfigChange(String changeMessage) {
        LOGGER.info("[CONFIG] Automatic change: {}", changeMessage);
    }

    /**
     * Edits in place the passed in response object
     *
     * @param response the Json Object to add banner info to
     * @throws DataAccessException if it screws up while getting into the database
     */
    private static void addBannerConfig(JsonObject response) throws DataAccessException {
        Instant bannerExpiration = dao.getConfiguration(ConfigurationDao.Configuration.BANNER_EXPIRATION, Instant.class);

        if (bannerExpiration.isBefore(Instant.now())) { //Banner has expired
            clearBannerConfig();
        }

        if (bannerExpiration.equals(Instant.MAX)) { //shutdown is not set
            response.addProperty("bannerExpiration", "never");
        } else {
            response.addProperty("bannerExpiration", bannerExpiration.toString());
        }

        response.addProperty("bannerMessage", dao.getConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, String.class));
        response.addProperty("bannerLink", dao.getConfiguration(ConfigurationDao.Configuration.BANNER_LINK, String.class));
        response.addProperty("bannerColor", dao.getConfiguration(ConfigurationDao.Configuration.BANNER_COLOR, String.class));
    }

    private static void clearBannerConfig() throws DataAccessException {
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, "", String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_LINK, "", String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_COLOR, "", String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_EXPIRATION, Instant.MAX, Instant.class);

        logAutomaticConfigChange("Banner message has expired");
    }

    public static JsonObject getPublicConfig() throws DataAccessException {
        checkForShutdown();

        JsonObject response = new JsonObject();

        addBannerConfig(response);
        response.addProperty("phases", dao.getConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, String.class));

        Instant shutdownTimestamp = dao.getConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_DATE, Instant.class);
        if (shutdownTimestamp.isBefore(Instant.now())) { //shutdown time has passed
            clearShutdownSchedule();
        }

        if (shutdownTimestamp.equals(Instant.MAX)) { //shutdown is not set
            response.addProperty("shutdownSchedule", "never");
        } else {
            response.addProperty("shutdownSchedule", shutdownTimestamp.toString());
        }

        response.addProperty("shutdownWarningMilliseconds", dao.getConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_WARNING_MILLISECONDS, Integer.class));

        return response;
    }

    public static JsonObject getPrivateConfig() throws DataAccessException {
        Map<Phase, Integer> assignmentIds = new EnumMap<>(Phase.class);
        Map<Phase, Map<Rubric.RubricType, CanvasAssignment.CanvasRubric>> rubricInfo = new EnumMap<>(Phase.class);

        for (Phase phase : Phase.values()) {
            if (!isPhaseGraded(phase)) continue;
            Integer assignmentId = PhaseUtils.getPhaseAssignmentNumber(phase);
            assignmentIds.put(phase, assignmentId);
            if (rubricInfo.get(phase) == null) {
                rubricInfo.put(phase, new EnumMap<>(Rubric.RubricType.class));
            }
            var rubricConfigItems = DaoService.getRubricConfigDao().getRubricConfig(phase).items();
            for (Rubric.RubricType type : rubricConfigItems.keySet()) {
                RubricConfig.RubricConfigItem item = rubricConfigItems.get(type);
                if (item == null) continue;
                rubricInfo.get(phase).put(
                        type,
                        new CanvasAssignment.CanvasRubric(item.rubric_id(), item.points(), null)
                );
            }
        }

        JsonObject response = getPublicConfig();
        int courseNumber = dao.getConfiguration(
                ConfigurationDao.Configuration.COURSE_NUMBER,
                Integer.class
        );
        response.addProperty("courseNumber", courseNumber);
        response.addProperty("assignmentIds", Serializer.serialize(assignmentIds));
        response.addProperty("rubricInfo", Serializer.serialize(rubricInfo));
        return response;
    }

    public static void updateLivePhases(ArrayList phasesArray, User user) throws DataAccessException {
        dao.setConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, phasesArray, ArrayList.class);

        logConfigChange("set the following phases as live: %s".formatted(phasesArray), user.netId());
    }

    public static void setShutdownWarningDuration(User user, Integer warningMilliseconds) throws DataAccessException {
        if (warningMilliseconds < 0) {
            throw new IllegalArgumentException("warningMilliseconds must be non-negative");
        }

        dao.setConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_WARNING_MILLISECONDS, warningMilliseconds, Integer.class);

        logConfigChange("set the shutdown warning duration to %s milliseconds".formatted(warningMilliseconds), user.netId());
    }

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

    public static void clearShutdownSchedule() throws DataAccessException {
        clearShutdownSchedule(null);
    }

    public static void clearShutdownSchedule(User user) throws DataAccessException {
        dao.setConfiguration(ConfigurationDao.Configuration.GRADER_SHUTDOWN_DATE, Instant.MAX, Instant.class);
        if (user == null) {
            logAutomaticConfigChange("Grader Shutdown Schedule was cleared");
        } else {
            logConfigChange("cleared the Grader Shutdown Schedule", user.netId() );
        }
    }

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

        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, message, String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_LINK, link, String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_COLOR, color, String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_EXPIRATION, expirationTimestamp, Instant.class);

        if (message.isEmpty()) {
            logConfigChange("cleared the banner message", user.netId());
        } else {
            expirationString = !expirationString.isEmpty() ? expirationString : "never";
            logConfigChange("set the banner message to: '%s' with link: {%s} to expire at %s".formatted(message, link, expirationString), user.netId());
        }

    }

    private static Instant getInstantFromUnzonedTime(String timestampString) throws DateTimeParseException {
        ZoneId utahZone = ZoneId.of("America/Denver");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime localDateTime = LocalDateTime.parse(timestampString, formatter);
        ZonedDateTime utahTime = localDateTime.atZone(utahZone);

        return utahTime.toInstant();
    }

    public static void updateCourseIds(User user, SetCourseIdsRequest setCourseIdsRequest) throws DataAccessException {

        // Course Number
        dao.setConfiguration(
                ConfigurationDao.Configuration.COURSE_NUMBER,
                setCourseIdsRequest.courseNumber(),
                Integer.class
        );

        // Assignment IDs and Rubric Info
        var assignmentIds = setCourseIdsRequest.assignmentIds();
        var rubricInfo = setCourseIdsRequest.rubricInfo();
        for (Phase phase : assignmentIds.keySet()) {
            Integer id = assignmentIds.get(phase);
            DaoService.getConfigurationDao().setConfiguration(
                    PhaseUtils.getConfigurationAssignmentNumber(phase),
                    id,
                    Integer.class
            );
            var rubricTypeMap = rubricInfo.get(phase);
            for (Rubric.RubricType type : rubricTypeMap.keySet()) {
                CanvasAssignment.CanvasRubric rubric = rubricTypeMap.get(type);
                DaoService.getRubricConfigDao().setRubricIdAndPoints(
                        phase,
                        type,
                        rubric.points(),
                        rubric.id()
                );
            }
        }

        logConfigChange(
                "updated course info (course number, assignment IDs, rubric, IDs, rubric points) " +
                        "in the database manually",
                user.netId()
        );
    }

    public static void updateCourseIdsUsingCanvas(User user) throws CanvasException, DataAccessException {
        var retriever = new CanvasIntegrationImpl.CourseInfoRetriever();
        retriever.useCourseRelatedInfoFromCanvas();

        logConfigChange(
                "updated course info (assignment IDs, rubric IDs, rubric points) " +
                        "in the database using Canvas",
                user.netId()
        );
    }
}
