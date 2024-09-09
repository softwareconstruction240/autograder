package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import edu.byu.cs.util.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import static spark.Spark.halt;

public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    public static final Route repoPatch = (req, res) -> {
        User user = req.session().attribute("user");

        JsonObject jsonObject = new Gson().fromJson(req.body(), JsonObject.class);
        String repoUrl = new Gson().fromJson(jsonObject.get("repoUrl"), String.class);

        try {
            if (!isValidRepoUrl(repoUrl)) {
                halt(400, "Invalid Github Repo Url. Check if the link is valid");
            }
        } catch (Exception e) {
            LOGGER.error("Error cloning repo during repoPatch: " + e.getMessage());
            halt(500, "There was an internal server error in cloning the Github Repo");
        }

        DaoService.getUserDao().setRepoUrl(user.netId(), repoUrl);
        LOGGER.info("{} changed their repoUrl to {}", user.netId(), repoUrl);

        res.status(200);
        return "Successfully updated repoUrl";
    };

    private static boolean isValidRepoUrl(String url) throws IOException {
        File cloningDir = new File("./validation_cloning/" + UUID.randomUUID());
        CloneCommand cloneCommand = Git.cloneRepository().setURI(url).setDirectory(cloningDir);

        try (Git git = cloneCommand.call()) {
            LOGGER.info("Test cloning git repo to {}", git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            FileUtils.removeDirectory(cloningDir);
            return false;
        }
        FileUtils.removeDirectory(cloningDir);
        return true;
    }
}
