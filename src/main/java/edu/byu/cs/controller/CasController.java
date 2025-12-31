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
 * Handles CAS-related HTTP endpoints. CAS, standing for <em>Central Authentication Service</em>,
 * is BYU's centralized authentication provider for all BYU users
 */
public class CasController {
    //FIXME: when in production mode should be api-production
    public static final String BYU_OAUTH_URL = "https://api-sandbox.byu.edu";

    public static final Handler callbackGet = ctx -> {
        System.out.println("Callback called");
        String code = ctx.queryParam("code");
        System.out.println("code extracted");
        //TODO: throw a fit if there's no code
        CasService.TokenResponse response = CasService.exchangeCodeForTokens(code);
        System.out.println("Token recieved");
        System.out.println(CasService.callPersonApi(response.accessToken()));
        String ticket = ctx.queryParam("ticket");

        User user;
        try {
            user = CasService.callback(ticket);
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
        //ctx.redirect(CasService.BYU_CAS_URL + "/login" + "?service=" + ApplicationProperties.casCallbackUrl());
        ctx.redirect(BYU_OAUTH_URL + "/authorize" + "?response_type=code" + "&client_id="+
                ApplicationProperties.clientId() + "&redirect_uri=" + ApplicationProperties.casCallbackUrl());
    };


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
