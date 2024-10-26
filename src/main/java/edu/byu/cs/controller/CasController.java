package edu.byu.cs.controller;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.service.CasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static edu.byu.cs.util.JwtUtils.generateToken;
import static spark.Spark.halt;

public class CasController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasController.class);

    public static final Route callbackGet = (req, res) -> {
        // TODO Move logic into CasService?

        String ticket = req.queryParams("ticket");

        String netId;
        try {
            netId = CasService.validateCasTicket(ticket);
        } catch (IOException e) {
            LOGGER.error("Error validating ticket", e);
            halt(500);
            return null;
        }

        if (netId == null) {
            halt(400, "Ticket validation failed");
            return null;
        }

        UserDao userDao = DaoService.getUserDao();

        User user;
        // Check if student is already in the database
        try {
            user = userDao.getUser(netId);
        } catch (DataAccessException e) {
            LOGGER.error("Couldn't get user from database", e);
            halt(500);
            return null;
        }

        // If there isn't a student in the database with this netId
        if (user == null) {
            try {
                user = CanvasService.getCanvasIntegration().getUser(netId);
            } catch (CanvasException e) {
                LOGGER.error("Error getting user from canvas", e);

                String errorUrlParam = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
                res.redirect(ApplicationProperties.frontendUrl() + "/login?error=" + errorUrlParam, 302);
                halt(500);
                return null;
            }

            userDao.insertUser(user);
            LOGGER.info("Registered {}", user);
        }

        // FIXME: secure cookie with httpOnly
        res.cookie("/", "token", generateToken(netId), 14400, false, false);
        res.redirect(ApplicationProperties.frontendUrl(), 302);
        return null;
    };

    public static final Route loginGet = (req, res) -> {
        // check if already logged in
        if (req.cookie("token") != null) {
            res.redirect(ApplicationProperties.frontendUrl(), 302);
            return null;
        }
        res.redirect(CasService.BYU_CAS_URL + "/login" + "?service=" + ApplicationProperties.casCallbackUrl());
        return null;
    };

    public static final Route logoutPost = (req, res) -> {
        if (req.cookie("token") == null) {
            res.redirect(ApplicationProperties.frontendUrl(), 401);
            return null;
        }

        // TODO: call cas logout endpoint with ticket
        res.removeCookie("/", "token");
        res.redirect(ApplicationProperties.frontendUrl(), 200);
        return null;
    };

}
