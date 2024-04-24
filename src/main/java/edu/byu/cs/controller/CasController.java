package edu.byu.cs.controller;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Route;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static edu.byu.cs.util.JwtUtils.generateToken;
import static spark.Spark.halt;

public class CasController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasController.class);
    public static final String BYU_CAS_URL = "https://cas.byu.edu/cas";

    public static final Route callbackGet = (req, res) -> {
        String ticket = req.queryParams("ticket");

        String netId = validateCasTicket(ticket);

        if (netId == null) {
            halt(400, "Ticket validation failed");
            return null;
        }

        UserDao userDao = DaoService.getUserDao();

        User user = null;
        try {
            user = userDao.getUser(netId);
        } catch (DataAccessException e) {
            LOGGER.error("Couldn't get user from database", e);
            halt(500);
            return null;
        }

        if (user == null) {
            try {
                user = CanvasService.getCanvasIntegration().getUser(netId);
            } catch (CanvasException e) {
                LOGGER.error("Couldn't create user from Canvas", e);

                String errorUrlParam = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
                res.redirect(ApplicationProperties.frontendUrl() + "/login?error=" + errorUrlParam, 302);
                halt(500);
                return null;
            }

            if (userDao.repoUrlClaimed(user.repoUrl())) {
                LOGGER.error("Repo URL already claimed: " + user.repoUrl());

                String errorUrlParam = URLEncoder.encode("Repo URL already claimed. Meet with a TA for help resolving this.", StandardCharsets.UTF_8);
                res.redirect(ApplicationProperties.frontendUrl() + "/login?error=" + errorUrlParam, 302);
                halt(400, "Repo URL already claimed");
                return null;
            }

            userDao.insertUser(user);
            LOGGER.info("Registered " + user);
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
        res.redirect(
                BYU_CAS_URL + "/login"
                        + "?service=" + ApplicationProperties.casCallbackUrl());
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

    /**
     * Validates a CAS ticket and returns the netId of the user if valid <br/>
     * <a href="https://calnet.berkeley.edu/calnet-technologists/cas/how-cas-works">Berkeley CAS docs</a>
     *
     * @param ticket the ticket to validate
     * @return the netId of the user if valid, null otherwise
     * @throws IOException if there is an error with the CAS server response
     */
    private static String validateCasTicket(String ticket) throws IOException {
        String validationUrl = BYU_CAS_URL + "/serviceValidate" +
                "?ticket=" + ticket +
                "&service=" + ApplicationProperties.casCallbackUrl();


        URI uri = URI.create(validationUrl);
        HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();

        try {
            String body = new String(connection.getInputStream().readAllBytes());

            Map<?, ?> casServiceResponse = XmlMapper
                    .builder()
                    .build().readValue(body, Map.class);
            return (String) ((Map<?, ?>) casServiceResponse.get("authenticationSuccess")).get("user");

        } catch (Exception e) {
            LOGGER.error("Error with response from CAS server:", e);
            throw e;
        } finally {
            connection.disconnect();
        }
    }
}
