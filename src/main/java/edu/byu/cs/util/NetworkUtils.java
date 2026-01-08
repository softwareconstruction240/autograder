package edu.byu.cs.util;

import edu.byu.cs.controller.exception.InternalServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * A utility class that provides methods for making HTTP Requests
 */
public class NetworkUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtils.class);

    /**
     * Leverages the built-in {@link java.net.http.HttpClient} library to make an HTTP Get request.
     * Requires a json response.
     *
     * @param url The URL to request
     * @return The {@link HttpResponse<String>} response, or the errors generated in the process.
     */
    public static HttpResponse<String> makeJsonGetRequest(String url) throws IOException, InterruptedException {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET() // HTTP GET method
                    .build();

            var response =  httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (isFailure(response.statusCode())){
                LOGGER.warn("Error making GET request to '{}': {} status returned", url, response.statusCode());
            }
            return response;
        }
    }

    public static HttpResponse<String> makeParameterizedPostRequest(String url, String formData) throws IOException, InterruptedException {
        try(HttpClient httpClient = HttpClient.newHttpClient()){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(formData))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (isFailure(response.statusCode())){
                LOGGER.warn("Error making POST request to '{}': {} status returned with form data: {}",
                        url, response.statusCode(), formData);
            }
            return response;
        }
    }

    /**
     * Makes an HTTP Get request and returns the body text, or null if an error occurs.
     *
     * @param url The URL to request.
     * @return A string or null.
     */
    public static String readGetRequestBody(String url) {
        try {
            HttpResponse<String> response = makeJsonGetRequest(url);

            return response.body();
        } catch (IOException | InterruptedException e) {
            System.err.print("Error making GET request to '" + url + "': " + e.getMessage());
            LOGGER.warn("Exception thrown while making GET request to '" + url + "': ", e);
            return null;
        }
    }

    private static boolean isFailure(int status){
        return status / 100 != 2;
    }

    /**
     * Grabs the Instant that the Cache-Control indicates expiration
     *
     * @param response http response with Cache-Control header
     * @return Expire time of given response
     * @throws InternalServerException if unable to find the Cache-Control header
     */
    public static Instant getCacheTime(HttpResponse<String> response) throws InternalServerException {
        Optional<String> cache = response.headers().firstValue("Cache-Control");
        try{
            String seconds = cache.get().replace("max-age=", "");
            return Instant.now().plusSeconds(Long.parseLong(seconds));
        }
        catch (NoSuchElementException e) {
            throw new InternalServerException("Unable to determine cache time", e);
        }
    }
}
