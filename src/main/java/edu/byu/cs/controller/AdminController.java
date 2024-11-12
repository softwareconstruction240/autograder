package edu.byu.cs.controller;

import edu.byu.cs.analytics.CommitAnalyticsRouter;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.honorChecker.HonorCheckerCompiler;
import edu.byu.cs.model.User;
import edu.byu.cs.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.Collection;

import static edu.byu.cs.util.JwtUtils.generateToken;
import static spark.Spark.halt;

public class AdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

    public static final Route usersGet = (req, res) -> {
        UserDao userDao = DaoService.getUserDao();

        Collection<User> users;
        try {
            users = userDao.getUsers();
        } catch (DataAccessException e) {
            LOGGER.error("Error getting users", e);
            halt(500);
            return null;
        }

        res.type("application/json");
        res.status(200);

        return Serializer.serialize(users);

    };

    public static final Route testModeGet = (req, res) -> {
        User latestTestStudent;
        try {
            latestTestStudent = CanvasService.getCanvasIntegration().getTestStudent();
        } catch (CanvasException e) {
            LOGGER.error("Error getting test student", e);
            halt(500);
            return null;
        }

        UserDao userDao = DaoService.getUserDao();
        User user;
        try {
            user = userDao.getUser("test");
        } catch (DataAccessException e) {
            LOGGER.error("Error getting user", e);
            halt(500);
            return null;
        }

        try {

            if (user == null) {
                user = latestTestStudent;
                userDao.insertUser(latestTestStudent);
            } else {
                userDao.setRepoUrl(user.netId(), latestTestStudent.repoUrl());
                userDao.setCanvasUserId(user.netId(), latestTestStudent.canvasUserId());
            }

        } catch (DataAccessException e) {
            LOGGER.error("Error updating user", e);
            halt(500);
            return null;
        }

        try {
            DaoService.getSubmissionDao().removeSubmissionsByNetId(user.netId(), 0);
        } catch (DataAccessException e) {
            LOGGER.error("Error removing submissions", e);
            halt(500);
            return null;
        }

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
            LOGGER.error(e.getMessage());
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
            LOGGER.error("Error compiling honor checker", e);
            res.status(500);
            return e.getMessage();
        }
    };

    public static Route sectionsGet = (req, res) -> {
        try {
            CanvasSection[] sections = CanvasService.getCanvasIntegration().getAllSections();
            res.type("application/json");
            res.status(200);
            return Serializer.serialize(sections);
        } catch (CanvasException e) {
            res.status(500);
            return e.getMessage();
        }
    };
}
