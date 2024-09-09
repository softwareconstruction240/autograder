package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import static spark.Spark.halt;

public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    public static final Route repoPatch = (req, res) -> {
        User user = req.session().attribute("user");

        String repoUrl = req.queryParams("repoUrl");

        if (!isValidRepoUrl(repoUrl)) {
            halt(400, "invalid github repo");
        }

        DaoService.getUserDao().setRepoUrl(user.netId(), repoUrl);
        LOGGER.info("{} changed their repoUrl to {}", user.netId(), repoUrl);

        res.status(200);
        return "Successfully updated repoUrl";
    };

    private static boolean isValidRepoUrl(String url) {
        CloneCommand cloneCommand = Git.cloneRepository().setURI(url);

        try (Git git = cloneCommand.call()) {
            LOGGER.info("Test cloning git repo to {}", git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            LOGGER.error("Failed to clone repo", e);
            return false;
        }
        return true;
    }
}
