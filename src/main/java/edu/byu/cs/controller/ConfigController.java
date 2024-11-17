package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.service.ConfigService;
import edu.byu.cs.util.Serializer;
import spark.Route;

import java.util.ArrayList;

import static spark.Spark.halt;

public class ConfigController {

    public static final Route getConfigAdmin = (req, res) -> {
        try {
            JsonObject response = ConfigService.getPrivateConfig();
            res.status(200);
            return response;
        } catch (DataAccessException e) {
            res.status(500);
            res.body(e.getMessage());
            return res;
        }
    };

    public static final Route getConfigStudent = (req, res) -> {
        String response = ConfigService.getPublicConfig().toString();

        res.status(200);
        return response;
    };

    public static final Route updateLivePhases = (req, res) -> {
        JsonObject jsonObject = Serializer.deserialize(req.body(), JsonObject.class);
        ArrayList phasesArray = Serializer.deserialize(jsonObject.get("phases"), ArrayList.class);
        User user = req.session().attribute("user");

        ConfigService.updateLivePhases(phasesArray, user);

        res.status(200);
        return "";
    };

    public static final Route scheduleShutdown = (req, res) -> {
        User user = req.session().attribute("user");

        JsonObject jsonObject = Serializer.deserialize(req.body(), JsonObject.class);
        String shutdownTimestampString = Serializer.deserialize(jsonObject.get("shutdownTimestamp"), String.class);
        Integer shutdownWarningMilliseconds = Serializer.deserialize(jsonObject.get("shutdownWarningMilliseconds"), Integer.class);

        try {
            ConfigService.scheduleShutdown(user, shutdownTimestampString);
            ConfigService.setShutdownWarningDuration(user, shutdownWarningMilliseconds);
        } catch (DataAccessException e) {
            halt(500, e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            halt(400, e.getMessage());
            return null;
        }

        res.status(200);
        return "";
    };

    public static final Route updateBannerMessage = (req, res) -> {
        User user = req.session().attribute("user");

        JsonObject jsonObject = Serializer.deserialize(req.body(), JsonObject.class);
        String expirationString = Serializer.deserialize(jsonObject.get("bannerExpiration"), String.class);

        String message = Serializer.deserialize(jsonObject.get("bannerMessage"), String.class);
        String link = Serializer.deserialize(jsonObject.get("bannerLink"), String.class);
        String color = Serializer.deserialize(jsonObject.get("bannerColor"), String.class);

        try {
            ConfigService.updateBannerMessage(user, expirationString, message, link, color);
        } catch (IllegalArgumentException e) {
            halt(400, e.getMessage());
            return null;
        }

        res.status(200);
        return "";
    };

    public static final Route updateCourseIdsPost = (req, res) -> {
        SetCourseIdsRequest setCourseIdsRequest = Serializer.deserialize(req.body(), SetCourseIdsRequest.class);

        User user = req.session().attribute("user");

        // Course Number
        try {
            ConfigService.updateCourseIds(user, setCourseIdsRequest);
        } catch (DataAccessException e) {
            res.status(400);
            res.body(e.getMessage());
            return res;
        }

        res.status(200);
        return "";
    };

    public static final Route updateCourseIdsUsingCanvasGet = (req, res) -> {
        User user = req.session().attribute("user");
        ConfigService.updateCourseIdsUsingCanvas(user);
        res.status(200);
        return "";
    };
}
