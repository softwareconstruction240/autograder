package edu.byu.cs.service;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
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
