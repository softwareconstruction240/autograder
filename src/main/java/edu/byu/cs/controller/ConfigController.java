package edu.byu.cs.controller;

import com.google.gson.JsonObject;
import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.exception.UnauthorizedException;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.model.request.ConfigHolidayUpdateRequest;
import edu.byu.cs.model.request.ConfigPenaltyUpdateRequest;
import edu.byu.cs.service.ConfigService;
import edu.byu.cs.util.Serializer;
import io.javalin.http.Handler;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles config-specific HTTP endpoints
 */
public class ConfigController {
    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    public Handler getConfigAdmin() {
        return ctx -> {
            try {
                PrivateConfig config = configService.getPrivateConfig();
                ctx.status(200);
                ctx.result(Serializer.serialize(config));
            } catch (DataAccessException e) {
                ctx.status(500);
                ctx.result(e.getMessage());
            }
        };
    }

    public Handler getConfigStudent() {
        return ctx -> {
            PublicConfig config = configService.getPublicConfig();
            ctx.status(200);
            ctx.result(Serializer.serialize(config));
        };
    }

    public Handler updateLivePhases() {
        return ctx -> {
            JsonObject jsonObject = Serializer.deserialize(ctx.body(), JsonObject.class);
            ArrayList phasesArray = Serializer.deserialize(jsonObject.get("phases"), ArrayList.class);

            User user = ctx.sessionAttribute("user");
            if (user == null) {
                throw new UnauthorizedException("No user credentials found");
            }

            configService.updateLivePhases(phasesArray, user);
        };
    }

    public Handler scheduleShutdown() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");

            JsonObject jsonObject = ctx.bodyAsClass(JsonObject.class);
            String shutdownTimestampString = Serializer.deserialize(jsonObject.get("shutdownTimestamp"), String.class);
            Integer shutdownWarningMilliseconds = Serializer.deserialize(jsonObject.get("shutdownWarningMilliseconds"), Integer.class);

            try {
                configService.scheduleShutdown(user, shutdownTimestampString);
                configService.setShutdownWarningDuration(user, shutdownWarningMilliseconds);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(e.getMessage());
            }
        };
    }

    public Handler updateBannerMessage() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");

            JsonObject jsonObject = Serializer.deserialize(ctx.body(), JsonObject.class);
            String expirationString = Serializer.deserialize(jsonObject.get("bannerExpiration"), String.class);

            String message = Serializer.deserialize(jsonObject.get("bannerMessage"), String.class);
            String link = Serializer.deserialize(jsonObject.get("bannerLink"), String.class);
            String color = Serializer.deserialize(jsonObject.get("bannerColor"), String.class);

            try {
                configService.updateBannerMessage(user, expirationString, message, link, color);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(e.getMessage(), e);
            }
        };
    }

    public Handler updateCourseIdPost() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");

            JsonObject jsonObject = Serializer.deserialize(ctx.body(), JsonObject.class);
            Integer courseId = Serializer.deserialize(jsonObject.get("courseId"), Integer.class);

            try {
                configService.setCourseId(user, courseId);
            } catch (DataAccessException e) {
                ctx.status(500);
                ctx.result(e.getMessage());
            } catch (CanvasException e) {
                ctx.status(400);
                ctx.result("Canvas Error:\nEither the Canvas Course #%d doesn't exist, the Autograder doesn't have access to the course, or Canvas was unable to be reached.".formatted(courseId));
            }
            ctx.result("");
        };
    }

    public Handler updateCourseIdsUsingCanvasGet() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");
            if (user == null) {
                throw new UnauthorizedException("No user credentials found");
            }
            configService.updateCourseIdsUsingCanvas(user);
        };
    }

    public Handler updatePenalties() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");
            ConfigPenaltyUpdateRequest request = ctx.bodyAsClass(ConfigPenaltyUpdateRequest.class);

            configService.processPenaltyUpdates(user, request);
        };
    }

    public Handler updateHolidays() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");

            ConfigHolidayUpdateRequest request = Serializer.deserialize(ctx.body(), ConfigHolidayUpdateRequest.class);

            List<LocalDate> holidays = new ArrayList<>();
            try {
                for (String date : request.holidays()) {
                    holidays.add(LocalDate.parse(date));
                }

                configService.updateHolidays(user, holidays);
            } catch (DataAccessException e) {
                ctx.status(500);
                ctx.result(e.getMessage());
            } catch (DateTimeParseException e) {
                ctx.status(400);
                ctx.result("Invalid date format provided.");
            }

            ctx.result("");
        };
    }

    public Handler updateSlackLink() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");

            JsonObject jsonObject = Serializer.deserialize(ctx.body(), JsonObject.class);
            String request = Serializer.deserialize(jsonObject.get("slackLink"), String.class);

            configService.updateSlackLink(user, request);
        };
    }
}
