package edu.byu.cs.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import edu.byu.cs.autograder.Grader;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.dataAccess.sql.RepoUpdateDao;
import edu.byu.cs.model.RepoUpdate;
import edu.byu.cs.model.User;
import edu.byu.cs.util.FileUtils;
import edu.byu.cs.util.Serializer;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.HaltException;
import spark.Route;

import java.io.File;
import java.time.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import static spark.Spark.halt;

public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    public static final Route repoPatch = (req, res) -> {
        User user = req.session().attribute("user");

        JsonObject jsonObject = new Gson().fromJson(req.body(), JsonObject.class);
        String repoUrl = cleanRepoUrl(new Gson().fromJson(jsonObject.get("repoUrl"), String.class));

        setRepoUrl(user.netId(), repoUrl);

        res.status(200);
        return "Successfully updated repoUrl";
    };

    public static final Route repoPatchAdmin = (req, res) -> {
        User admin = req.session().attribute("user");
        String studentNetId = req.params(":netId");

        JsonObject jsonObject = new Gson().fromJson(req.body(), JsonObject.class);
        String repoUrl = cleanRepoUrl(new Gson().fromJson(jsonObject.get("repoUrl"), String.class));

        setRepoUrl(studentNetId, repoUrl, admin.netId());

        res.status(200);
        return "Successfully updated repoUrl for user: " + studentNetId;
    };

    public static final Route repoHistoryAdminGet = (req, res) -> {
        String repoUrl = req.queryParams("repoUrl");
        String netId = req.queryParams("netId");

        Collection<RepoUpdate> updates = new ArrayList<>();
        try {
            if (repoUrl == null && netId == null) {
                halt(422, "You must provide either a repoUrl or a netId");
            }
            if (repoUrl != null) {
                updates.addAll(DaoService.getRepoUpdateDao().getUpdatesForRepo(repoUrl));
            }
            if (netId != null) {
                updates.addAll(DaoService.getRepoUpdateDao().getUpdatesForUser(netId));
            }
        } catch (Exception e) {
            LOGGER.error("Error getting repo updates:", e);
            halt(500, "There was an internal server error getting repo updates");
        }

        res.status(200);
        res.type("application/json");

        return Serializer.serialize(updates);
    };

    private static void setRepoUrl(String studentNetId, String repoUrl, String adminNetId) {
        try {
            if (!isValidRepoUrl(repoUrl)) {
                halt(400, "Invalid Github Repo Url. Check if the link is valid and points directly to a Github Repo.");
            }
        } catch (HaltException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error cloning repo during repoPatch: " + e.getMessage());
            halt(500, "There was an internal server error in verifying the Github Repo");
        }

        RepoUpdate historicalUpdate = null;
        try {
            historicalUpdate = verifyRepoIsAvailableForUser(repoUrl, studentNetId);
        } catch (DataAccessException e) {
            halt(500, "There was an internal server error in checking the repo update history for this url");
        }
        if (historicalUpdate != null) {
            if (adminNetId != null) {
                halt(418, "Repo is blocked because of a prior claim: " + historicalUpdate);
            } else {
                LOGGER.info("Student " + studentNetId + " was blocked from updating their url because of a prior claim: " + historicalUpdate);
                halt(418, "Please talk to a TA to submit this url");
            }
        }

        try {
            RepoUpdate update = new RepoUpdate(Instant.now(), studentNetId, repoUrl, adminNetId != null, adminNetId);
            DaoService.getUserDao().setRepoUrl(studentNetId, repoUrl);
            DaoService.getRepoUpdateDao().insertUpdate(update);
        } catch (DataAccessException e) {
            halt(500, "There was an internal server error in saving the GitHub Repo URL");
        }

        if (adminNetId == null) {
            LOGGER.info("student {} changed their repoUrl to {}", studentNetId, repoUrl);
        } else {
            LOGGER.info("admin {} changed the repoUrl for student {} to {}", adminNetId, studentNetId, repoUrl);
        }
    }

    private static void setRepoUrl(String student, String repoUrl) {
        setRepoUrl(student, repoUrl, null);
    }

    private static boolean isValidRepoUrl(String url) {
        File cloningDir = new File("./tmp" + UUID.randomUUID());
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
     * Checks to see if anyone currently or previously (other than the provided user) has claimed the provided repoUrl.
     * returns if the repo is available. it will throw otherwise, containing a message why
     * @param url the repoUrl to check if currently or previously claimed
     * @param netId the user trying to claim the url, so that they can claim urls they previously claimed
     * @return null if the repo is available for that user. returns the update that prevents the user from claiming the url.
     */
    private static RepoUpdate verifyRepoIsAvailableForUser(String url, String netId) throws DataAccessException {
        if (netId.equals("test")) {
            loadCurrentReposIntoUpdateTable();
        }

        Collection<RepoUpdate> updates = DaoService.getRepoUpdateDao().getUpdatesForRepo(url);
        if (updates.isEmpty()) {
            return null;
        }
        for (RepoUpdate update : updates) {
            if (!Objects.equals(update.netId(), netId)) {
                return update;
            }
        }
        return null;
    }

    // TODO: Remove this code soon or figure out a better way to do this
    // This loads all the repo urls in the user table into the update table so they can be compared
    // This will not be needed in future semesters, since updates will be logged from the start
    private static void loadCurrentReposIntoUpdateTable() {
        RepoUpdateDao updateDao = DaoService.getRepoUpdateDao();
        UserDao userDao = DaoService.getUserDao();

        try {
            Collection<User> users = userDao.getUsers();
            int minuteOffset = 0;
            int hourOffset = 0;
            for (User user : users) {
                if (user.role() == User.Role.ADMIN) { continue; }
                minuteOffset = (minuteOffset == 59) ? 0 : minuteOffset + 1;
                hourOffset = (minuteOffset == 0) ? hourOffset + 1 : hourOffset;
                LocalDateTime fakeUpdateTime = LocalDateTime.of(2024, 9, 4, hourOffset, minuteOffset);
                ZonedDateTime mountainTime = fakeUpdateTime.atZone(ZoneId.of("America/Denver"));
                Instant fakeUpdateInstant = mountainTime.toInstant();

                if (updateDao.getUpdatesForUser(user.netId()).isEmpty()) {
                    updateDao.insertUpdate(new RepoUpdate(fakeUpdateInstant, user.netId(), cleanRepoUrl(user.repoUrl()), true, "Canvas"));
                }
            }
        } catch (DataAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Cleans up and returns the provided GitHub Repo URL for consistent formatting.
     */
    private static String cleanRepoUrl(String url) {
        try {
            return Grader.cleanRepoUrl(url);
        } catch (GradingException e) {
            halt(400, "Invalid GitHub Repo URL: " + url);
        }
        return null;
    }
}
