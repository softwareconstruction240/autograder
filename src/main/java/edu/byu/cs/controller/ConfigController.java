package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.controller.httpexception.BadRequestException;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.service.ConfigService;
import io.javalin.http.Handler;

import java.util.ArrayList;

public class ConfigController {

    public static final Handler getConfigAdmin = ctx -> {
        try {
            JsonObject response = ConfigService.getPrivateConfig();
            ctx.status(200);

            // TODO unserialize or something...?
            // Original was simply `return response;`
            ctx.json(response);

        } catch (DataAccessException e) {
            ctx.status(500);
            ctx.result(e.getMessage());
        }
    };

    public static final Handler getConfigStudent = ctx -> {
        String response = ConfigService.getPublicConfig().toString();

        ctx.status(200);
        ctx.result(response);
    };

    public static final Handler updateLivePhases = ctx -> {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(ctx.body(), JsonObject.class);
        ArrayList phasesArray = gson.fromJson(jsonObject.get("phases"), ArrayList.class);
        User user = ctx.sessionAttribute("user");

        ConfigService.updateLivePhases(phasesArray, user);

        ctx.status(200);
    };

    public static final Handler updateBannerMessage = ctx -> {
        User user = ctx.sessionAttribute("user");

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(ctx.body(), JsonObject.class);
        String expirationString = gson.fromJson(jsonObject.get("bannerExpiration"), String.class);

        String message = gson.fromJson(jsonObject.get("bannerMessage"), String.class);
        String link = gson.fromJson(jsonObject.get("bannerLink"), String.class);
        String color = gson.fromJson(jsonObject.get("bannerColor"), String.class);

        try {
            ConfigService.updateBannerMessage(user, expirationString, message, link, color);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }

        ctx.status(200);
    };

    public static final Handler updateCourseIdsPost = ctx -> {
        SetCourseIdsRequest setCourseIdsRequest = new Gson().fromJson(ctx.body(), SetCourseIdsRequest.class);

        User user = ctx.sessionAttribute("user");

        // Course Number
        try {
            ConfigService.updateCourseIds(user, setCourseIdsRequest);
        } catch (DataAccessException e) {
            ctx.status(400);
            ctx.result(e.getMessage());
            return;
        }

        ctx.status(200);
    };

    public static final Handler updateCourseIdsUsingCanvasGet = ctx -> {
        User user = ctx.sessionAttribute("user");
        ConfigService.updateCourseIdsUsingCanvas(user);
        ctx.status(200);
    };
}
