package edu.byu.cs.controller;

import com.google.gson.Gson;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import spark.Route;

import java.util.Collection;

public class AdminController {
    public static Route usersGet = (req, res) -> {
        UserDao userDao = DaoService.getUserDao();

        Collection<User> users = userDao.getUsers();

        res.type("application/json");
        res.status(200);

        return new Gson().toJson(users);

    };
}
