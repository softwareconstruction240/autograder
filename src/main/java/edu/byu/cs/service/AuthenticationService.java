package edu.byu.cs.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Optional;

import edu.byu.cs.controller.RedirectController;
import edu.byu.cs.util.JwtUtils;
import edu.byu.cs.util.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.exception.InternalServerException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;

/**
 * Contains service logic for the {@link RedirectController}. <br> View the
 * <a href="https://developer.byu.edu/data/api-usage/learn-about-oauth-2-0">BYU API documentation</a>
 * to understand OAuth works, if needed. Other sites on this page are a great resource as well,
 * particularly verifying JWT tokens. You should also check out the {@link JwtUtils} class as well.
 * <br><br>
 * The {@code AuthenticationService} ensures the user authenticates before they access
 * and use the AutoGrader.
 */
public class AuthenticationService {
    public static final String BYU_API_URL = "https://api-sandbox.byu.edu";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static Instant configExpiration = Instant.now();
    private static Instant keyExpiration = Instant.now();

    public static OpenIDConfig config;


    /**
     * Validates an identity token and retrieves the associated user.
     * <br>
     * If the user exists in the database, they are returned directly. Otherwise, the user
     * is retrieved from Canvas and stored in the database before being returned
     *
     * @param ticket the identity token in the form of a JWT
     * @return the user, either stored in the database or from Canvas if not
     * @throws InternalServerException if an error arose during ticket validation or user retrieval
     * @throws BadRequestException     if JWT validation failed
     * @throws DataAccessException     if there was an issue storing the user in the database
     * @throws CanvasException         if there was an issue getting the user from Canvas
     */
    public static User callback(String ticket) throws InternalServerException, BadRequestException, DataAccessException, CanvasException {
        String netId;
        try {
            netId = AuthenticationService.validateToken(ticket);
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error validating ticket", e);
            throw new InternalServerException("Error validating ticket", e);
        }

        if (netId == null) {
            throw new BadRequestException("Ticket validation failed");
        }

        UserDao userDao = DaoService.getUserDao();

        User user;
        // Check if student is already in the database
        try {
            user = userDao.getUser(netId);
        } catch (DataAccessException e) {
            LOGGER.error("Couldn't get user from database", e);
            throw new InternalServerException("Couldn't get user from database", e);
        }

        // If there isn't a student in the database with this netId
        if (user == null) {
            try {
                user = CanvasService.getCanvasIntegration().getUser(netId);
            } catch (CanvasException e) {
                LOGGER.error("Error getting user from canvas", e);
                throw e;
            }

            userDao.insertUser(user);
            LOGGER.info("Registered {}", user);
        }
        return user;
    }

    /**
     *
     * @param token the JWT token to validate
     * @return the JWT subject (currently the netid)
     * @throws InternalServerException when unable to grab keys or OpenID config
     */
    public static String validateToken(String token) throws InternalServerException, IOException, InterruptedException {
        if (isExpired(keyExpiration)){
            if (isExpired(configExpiration)){
                cacheBYUOpenIDConfig();
            }
            cacheJWK();

        }
        return JwtUtils.validateTokenAgainstKeys(token);
    }

    public static TokenResponse exchangeCodeForTokens(String code) throws IOException, InterruptedException, InternalServerException {
        if (isExpired(configExpiration)){
            cacheBYUOpenIDConfig();
        }

        String formData = "grant_type=authorization_code" +
                "&client_id=" + URLEncoder.encode(ApplicationProperties.clientId(), StandardCharsets.UTF_8) +
                "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(ApplicationProperties.casCallbackUrl(), StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.tokenEndpoint))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();


        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return new Gson().fromJson(response.body(), TokenResponse.class);

    }


    public record TokenResponse(
            @SerializedName("access_token") String accessToken,
            @SerializedName("id_token") String idToken,
            @SerializedName("refresh_token") String refreshToken,
            @SerializedName("expires_in") int expiresIn,
            @SerializedName("token_type") String tokenType

    ) {}

