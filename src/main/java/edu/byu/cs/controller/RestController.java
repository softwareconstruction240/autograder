package edu.byu.cs.controller;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static spark.Spark.*;

public class RestController {

    private static final String CAS_SERVER_URL = "https://cas.byu.edu/cas";
    private static final String APP_URL = "http://localhost:8080";
    private static final String SERVICE_VALIDATE_ENDPOINT = CAS_SERVER_URL + "/serviceValidate";

    public static void registerRoutes() {

        staticFiles.location("/public");

        get("/callback", (req, res) -> {
            String ticket = req.queryParams("ticket");

            String netId = validateCasTicket(ticket);

            if (netId != null)
                return "You're authenticated!";

            halt(400, "Ticket validation failed");
            return null;
        });

        get("/login", (req, res) -> {
            res.redirect(CAS_SERVER_URL + "/login?service=" + APP_URL + "/callback");
            return null;
        });


        // authenticated routes
        path("/api", () -> {
            before("/*", (req, res) -> {
//              ... check if authenticated
//              halt(401, "You are not welcome here");

            });
            post("/logout", (req, res) -> "Bye World");


        });

    }

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
