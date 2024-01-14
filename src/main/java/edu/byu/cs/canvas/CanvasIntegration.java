package edu.byu.cs.canvas;

import com.google.gson.Gson;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ConfigProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CanvasIntegration {

    private static final String CANVAS_HOST = "https://byu.instructure.com";

    private static final String AUTHORIZATION_HEADER = ConfigProperties.canvasAuthorizationHeader();

    private static final int COURSE_NUMBER = 24410;

    private static final int GIT_REPO_ASSIGNMENT_NUMBER = 880442;


    public static User getUser(String netId) throws CanvasException {
        try {
            URL url = new URI(CANVAS_HOST + "/api/v1/courses/" + COURSE_NUMBER + "/search_users?search_term=" + netId +
                    "&include[]=enrollments").toURL();

            String resp = getResponse(url, "GET");

            CanvasUser[] users = new Gson().fromJson(resp, CanvasUser[].class);
            for (CanvasUser user : users) {
                if (user.login_id().equalsIgnoreCase(netId)) {
                    User.Role role;
                    if (user.enrollments.length == 0) role = null;
                    else role = switch (user.enrollments[0].type()) {
                        case StudentEnrollment -> User.Role.STUDENT;
                        case TeacherEnrollment, TaEnrollment -> User.Role.ADMIN;
                        case DesignerEnrollment, ObserverEnrollment ->
                                throw new CanvasException("Unsupported role: " + user.enrollments[0]);
                    };

                    String[] names = user.sortable_name().split(",");
                    String firstName = ((names.length >= 2) ? names[1] : "").trim();
                    String lastName = ((names.length >= 1) ? names[0] : "").trim();

                    String repoUrl = (role == User.Role.STUDENT) ? getGitRepo(user.id()) : null;

                    return new User(netId, user.id(), firstName, lastName, repoUrl, role);
                }
            }

            throw new CanvasException("Couldn't find user matching netID " + netId);

        } catch (IOException | URISyntaxException e) {
            throw new CanvasException("Exception while contacting canvas for getting user information", e);
        }
    }


    public static void submitGrade(int userId, int assignmentNum, float grade) throws CanvasException {
        try {
            URL url = new URI(CANVAS_HOST + "/api/v1/courses/" + COURSE_NUMBER + "/assignments/" + assignmentNum +
                    "/submissions/" + userId + "?submission[posted_grade]=" + grade).toURL();

            String resp = getResponse(url, "PUT");

        } catch (IOException | URISyntaxException e) {
            throw new CanvasException("Exception while contacting canvas for submitting grade", e);
        }
    }


    private static String getGitRepo(int userId) throws CanvasException {
        try {
            URL url = new URI(CANVAS_HOST + "/api/v1/courses/" + COURSE_NUMBER + "/assignments/" +
                    GIT_REPO_ASSIGNMENT_NUMBER + "/submissions/" + userId).toURL();

            String resp = getResponse(url, "GET");

            CanvasSubmission submission = new Gson().fromJson(resp, CanvasSubmission.class);
            if (submission == null) {
                throw new CanvasException("Error while accessing GitHub Repository assignment submission on canvas");
            }
            if (submission.url() == null) {
                throw new CanvasException(
                        "Please turn in the GitHub repository assignment on canvas before accessing the autograder");
            }
            return submission.url();

        } catch (IOException | URISyntaxException e) {
            throw new CanvasException("Exception while contacting canvas for getting git repo", e);
        }
    }


    private enum EnrollmentType {
        StudentEnrollment, TeacherEnrollment, TaEnrollment, DesignerEnrollment, ObserverEnrollment
    }

    private record Enrollment(EnrollmentType type) {
    }

    private record CanvasUser(int id, String sortable_name, String login_id, Enrollment[] enrollments) {
    }

    private record CanvasSubmission(String url) {
    }

    /**
     * Sends a request to canvas and returns the requested response
     *
     * @param method        The request method to use (e.g. "GET", "PUT", etc.)
     * @param path          The path to the endpoint to use (e.g. "/courses/12345")
     * @param request       The request body to send (or null if there is no request body)
     * @param responseClass The class of the response to return (or null if there is no response body)
     * @param <T>           The type of the response to return
     * @return The response from canvas
     * @throws CanvasException If there is an error while contacting canvas
     */
    private <T> T makeCanvasRequest(String method, String path, Object request, Class<T> responseClass) throws CanvasException {
        try {
            URL url = new URI(CANVAS_HOST + "/api/v1" + path).toURL();
            HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
            https.setRequestMethod(method);
            https.addRequestProperty("Accept", "*/*");
            https.addRequestProperty("Accept-Encoding", "deflate");
            https.addRequestProperty("Authorization", AUTHORIZATION_HEADER);

            if (method.equals("POST") || method.equals("PUT"))
                https.setDoOutput(true);

            if (request != null) {
                https.addRequestProperty("Content-Type", "application/json");
                String reqData = new Gson().toJson(request);
                try (OutputStream reqBody = https.getOutputStream()) {
                    reqBody.write(reqData.getBytes());
                }
            }

            https.connect();

            if (https.getResponseCode() % 100 != 2) {
                throw new CanvasException("Response from canvas wasn't 2xx, was " + https.getResponseCode());
            }

            return readBody(https, responseClass);
        } catch (Exception ex) {
            throw new CanvasException("Exception while contacting canvas", ex);
        }
    }

    /**
     * Reads the body of the response from canvas
     *
     * @param https         The connection to read the body from
     * @param responseClass The class of the response to return
     * @param <T>           The type of the response to return
     * @return The response from canvas
     * @throws IOException If there is an error reading the response from canvas
     */
    private static <T> T readBody(HttpsURLConnection https, Class<T> responseClass) throws IOException {
        T response = null;
        if (https.getContentLength() < 0) {
            try (InputStream respBody = https.getInputStream()) {
                InputStreamReader reader = new InputStreamReader(respBody);
                if (responseClass != null) {
                    response = new Gson().fromJson(reader, responseClass);
                }
            }
        }
        return response;
    }

}
