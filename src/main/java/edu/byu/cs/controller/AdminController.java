package edu.byu.cs.controller;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.ItemNotFoundException;
import edu.byu.cs.model.User;
import edu.byu.cs.service.AdminService;
import edu.byu.cs.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.io.OutputStream;
import java.util.Collection;

import static edu.byu.cs.util.JwtUtils.generateToken;
import static spark.Spark.halt;

public class AdminController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

    public static final Route usersGet = (req, res) -> {
        Collection<User> users;
        try {
            users = AdminService.getUsers();
        } catch (DataAccessException e) {
            LOGGER.error("Error getting users", e);
            halt(500);
            return null;
        }

        res.type("application/json");
        res.status(200);

        return Serializer.serialize(users);

    };

    public static final Route userPatch = (req, res) -> {
        String netId = req.params(":netId");
        String firstName = req.queryParams("firstName");
        String lastName = req.queryParams("lastName");
        String repoUrl = req.queryParams("repoUrl");
        String roleString = req.queryParams("role");

        User.Role role = null;
        if (roleString != null) {
            try {
                role = User.Role.parse(roleString);
            } catch (IllegalArgumentException e) {
                halt(400, "invalid role. must be one of: STUDENT, ADMIN");
                return null;
            }
        }

        User userData = new User(netId, -1, firstName, lastName, repoUrl, role);
        try {
            AdminService.updateUser(userData);

        } catch (DataAccessException e) {
            LOGGER.error("Error getting user", e);
            halt(500);
            return null;

        } catch (ItemNotFoundException e) {
            halt(404, "user not found");
            return null;

        }

        res.status(204);

        return "";
    };

    public static final Route testModeGet = (req, res) -> {
        User testStudent;
        try {
            testStudent = AdminService.updateTestStudent();
        } catch (CanvasException | DataAccessException e) {
            halt(500);
            return null;
        }

        res.cookie("/", "token", generateToken(testStudent.netId()), 14400, false, false);

        res.status(200);

        return null;
    };

    public static final Route commitAnalyticsGet = (req, res) -> {
        String option = req.params(":option");
        String data;

        try {
            data = AdminService.getCommitAnalytics(option);
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

        try (OutputStream os = res.raw().getOutputStream()) {
            AdminService.streamHonorCheckerZip(sectionStr, os);

        } catch (Exception e) {
            LOGGER.error("Error compiling honor checker", e);
            res.status(500);
            return e.getMessage();
        }

        res.status(200);

        res.header("Content-Type", "application/zip");
        res.header("Content-Disposition", "attachment; filename=" + "downloaded_file.zip");

        return res.raw();

    };

    public static Route sectionsGet = (req, res) -> {
        try {
            CanvasSection[] sections = AdminService.getAllSections();
            res.type("application/json");
            res.status(200);
            return Serializer.serialize(sections);
        } catch (CanvasException e) {
            res.status(500);
            return e.getMessage();
        }
    };
}
