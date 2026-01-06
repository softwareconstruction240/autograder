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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;

import edu.byu.cs.controller.RedirectController;
import edu.byu.cs.util.JwtUtils;
import io.jsonwebtoken.security.JwkSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
 * <a href="https://calnet.berkeley.edu/calnet-technologists/cas/how-cas-works">Berkeley CAS docs</a>
 * to understand how CAS, or <em>Central Authentication Service</em>, works, if needed.
 * <br><br>
 * The {@code CasService} ensures user authentication using BYU's CAS before they access
 * and use the AutoGrader.
 */
public class CasService {
    public static final String BYU_CAS_URL = "https://cas.byu.edu/cas";
    public static final String BYU_API_URL = "https://api-sandbox.byu.edu";
    private static final Logger LOGGER = LoggerFactory.getLogger(CasService.class);

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static Instant configExpiration = Instant.now();
    private static Instant keyExpiration = Instant.now();

    public static openIDConfig config;
    private static JwkSet byuPublicKeys;

    /**
     * Validates a CAS ticket and retrieves the associated user.
     * <br>
     * If the user exists in the database, they are returned directly. Otherwise, the user
     * is retrieved from Canvas and stored in the database before being returned
     *
     * @param ticket the CAS ticket to validate
     * @return the user, either stored in the database or from Canvas if not
     * @throws InternalServerException if an error arose during ticket validation or user retrieval
     * @throws BadRequestException     if ticket validation failed
     * @throws DataAccessException     if there was an issue storing the user in the database
     * @throws CanvasException         if there was an issue getting the user from Canvas
     */
    public static User callback(String ticket) throws InternalServerException, BadRequestException, DataAccessException, CanvasException {
        String netId;
        try {
            netId = CasService.validateToken(ticket);
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
     * Validates a CAS ticket and returns the netId of the user if valid <br/>
     * <a href="https://calnet.berkeley.edu/calnet-technologists/cas/how-cas-works">Berkeley CAS docs</a>
     *
     * @param ticket the ticket to validate
     * @return the netId of the user if valid, null otherwise
     * @throws IOException if there is an error with the CAS server response
     */
    public static String validateCasTicket(String ticket) throws IOException {
        String validationUrl = BYU_CAS_URL + "/serviceValidate" + "?ticket=" + ticket + "&service=" + ApplicationProperties.casCallbackUrl();


        URI uri = URI.create(validationUrl);
        HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();

        try {
            String body = new String(connection.getInputStream().readAllBytes());

            Map<?, ?> casServiceResponse = XmlMapper.builder().build().readValue(body, Map.class);
            return (String) ((Map<?, ?>) casServiceResponse.get("authenticationSuccess")).get("user");

        } catch (Exception e) {
            LOGGER.error("Error with response from CAS server:", e);
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    public static String validateToken(String token) throws InternalServerException, IOException, InterruptedException {
        if (isExpired(keyExpiration)){
            if (isExpired(configExpiration)){
                cacheBYUOpenIDConfig();
            }
            cacheJWK();

        }
        return JwtUtils.validateToken(token, byuPublicKeys);
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

        LOGGER.info(response.body());

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
     * Is only called once to get the inital OpenIDConfig
     */
    public static void initalizeCache() {
        try{
            cacheBYUOpenIDConfig();
            cacheJWK();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void cacheBYUOpenIDConfig() throws InternalServerException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BYU_API_URL + "/.well-known/openid-configuration"))
                .header("Accept", "application/json")
                .GET()
                .build();
        try{
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            configExpiration = getCacheTime(response);

            openIDConfig config = new Gson().fromJson(response.body(), openIDConfig.class);
            if (isValidConfig(config)){
                CasService.config = config;
            }
            else {
                throw new Exception("Invalid OpenIDConfig");
            }

        } catch (Exception e){
            LOGGER.error("Unable to pull openid config from BYU:", e);
            throw new InternalServerException("Unable to determine identity", e);
        }
    }

    private static void cacheJWK () throws IOException, InterruptedException, InternalServerException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.keyUri))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        keyExpiration = getCacheTime(response);

        byuPublicKeys = JwtUtils.readJWKs(response.body());

    }

    private static Instant getCacheTime(HttpResponse<String> response) throws InternalServerException {
        Optional<String> cache = response.headers().firstValue("Cache-Control");
        try{
            String seconds = cache.get().replace("max-age=", "");
            return Instant.now().plusSeconds(Long.parseLong(seconds));
        }
        catch (NoSuchElementException e) {
            throw new InternalServerException("Unable to determine cache time", e);
        }
    }

    public record openIDConfig(
            String issuer,
            @SerializedName("authorization_endpoint") String authorizationEndpoint,
            @SerializedName("token_endpoint") String tokenEndpoint,
            @SerializedName("jwks_uri") String keyUri,
            @SerializedName("scopes_supported") Collection<String> scopes,
            @SerializedName("id_token_signing_alg_values_supported")Collection<String> encryptions
    ){};


    private static boolean isValidConfig(openIDConfig config){
        if (!config.issuer().equals(BYU_API_URL)){
            return false;
        }
        if (!config.equals(CasService.config) && CasService.config != null){
            LOGGER.info("OpenID config has changed: {}", config);
        }
        if (config.scopes().size()!= 1){
            LOGGER.warn("Config has multiple scopes: {}", config);
        }
        if (config.encryptions().size()!=1){
            LOGGER.warn("Config has multiple encryption types: {}", config);
        }
        return config.authorizationEndpoint.contains(BYU_API_URL) && config.tokenEndpoint().contains(BYU_API_URL) &&
                config.keyUri().contains(BYU_API_URL);
    }

    private static boolean isExpired(Instant time){
        return time.isBefore(Instant.now());
    }

    public static String getAuthorizationUrl() throws InternalServerException{
        if (isExpired(configExpiration)){
            cacheBYUOpenIDConfig();
        }
        return CasService.config.authorizationEndpoint() + "?response_type=code" + "&client_id="+
                ApplicationProperties.clientId() + "&redirect_uri=" + ApplicationProperties.casCallbackUrl()
                + "&scope=openid";
    }
}
