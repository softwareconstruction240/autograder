package edu.byu.cs.controller;

import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.exception.InternalServerException;
import edu.byu.cs.controller.exception.ResourceForbiddenException;
import edu.byu.cs.controller.exception.UnauthorizedException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.model.User;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static edu.byu.cs.util.JwtUtils.validateToken;

/**
 * Handles authentication-related HTTP endpoints
 */
public class AuthController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);

    /**
     * A handler that verifies that the request has a valid JWT in the Authorization header.
     * If the request is valid, the netId is added to the session for later use.
     */
    public static final Handler verifyAuthenticatedMiddleware = ctx -> {
        String token = ctx.cookie("token");

        if (token == null) {
            throw new UnauthorizedException();
        }
        String netId = validateToken(token);

        // token is expired or invalid
        if (netId == null) {
            ctx.cookie("token", "", 0);
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

        ctx.sessionAttribute("user", user);
    };

    public static final Handler verifyAdminMiddleware = ctx -> {
        User user = ctx.sessionAttribute("user");

        if (user == null) {
            throw new UnauthorizedException("No user credentials found");
        }

        if (user.role() != User.Role.ADMIN) {
            throw new ResourceForbiddenException();
        }
    };

    public static final Handler meGet = ctx -> {
        User user = ctx.sessionAttribute("user");
        if (user == null) {
            ctx.result("No user found.");
        } else {
            ctx.json(user);
        }
    };

}
