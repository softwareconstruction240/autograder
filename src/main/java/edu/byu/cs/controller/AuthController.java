package edu.byu.cs.controller;

import edu.byu.cs.controller.httpexception.BadRequestException;
import edu.byu.cs.controller.httpexception.InternalServerException;
import edu.byu.cs.controller.httpexception.ResourceForbiddenException;
import edu.byu.cs.controller.httpexception.UnauthorizedException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import edu.byu.cs.util.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Filter;
import spark.Route;

import static edu.byu.cs.util.JwtUtils.validateToken;

public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    /**
     * A filter that verifies that the request has a valid JWT in the Authorization header.
     * If the request is valid, the netId is added to the session for later use.
     */
    public static final Filter verifyAuthenticatedMiddleware = (req, res) -> {
        String token = req.cookie("token");

        if (token == null) {
            throw new UnauthorizedException();
        }
        String netId = validateToken(token);

        // token is expired or invalid
        if (netId == null) {
            res.cookie("/", "token", "", 0, false, false);
            throw new UnauthorizedException();
        }

        UserDao userDao = DaoService.getUserDao();
        User user;
        try {
            user = userDao.getUser(netId);
        } catch (DataAccessException e) {
            LOGGER.error("Error getting user from database", e);
            throw new InternalServerException("Error getting user from database", e);
        }

        if (user == null) {
            LOGGER.error("Received request from unregistered user. This shouldn't be possible: {}", netId);
            throw new BadRequestException("You must register first.");
        }

        req.session().attribute("user", user);
    };

    public static final Filter verifyAdminMiddleware = (req, res) -> {
        User user = req.session().attribute("user");

        if (user.role() != User.Role.ADMIN) {
            throw new ResourceForbiddenException();
        }
    };

    public static final Route meGet = (req, res) -> {
        User user = req.session().attribute("user");

        res.status(200);
        res.type("application/json");
        return Serializer.serialize(user);
    };

}
