package edu.byu.cs.controller;

import com.google.gson.Gson;
import edu.byu.cs.analytics.CommitAnalyticsRouter;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.honorChecker.HonorCheckerCompiler;
import edu.byu.cs.model.User;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Collection;

import static edu.byu.cs.util.JwtUtils.generateToken;
import static spark.Spark.halt;

public class AdminController {
    public static final Route usersGet = (req, res) -> {
        UserDao userDao = DaoService.getUserDao();

        Collection<User> users = userDao.getUsers();

        res.type("application/json");
        res.status(200);

        return new Gson().toJson(users);

    };

    public static final Route userPatch = (req, res) -> {
        String netId = req.params(":netId");

        UserDao userDao = DaoService.getUserDao();
        User user = userDao.getUser(netId);
        if (user == null) {
            halt(404, "user not found");
            return null;
        }

        String firstName = req.queryParams("firstName");
        if (firstName != null)
            userDao.setFirstName(user.netId(), firstName);

        String lastName = req.queryParams("lastName");
        if (lastName != null)
            userDao.setLastName(user.netId(), lastName);

        String repoUrl = req.queryParams("repoUrl");
        if (repoUrl != null)
            userDao.setRepoUrl(user.netId(), repoUrl);

        String role = req.queryParams("role");
        if (role != null) {
            try {
                userDao.setRole(user.netId(), User.Role.valueOf(role.toUpperCase()));
            } catch (IllegalArgumentException e) {
                halt(400, "invalid role. must be one of: STUDENT, ADMIN");
                return null;
            }
        }

        res.status(204);

        return "";
    };

    public static final Route testModeGet = (req, res) -> {
        User latestTestStudent = CanvasService.getCanvasIntegration().getTestStudent();

        UserDao userDao = DaoService.getUserDao();
        User user = userDao.getUser("test");

        if (user == null) {
            user = latestTestStudent;
            userDao.insertUser(latestTestStudent);
        } else {
            userDao.setRepoUrl(user.netId(), latestTestStudent.repoUrl());
            userDao.setCanvasUserId(user.netId(), latestTestStudent.canvasUserId());
        }

        DaoService.getSubmissionDao().removeSubmissionsByNetId(user.netId());

        res.cookie("/", "token", generateToken(user.netId()), 14400, false, false);

        res.status(200);

        return null;
    };

    public static final Route commitAnalyticsGet = (req, res) -> {
        String option = req.params(":option");
        String data;

        try {
            data = switch (option) {
                case "update" -> CommitAnalyticsRouter.update();
                case "cached" -> CommitAnalyticsRouter.cached();
                case "when" -> CommitAnalyticsRouter.when();
                default -> throw new IllegalStateException("Not found (invalid option: " + option + ")");
            };
        } catch (Exception e) {
            LoggerFactory.getLogger(AdminController.class).error(e.getMessage());
            if (e instanceof IllegalStateException) res.status(404);
            else res.status(500);
            return e.getMessage();
        }

        res.status(200);

        return data;
    };

    public static final Route honorCheckerZipGet = (req, res) -> {
        String sectionStr = req.params(":section");
        String filePath;

        res.header("Content-Type", "application/zip");
        res.header("Content-Disposition", "attachment; filename=" + "downloaded_file.zip");

        try {
            filePath = HonorCheckerCompiler.compileSection(Integer.parseInt(sectionStr));
            try (FileInputStream fis = new FileInputStream(filePath);
                 OutputStream os = res.raw().getOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }

                fis.close();
                new File(filePath).delete();

                res.status(200);
                return res.raw();
            }
        } catch (Exception e) {
            res.status(500);
            return e.getMessage();
        }
    };
}
