package edu.byu.cs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NetworkUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkUtils.class);

    /**
     * Leverages the built-in {@link java.net.http.HttpClient} library to make a basic HTTP Get request.
     *
     * @param url The URL to request
     * @return The {@link HttpResponse<String>} response, or the errors generated in the process.
     */
    public static HttpResponse<String> makeGetRequest(String url) throws IOException, InterruptedException {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET() // HTTP GET method
                    .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    /**
     * Makes an HTTP Get request and returns the body text, or null if an error occurs.
     * @param url The URL to request.
     * @return A string or null.
     */
    public static String readGetRequestBody(String url) {
        try {
            HttpResponse<String> response = makeGetRequest(url);
            return response.body();
        } catch (IOException | InterruptedException e) {
            System.err.print("Error making GET request to '" + url + "': " + e.getMessage());
            LOGGER.warn("Exception thrown while making GET request to '" + url + "': ", e);
            return null;
        }
    }
}
