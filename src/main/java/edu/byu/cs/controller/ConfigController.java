package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.User;
import edu.byu.cs.model.appConfig.BannerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

public class ConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmissionController.class);

    public static final Route getConfig = (req, res) -> {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        JsonObject response = new JsonObject();

        response.addProperty("bannerMessage", dao.getConfiguration(ConfigurationDao.Configuration.BANNER_MESSAGE, String.class));

        res.status(200);
        return response.toString();
    };

    public static final Route updateLivePhases = (req, res) -> {

        return null;
    };

    public static final Route updateBannerMessage = (req, res) -> {
        ConfigurationDao dao = DaoService.getConfigurationDao();

        String message = new Gson().fromJson(req.body(), BannerMessage.class).bannerMessage().strip();
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
