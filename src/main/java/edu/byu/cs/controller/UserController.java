package edu.byu.cs.controller;

import java.util.Collection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.exception.InternalServerException;
import edu.byu.cs.controller.exception.WordOfWisdomViolationException;
import edu.byu.cs.model.RepoUpdate;
import edu.byu.cs.model.User;
import edu.byu.cs.service.UserService;
import edu.byu.cs.util.Serializer;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.ImATeapotResponse;
import io.javalin.http.InternalServerErrorResponse;

/**
 * Handles user/repo-related HTTP endpoints
 */
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public Handler setRepoUrl() {
        return ctx -> {
            User user = ctx.sessionAttribute("user");
            updateRepoUrl(user.netId(), null, ctx);
            ctx.result("Successfully updated repoUrl");
        };
    }

    public Handler setRepoUrlAdmin() {
        return ctx -> {
            User admin = ctx.sessionAttribute("user");
            String studentNetId = ctx.pathParam("netId");
            updateRepoUrl(studentNetId, admin.netId(), ctx);
            ctx.result("Successfully updated repoUrl for user: " + studentNetId);
        };
    }

    public Handler repoHistoryAdminGet() {
        return ctx -> {
            String repoUrl = ctx.queryParam("repoUrl");
            String netId = ctx.queryParam("netId");

            Collection<RepoUpdate> updates;
            try {
                updates = userService.adminGetRepoHistory(repoUrl, netId);
            } catch (InternalServerException e) {
                throw new InternalServerErrorResponse("There was an internal server error getting repo updates");
            }
            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(Serializer.serialize(updates));
        };
    }

    private void updateRepoUrl(String studentNetId, String adminNetId, Context ctx) {
        JsonObject jsonObject = new Gson().fromJson(ctx.body(), JsonObject.class);
        String repoUrl = new Gson().fromJson(jsonObject.get("repoUrl"), String.class);

        try {
            userService.updateRepoUrl(studentNetId, repoUrl, adminNetId);
        } catch (BadRequestException e) {
            throw new BadRequestResponse(e.getMessage());
        }  catch (InternalServerException e) {
            throw new InternalServerErrorResponse(e.getMessage());
        } catch (WordOfWisdomViolationException e) {
            throw new ImATeapotResponse(e.getMessage());
        }

        ctx.status(200);
    }
}
