package edu.byu.cs.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.controller.exception.UnauthorizedException;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.service.AuthenticationService;
import edu.byu.cs.service.ConfigService;
import static edu.byu.cs.util.JwtUtils.generateToken;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

/**
 * Handles Redirect related endpoints, including the class chat link (i. e. Slack or Discord)
 * and authentication redirects.
 */
public class RedirectController {
    private final AuthenticationService authenticationService;
    private final ConfigService configService;

    public RedirectController(AuthenticationService authenticationService, ConfigService configService) {
        this.authenticationService = authenticationService;
        this.configService = configService;
    }

    public Handler callbackGet() {
        return ctx -> {
            String code = ctx.queryParam("code");
            if (code == null){
                throw new UnauthorizedException();
            }
            AuthenticationService.TokenResponse response = authenticationService.exchangeCodeForTokens(code);

            User user;
            try {
                user = authenticationService.callback(response.idToken());
            } catch (CanvasException e) {
                String errorUrlParam = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
                ctx.redirect(ApplicationProperties.frontendUrl() + "/login?error=" + errorUrlParam, HttpStatus.FOUND);
                return;
            }

            ctx.cookie (new Cookie(
                    "token",
                    generateToken(user.netId()),
                    "/",
                    14400,
                    authenticationService.isSecure(),
                    0,
                    true
            ));

            redirect(ctx);
        };
    }



    public Handler loginGet() {
        return ctx -> {
            // check if already logged in
            if (ctx.cookie("token") != null) {
                redirect(ctx);
                return;
            }
            ctx.redirect(authenticationService.getAuthorizationUrl());
        };
    }

    /**
     * Redirects students to the class chat invite. At the time we used Slack, and therefore all references use
     * that name
     */
    private void redirect(Context ctx) throws DataAccessException {
        String redirectTo;
        if(ctx.sessionAttribute("slack") != null) {
            redirectTo = configService.getSlackLink();
            ctx.sessionAttribute("slack", null);
        }
        else redirectTo = ApplicationProperties.frontendUrl();
        ctx.redirect(redirectTo, HttpStatus.FOUND);
    }

    public Handler logoutPost() {
        return ctx -> {
            if (ctx.cookie("token") == null) {
                ctx.redirect(ApplicationProperties.frontendUrl(), HttpStatus.UNAUTHORIZED);
                return;
            }

            // TODO: call logout endpoint with token
            ctx.removeCookie("token", "/");
            ctx.redirect(ApplicationProperties.frontendUrl(), HttpStatus.OK);
        };
    }

}