    /**
     * Caches the info from the api needed to complete the OAuth transaction.
     * @throws InternalServerException when there is something suspicious about the OpenID config
     */
    private static void cacheBYUOpenIDConfig() throws InternalServerException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BYU_API_URL + "/.well-known/openid-configuration"))
                .header("Accept", "application/json")
                .GET()
                .build();
            HttpResponse<String> response = NetworkUtils.makeJsonGetRequest(BYU_API_URL +
                    "/.well-known/openid-configuration");

            configExpiration = NetworkUtils.getCacheTime(response);

            OpenIDConfig config = new Gson().fromJson(response.body(), OpenIDConfig.class);
            if (isValidConfig(config)){
                AuthenticationService.config = config;
            }
            else {
                throw new InternalServerException("Unable to verify OpenID config", null);
            }

    }

    /**
     * Grabs a set of JWKs from the endpoint specified in the config. These are public keys used to verify that
     * JWTs received are in fact from BYU. The sandbox api should usually only have one at a time, but they
     * can rotate the keys whenever, so we must be able to account for multiple.
     */
    private static void cacheJWK () throws IOException, InterruptedException, InternalServerException {

        HttpResponse<String> response = NetworkUtils.makeJsonGetRequest(config.keyUri);

        keyExpiration = NetworkUtils.getCacheTime(response);

        JwtUtils.readJWKs(response.body());

    }

    /**
     * Some of the fields delivered for the OpenID config. Call the endpoint yourself to see the full config.
     * @param issuer - should be a byu api, and the specific API called
     * @param authorizationEndpoint - where the browser should redirect the user on login
     * @param tokenEndpoint - where the browser should confirm the redirect worked
     * @param keyUri - where public keys to verify JWT tokens are received from
     * @param scopes - only scope currently is openid
     * @param encryptions - types of encryptions supported when signing the JWT tokens
     */
    public record OpenIDConfig(
            String issuer,
            @SerializedName("authorization_endpoint") String authorizationEndpoint,
            @SerializedName("token_endpoint") String tokenEndpoint,
            @SerializedName("jwks_uri") String keyUri,
            @SerializedName("scopes_supported") Collection<String> scopes,
            @SerializedName("id_token_signing_alg_values_supported")Collection<String> encryptions
    ){};

    /**
     * Ensures the config came from the issuer, BYU API, and that any redirect links also are from the BYU API.
     * <br><br>
     * Also logs any changes to the OpenID config that may need to be looked at.
     * @param config
     * @return true if valid, false if there's a glaring problem
     */
    private static boolean isValidConfig(OpenIDConfig config){
        if (!config.issuer().equals(BYU_API_URL)){
            return false;
        }
        if (!config.equals(AuthenticationService.config) && AuthenticationService.config != null){
            LOGGER.info("OpenID config has changed: {}", config);
        }
        if (config.scopes().size()!= 1){
            LOGGER.warn("Config has multiple scopes: {}", config);
        }
        if (config.encryptions().size()!=1){
            LOGGER.warn("Config has multiple encryption types: {}", config);
        }
        return isValidUrl(config.authorizationEndpoint) && isValidUrl(config.tokenEndpoint()) && 
               isValidUrl(config.keyUri());
    }

    private static boolean isExpired(Instant time){
        return time.isBefore(Instant.now());
    }

    /**
     * Validates that a URL uses HTTPS and has the same host as the BYU API URL.
     * @param urlString the URL to validate
     * @return true if the URL is valid, false otherwise
     */
    private static boolean isValidUrl(String urlString) {
        try {
            URI uri = new URI(urlString);
            URI baseUri = new URI(BYU_API_URL);
            
            // Verify HTTPS is used
            if (!"https".equals(uri.getScheme())) {
                return false;
            }
            
            // Verify the host matches the base API URL's host
            String host = uri.getHost();
            String expectedHost = baseUri.getHost();
            return host != null && host.equals(expectedHost);
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return authorization url with parameters filled in
     * @throws InternalServerException when unable to reload OpenID config
     */
    public static String getAuthorizationUrl() throws InternalServerException{
        try {
            if (isExpired(configExpiration)) {
                cacheBYUOpenIDConfig();
            }
        } catch (IOException | InterruptedException e){
            LOGGER.error("Unable to cache OpenID Config", e);
            throw new InternalServerException("Unable to verify identity", e);
        }
        return AuthenticationService.config.authorizationEndpoint()
            + "?response_type=code&client_id=" + URLEncoder.encode(ApplicationProperties.clientId(), StandardCharsets.UTF_8)
            + "&redirect_uri=" + URLEncoder.encode(ApplicationProperties.casCallbackUrl(), StandardCharsets.UTF_8)
            + "&scope=" + URLEncoder.encode("openid", StandardCharsets.UTF_8);
    }
}
