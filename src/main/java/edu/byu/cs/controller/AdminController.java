package edu.byu.cs.controller;

import com.google.gson.Gson;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import spark.Route;

import java.util.Collection;

import static edu.byu.cs.controller.JwtUtils.generateToken;
import static spark.Spark.halt;

public class AdminController {
    public static Route usersGet = (req, res) -> {
        UserDao userDao = DaoService.getUserDao();

        Collection<User> users = userDao.getUsers();

        res.type("application/json");
        res.status(200);

        return new Gson().toJson(users);

    };

    public static Route userPatch = (req, res) -> {
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

    public static Route testModeGet = (req, res) -> {
        User latestTestStudent = CanvasIntegration.getTestStudent();

        UserDao userDao = DaoService.getUserDao();
        User user = userDao.getUser("test");

        if (user == null) {
            user = latestTestStudent;
            userDao.insertUser(latestTestStudent);
        } else {
            userDao.setRepoUrl(user.netId(), latestTestStudent.repoUrl());
            userDao.setCanvasUserId(user.netId(), latestTestStudent.canvasUserId());
        }

        res.cookie("/", "token", generateToken(user.netId()), 14400, false, false);

        res.status(200);

        return null;
    };
}
