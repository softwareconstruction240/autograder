package edu.byu.cs.controller;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.byu.cs.properties.ConfigManager;
import spark.Route;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static edu.byu.cs.controller.JwtUtils.generateToken;
import static spark.Spark.halt;

public class CasController {

    public static Route callbackGet = (req, res) -> {
        String ticket = req.queryParams("ticket");

        String netId = validateCasTicket(ticket);

        if (netId == null) {
            halt(400, "Ticket validation failed");
            return null;
        }

        res.cookie("token", generateToken(netId), 14400, true, true);
        res.redirect("/", 302);
        return null;
    };

    public static Route loginGet = (req, res) -> {
        // check if already logged in
        if (req.cookie("token") != null) {
            res.redirect("/", 302);
            return null;
        }
        res.redirect(
                ConfigManager.casServerUrl() + ConfigManager.casServerLoginEndpoint() + "?service="
                        + ConfigManager.appUrl() + "/auth/callback");
        return null;
    };

    public static Route logoutGet = (req, res) -> {
        if (req.cookie("token") == null) {
            res.status(401);
            return "You are not logged in.";
        }


        // TODO: call cas logout endpoint with ticket
        res.removeCookie("token");
        res.status(200);
        return "You are logged out.";
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
        String validationUrl = ConfigManager.casServerUrl() + ConfigManager.casServerServiceValidateEndpoint() +
                "?ticket=" + ticket +
                "&service=" + ConfigManager.appUrl() + "/callback";

        URI uri = URI.create(validationUrl);
        HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();

        try {
            String body = new String(connection.getInputStream().readAllBytes());

            Map<?, ?> casServiceResponse = XmlMapper
                    .builder()
                    .build().readValue(body, Map.class);
            return (String) ((Map<?, ?>) casServiceResponse.get("authenticationSuccess")).get("user");

        } catch (Exception e) {
            System.err.println("Error with response from CAS server:");
            e.printStackTrace();
            throw e;
        } finally {
            connection.disconnect();
        }
    }
}
