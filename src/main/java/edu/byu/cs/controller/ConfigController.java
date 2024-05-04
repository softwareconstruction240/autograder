package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.util.ArrayList;

public class ConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);

    public static final Route getConfigAdmin = (req, res) -> {
        JsonObject response = getPublicConfig();

        res.status(200);
        return response.toString();
    };

    public static final Route getConfigStudent = (req, res) -> {
        String response = getPublicConfig().toString();

        res.status(200);
        return response;
    };

    private static JsonObject getPublicConfig() throws DataAccessException {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        JsonObject response = new JsonObject();

        response.addProperty("bannerMessage", dao.getConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, String.class));
        response.addProperty("phases", dao.getConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, String.class));

        return response;
    }

    public static final Route updateLivePhases = (req, res) -> {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        JsonObject jsonObject = new Gson().fromJson(req.body(), JsonObject.class);
        ArrayList phasesArray = new Gson().fromJson(jsonObject.get("phases"), ArrayList.class);

        dao.setConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, phasesArray, ArrayList.class);

        User user = req.session().attribute("user");
        LOGGER.info("[CONFIG] Admin %s has set the following phases as live: %s".formatted(user.netId(), phasesArray));

        res.status(200);
        return "";
    };

    public static final Route updateBannerMessage = (req, res) -> {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        JsonObject jsonObject = new Gson().fromJson(req.body(), JsonObject.class);
        String message = new Gson().fromJson(jsonObject.get("bannerMessage"), String.class);
        dao.setConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, message, String.class);

        User user = req.session().attribute("user");
        if (message.isEmpty()) {
            LOGGER.info("[CONFIG] Admin %s has cleared the banner message".formatted(user.netId()));
        } else {
            LOGGER.info("[CONFIG] Admin %s has set the banner message to: '%s'".formatted(user.netId(), message));
        }

        res.status(200);
        return "";
    };
}
