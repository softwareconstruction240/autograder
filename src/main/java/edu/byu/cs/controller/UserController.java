package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.User;
import edu.byu.cs.util.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.io.File;
import java.util.UUID;

import static spark.Spark.halt;

public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    public static final Route repoPatch = (req, res) -> {
        User user = req.session().attribute("user");

        JsonObject jsonObject = new Gson().fromJson(req.body(), JsonObject.class);
        String repoUrl = cleanRepoUrl(new Gson().fromJson(jsonObject.get("repoUrl"), String.class));

        try {
            if (!isValidRepoUrl(repoUrl)) {
                res.status(400);
                return "Invalid Github Repo Url. Check if the link is valid and points directly to a Github Repo.";
            }
        } catch (Exception e) {
            LOGGER.error("Error cloning repo during repoPatch: " + e.getMessage());
            halt(500, "There was an internal server error in cloning the Github Repo");
        }

        DaoService.getUserDao().setRepoUrl(user.netId(), repoUrl);
        LOGGER.info("user {} changed their repoUrl to {}", user.netId(), repoUrl);

        res.status(200);
        return "Successfully updated repoUrl";
    };

    private static boolean isValidRepoUrl(String url) {
        File cloningDir = new File("./validation_cloning/" + UUID.randomUUID());
        CloneCommand cloneCommand = Git.cloneRepository().setURI(url).setDirectory(cloningDir);

        try (Git git = cloneCommand.call()) {
            LOGGER.debug("Cloning repo to {} to check repo exists", git.getRepository().getDirectory());
        } catch (GitAPIException e) {
            FileUtils.removeDirectory(cloningDir);
            return false;
        }
        FileUtils.removeDirectory(cloningDir);
        return true;
    }

    /**
     * cleans up and returns the provided GitHub Repo URL for consistent formatting.
     * currently just removes the .git at the end of the URL if present
     */
    private static String cleanRepoUrl(String url) {
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        return url;
    }
}
