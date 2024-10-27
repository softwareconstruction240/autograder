package edu.byu.cs.controller;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.controller.httpexception.BadRequestException;
import edu.byu.cs.controller.httpexception.InternalServerException;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.service.CasService;
import spark.Route;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static edu.byu.cs.util.JwtUtils.generateToken;
import static spark.Spark.halt;

public class CasController {
    public static final Route callbackGet = (req, res) -> {
        String ticket = req.queryParams("ticket");

        User user;
        try {
            user = CasService.callback(ticket);
        } catch (InternalServerException | DataAccessException e) {
            halt(500);
            return null;
        } catch (BadRequestException e) {
            halt(400);
            return null;
        } catch (CanvasException e) {
            String errorUrlParam = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            res.redirect(ApplicationProperties.frontendUrl() + "/login?error=" + errorUrlParam, 302);
            halt(500);
            return null;
        }

        // FIXME: secure cookie with httpOnly
        res.cookie("/", "token", generateToken(user.netId()), 14400, false, false);
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
