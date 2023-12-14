package edu.byu.cs.controller.security;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static edu.byu.cs.controller.security.JwtUtils.generateToken;
import static spark.Spark.*;

public class CasController {

    private static final String CAS_SERVER_URL = "https://cas.byu.edu/cas";
    private static final String APP_URL = "http://localhost:8080";
    private static final String SERVICE_VALIDATE_ENDPOINT = CAS_SERVER_URL + "/serviceValidate";



    public static void registerRoutes() {
        get("/callback", (req, res) -> {
            String ticket = req.queryParams("ticket");

            String netId = validateCasTicket(ticket);

            if (netId == null) {
                halt(400, "Ticket validation failed");
                return null;
            }

            res.cookie("token", generateToken(netId), 14400, true, true);
            res.redirect("/", 302);
            return null;
        });

        get("/login", (req, res) -> {
            // check if already logged in
            if (req.cookie("token") != null) {
                res.redirect("/", 302);
                return null;
            }
            res.redirect(CAS_SERVER_URL + "/login?service=" + APP_URL + "/callback");
            return null;
        });

        get("/logout", (req, res) -> {
            res.removeCookie("token");
            res.status(200);
            return "You are logged out.";
        });
    }

    /**
     * Validates a CAS ticket and returns the netId of the user if valid <br/>
     * <a href="https://calnet.berkeley.edu/calnet-technologists/cas/how-cas-works">Berkeley CAS docs</a>
     *
     * @param ticket the ticket to validate
     * @return the netId of the user if valid, null otherwise
     * @throws IOException if there is an error with the CAS server response
     */
    private static String validateCasTicket(String ticket) throws IOException {
        String validationUrl = SERVICE_VALIDATE_ENDPOINT +
                "?ticket=" + ticket +
                "&service=" + APP_URL + "/callback";

        URI uri = URI.create(validationUrl);
        HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();

        try {
            String body = new String(connection.getInputStream().readAllBytes());

            Map casServiceResponse = XmlMapper
                    .builder()
                    .build().readValue(body, Map.class);
            return (String) ((Map) casServiceResponse.get("authenticationSuccess")).get("user");

        } catch (Exception e) {
            System.err.println("Error with response from CAS server:");
            e.printStackTrace();
            throw e;
        } finally {
            connection.disconnect();
        }
    }
}
