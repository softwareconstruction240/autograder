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
import java.util.Objects;

public class ConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigService.class);

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
        ConfigurationDao dao = DaoService.getConfigurationDao();
        Instant bannerExpiration = dao.getConfiguration(ConfigurationDao.Configuration.BANNER_EXPIRATION, Instant.class);

        if (bannerExpiration.isBefore(Instant.now())) { //Banner has expired
            clearBannerConfig();
        }

        response.addProperty("bannerMessage", dao.getConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, String.class));
        response.addProperty("bannerLink", dao.getConfiguration(ConfigurationDao.Configuration.BANNER_LINK, String.class));
        response.addProperty("bannerColor", dao.getConfiguration(ConfigurationDao.Configuration.BANNER_COLOR, String.class));
        response.addProperty("bannerExpiration", bannerExpiration.toString());
    }

    private static void addPenaltyConfig(JsonObject response) throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        response.addProperty("perDayLatePenalty", dao.getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class));
        response.addProperty("gitCommitPenalty", dao.getConfiguration(ConfigurationDao.Configuration.GIT_COMMIT_PENALTY, Float.class));
        response.addProperty("maxLateDaysPenalized", dao.getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class));
    }

    private static void clearBannerConfig() throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, "", String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_LINK, "", String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_COLOR, "", String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_EXPIRATION, Instant.MAX, Instant.class);

        logAutomaticConfigChange("Banner message has expired");
    }

    public static JsonObject getPublicConfig() throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        JsonObject response = new JsonObject();

        addBannerConfig(response);
        response.addProperty("phases", dao.getConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, String.class));

        return response;
    }

    public static JsonObject getPrivateConfig() throws DataAccessException {
        Map<Phase, Integer> assignmentIds = new EnumMap<>(Phase.class);
        Map<Phase, Map<Rubric.RubricType, CanvasAssignment.CanvasRubric>> rubricInfo = new EnumMap<>(Phase.class);

        for (Phase phase : Phase.values()) {
            if (!PhaseUtils.isPhaseGraded(phase)) continue;
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
        addPenaltyConfig(response);

        int courseNumber = DaoService.getConfigurationDao().getConfiguration(
                ConfigurationDao.Configuration.COURSE_NUMBER,
                Integer.class
        );
        response.addProperty("courseNumber", courseNumber);
        response.addProperty("assignmentIds", Serializer.serialize(assignmentIds));
        response.addProperty("rubricInfo", Serializer.serialize(rubricInfo));
        return response;
    }

    public static void updateLivePhases(ArrayList phasesArray, User user) throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        dao.setConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, phasesArray, ArrayList.class);

        logConfigChange("set the following phases as live: %s".formatted(phasesArray), user.netId());
    }

    public static void updateBannerMessage(User user, String expirationString, String message, String link, String color) throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

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
        DaoService.getConfigurationDao().setConfiguration(
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

    public static void setMaxLateDays(User user, Integer maxDays) throws DataAccessException {
        if (maxDays < 0) {
            throw new IllegalArgumentException("Max Late Days must be non-negative");
        }

        ConfigurationDao dao = DaoService.getConfigurationDao();

        Integer current = dao.getConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, Integer.class);
        if (current.equals(maxDays)) { return; }

        dao.setConfiguration(ConfigurationDao.Configuration.MAX_LATE_DAYS_TO_PENALIZE, maxDays, Integer.class);
        logConfigChange("set maximum late days penalized to %s".formatted(maxDays), user.netId());
    }

    /**
     *
     * @param user the user making the change
     * @param perDayPenalty the penalty per day that should be applied. For example, a 10% penalty per day should be
     *                      passed in as 0.1
     */
    public static void setPerDayLatePenalty(User user, Float perDayPenalty) throws DataAccessException {
        if ((perDayPenalty < 0) || (perDayPenalty > 1)) {
            throw new IllegalArgumentException("Per Day Late Penalty must be 0-1");
        }

        ConfigurationDao dao = DaoService.getConfigurationDao();

        Float current = dao.getConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, Float.class);
        if (current.equals(perDayPenalty)) { return; }

        dao.setConfiguration(ConfigurationDao.Configuration.PER_DAY_LATE_PENALTY, perDayPenalty, Float.class);
        logConfigChange("set the per day late penalty to %s".formatted(perDayPenalty), user.netId());
    }

    /**
     *
     * @param user the user making the change
     * @param gitCommitPenalty the penalty should be applied for not having enough commits. For example, a 10%
     *                         penalty per day should be passed in as 0.1
     */
    public static void setGitCommitPenalty(User user, Float gitCommitPenalty) throws DataAccessException {
        if ((gitCommitPenalty < 0) || (gitCommitPenalty > 1)) {
            throw new IllegalArgumentException("Git Commit Penalty must be 0-1");
        }

        ConfigurationDao dao = DaoService.getConfigurationDao();

        Float current = dao.getConfiguration(ConfigurationDao.Configuration.GIT_COMMIT_PENALTY, Float.class);
        if (current.equals(gitCommitPenalty)) { return; }

        dao.setConfiguration(ConfigurationDao.Configuration.GIT_COMMIT_PENALTY, gitCommitPenalty, Float.class);
        logConfigChange("set the git commit penalty to %s".formatted(gitCommitPenalty), user.netId());
    }
}
