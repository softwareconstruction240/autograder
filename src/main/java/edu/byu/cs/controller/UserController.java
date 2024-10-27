package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.controller.httpexception.BadRequestException;
import edu.byu.cs.controller.httpexception.InternalServerException;
import edu.byu.cs.controller.httpexception.WordOfWisdomViolationException;
import edu.byu.cs.model.RepoUpdate;
import edu.byu.cs.model.User;
import edu.byu.cs.service.UserService;
import edu.byu.cs.util.Serializer;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Collection;

public class UserController {
    public static final Route repoPatch = (req, res) -> {
        User user = req.session().attribute("user");
        applyRepoPatch(user.netId(), null, req, res);
        return "Successfully updated repoUrl";
    };

    public static final Route repoPatchAdmin = (req, res) -> {
        User admin = req.session().attribute("user");
        String studentNetId = req.params(":netId");
        applyRepoPatch(studentNetId, admin.netId(), req, res);
        return "Successfully updated repoUrl for user: " + studentNetId;
    };

    public static final Route repoHistoryAdminGet = (req, res) -> {
        String repoUrl = req.queryParams("repoUrl");
        String netId = req.queryParams("netId");

        Collection<RepoUpdate> updates = UserService.adminGetRepoHistory(repoUrl, netId);

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(updates);
    };

    private static void applyRepoPatch(String studentNetId, String adminNetId, Request req, Response res)
            throws WordOfWisdomViolationException, InternalServerException, BadRequestException {
        JsonObject jsonObject = new Gson().fromJson(req.body(), JsonObject.class);
        String repoUrl = new Gson().fromJson(jsonObject.get("repoUrl"), String.class);
        UserService.updateRepoUrl(studentNetId, repoUrl, adminNetId);
        res.status(200);
    }
}
