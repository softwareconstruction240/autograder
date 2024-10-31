package edu.byu.cs.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.controller.BadRequestException;
import edu.byu.cs.controller.InternalServerException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class CasService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CasService.class);
    public static final String BYU_CAS_URL = "https://cas.byu.edu/cas";

    public static User callback(String ticket) throws InternalServerException, BadRequestException, DataAccessException, CanvasException {
        String netId;
        try {
            netId = CasService.validateCasTicket(ticket);
        } catch (IOException e) {
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

}
