package edu.byu.cs.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.service.CasService;
import edu.byu.cs.service.ConfigService;
import static edu.byu.cs.util.JwtUtils.generateToken;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

/**
 * Handles Redirect related endpoints, including the class chat link (i. e. Slack or Discord)
 * and authentication redirects.
 */
public class RedirectController {

    public static final Handler callbackGet = ctx -> {
        String code = ctx.queryParam("code");
        //TODO: throw a fit if there's no code
        CasService.TokenResponse response = CasService.exchangeCodeForTokens(code);

        //String ticket = ctx.queryParam("ticket");

        User user;
        try {
            user = CasService.callback(response.idToken());
        } catch (CanvasException e) {
            String errorUrlParam = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            ctx.redirect(ApplicationProperties.frontendUrl() + "/login?error=" + errorUrlParam, HttpStatus.FOUND);
            return;
        }

        // FIXME: secure cookie with httpOnly
        ctx.cookie("token", generateToken(user.netId()), 14400);

        redirect(ctx);
    };



    public static final Handler loginGet = ctx -> {
        // check if already logged in
        if (ctx.cookie("token") != null) {
            redirect(ctx);
            return;
        }
        ctx.redirect(CasService.getAuthorizationUrl());
    };

    /**
     * Redirects students to the class chat invite. At the time we used Slack, and therefore all references use
     * that name
     */
    private static void redirect(Context ctx) throws DataAccessException {
        String redirectTo;
        if(ctx.sessionAttribute("slack") != null) {
            redirectTo = ConfigService.getSlackLink();
            ctx.sessionAttribute("slack", null);
        }
        else redirectTo = ApplicationProperties.frontendUrl();
        ctx.redirect(redirectTo, HttpStatus.FOUND);
    }

    public static final Handler logoutPost = ctx -> {
        if (ctx.cookie("token") == null) {
            ctx.redirect(ApplicationProperties.frontendUrl(), HttpStatus.UNAUTHORIZED);
            return;
        }

        // TODO: call cas logout endpoint with ticket
        ctx.removeCookie("token", "/");
        ctx.redirect(ApplicationProperties.frontendUrl(), HttpStatus.OK);
    };

}
