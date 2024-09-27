package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import spark.Route;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

import static spark.Spark.halt;

public class ConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);

    private static void logConfigChange(String changeMessage, String adminNetId) {
        LOGGER.info("[CONFIG] Admin %s has %s".formatted(adminNetId, changeMessage));
    }

    public static final Route getConfigAdmin = (req, res) -> {
        try {
            JsonObject response = getPrivateConfig();
            res.status(200);
            return response;
        } catch (DataAccessException e) {
            res.status(500);
            res.body(e.getMessage());
            return res;
        }
    };

    public static final Route getConfigStudent = (req, res) -> {
        String response = getPublicConfig().toString();

        res.status(200);
        return response;
    };

    /**
     * Edits in place the passed in response object
     *
     * @param response the Json Object to add banner info to
     * @throws DataAccessException
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

    private static void clearBannerConfig() throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, "", String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_LINK, "", String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_COLOR, "", String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_EXPIRATION, Instant.MAX, Instant.class);
    }

    private static JsonObject getPublicConfig() throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        JsonObject response = new JsonObject();

        addBannerConfig(response);
        response.addProperty("phases", dao.getConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, String.class));

        return response;
    }

    private static JsonObject getPrivateConfig() throws DataAccessException {
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
        int courseNumber = DaoService.getConfigurationDao().getConfiguration(
                ConfigurationDao.Configuration.COURSE_NUMBER,
                Integer.class
        );
        response.addProperty("courseNumber", courseNumber);
        response.addProperty("assignmentIds", Serializer.serialize(assignmentIds));
        response.addProperty("rubricInfo", Serializer.serialize(rubricInfo));
        return response;
    }

    public static final Route updateLivePhases = (req, res) -> {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        JsonObject jsonObject = new Gson().fromJson(req.body(), JsonObject.class);
        ArrayList phasesArray = new Gson().fromJson(jsonObject.get("phases"), ArrayList.class);

        dao.setConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, phasesArray, ArrayList.class);

        User user = req.session().attribute("user");
        logConfigChange("set the following phases as live: %s".formatted(phasesArray), user.netId());

        res.status(200);
        return "";
    };

    public static final Route updateBannerMessage = (req, res) -> {
        ConfigurationDao dao = DaoService.getConfigurationDao();
        Gson gson = new Gson();

        JsonObject jsonObject = gson.fromJson(req.body(), JsonObject.class);
        String expirationString = gson.fromJson(jsonObject.get("bannerExpiration"), String.class);

        Instant expirationTimestamp = Instant.MAX;
        if (!expirationString.isEmpty()) {
            try {
                expirationTimestamp = getInstantFromUnzonedTime(expirationString);
            } catch (Exception e) {
                halt(400,"Incomplete timestamp. Send a full timestamp {YYYY-MM-DDTHH:MM:SS} or send none at all");
            }
        }

        if (expirationTimestamp.isBefore(Instant.now())) {
            halt(400, "You tried to set the banner to expire in the past");
        }

        String message = gson.fromJson(jsonObject.get("bannerMessage"), String.class);
        String link = gson.fromJson(jsonObject.get("bannerLink"), String.class);
        String color = gson.fromJson(jsonObject.get("bannerColor"), String.class);

        // If they give us a color, and it's not long enough or is missing #
        if (!color.isEmpty() && ((color.length() != 7) || !color.startsWith("#"))) {
            halt(400, "Invalid hex color code. Must provide a hex code starting with a # symbol, followed by 6 hex digits");
        }

        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, message, String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_LINK, link, String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_COLOR, color, String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_EXPIRATION, expirationTimestamp, Instant.class);

        User user = req.session().attribute("user");
        if (message.isEmpty()) {
            logConfigChange("cleared the banner message", user.netId());
        } else {
            logConfigChange("set the banner message to: '%s' with link: {%s}".formatted(message, link), user.netId());
        }

        res.status(200);
        return "";
    };

    private static Instant getInstantFromUnzonedTime(String timestampString) throws DateTimeParseException {
        ZoneId utahZone = ZoneId.of("America/Denver");

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime localDateTime = LocalDateTime.parse(timestampString, formatter);
        ZonedDateTime utahTime = localDateTime.atZone(utahZone);

        return utahTime.toInstant();
    }

    public static final Route updateCourseIdsPost = (req, res) -> {
        SetCourseIdsRequest setCourseIdsRequest = new Gson().fromJson(req.body(), SetCourseIdsRequest.class);

        // Course Number
        try {
            DaoService.getConfigurationDao().setConfiguration(
                    ConfigurationDao.Configuration.COURSE_NUMBER,
                    setCourseIdsRequest.courseNumber(),
                    Integer.class
            );
        } catch (DataAccessException e) {
            res.status(400);
            res.body(e.getMessage());
            return res;
        }

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

        User user = req.session().attribute("user");
        logConfigChange(
                "updated course info (course number, assignment IDs, rubric, IDs, rubric points) " +
                        "in the database manually",
            user.netId()
        );
        res.status(200);
        return "";
    };

    public static final Route updateCourseIdsUsingCanvasGet = (req, res) -> {
        var retriever = new CanvasIntegrationImpl.CourseInfoRetriever();
        retriever.useCourseRelatedInfoFromCanvas();

        User user = req.session().attribute("user");
        logConfigChange(
                "updated course info (assignment IDs, rubric IDs, rubric points) " +
                        "in the database using Canvas",
                user.netId()
        );
        res.status(200);
        return "";
    };
}
