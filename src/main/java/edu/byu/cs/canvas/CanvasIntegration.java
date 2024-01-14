package edu.byu.cs.canvas;

import com.google.gson.Gson;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ConfigProperties;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

public class CanvasIntegration {

    private static final String CANVAS_HOST = "https://byu.instructure.com";

    private static final String AUTHORIZATION_HEADER = ConfigProperties.canvasAuthorizationHeader();

    // FIXME: set this dynamically or pull from config
    private static final int COURSE_NUMBER = 24410;

    // FIXME: set this dynamically or pull from config
    private static final int GIT_REPO_ASSIGNMENT_NUMBER = 880442;


    /**
     * Queries canvas for the user with the given netId
     *
     * @param netId The netId of the user to query for
     * @return The user with the given netId, or null if no such user exists
     * @throws CanvasException If there is an error with Canvas' response
     */
    public static User getUser(String netId) throws CanvasException {
        CanvasUser[] users = makeCanvasRequest(
                "GET",
                "/courses/" + COURSE_NUMBER + "/search_users?search_term=" + netId + "&include[]=enrollments",
                null,
                CanvasUser[].class);

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

        return null;
    }

    /**
     * Submits the given grade for the given assignment for the given user
     *
     * @param userId        The canvas user id of the user to submit the grade for
     * @param assignmentNum The assignment number to submit the grade for
     * @param grade         The grade to submit (this is the total points earned, not a percentage)
     * @throws CanvasException If there is an error with Canvas
     */
    public static void submitGrade(int userId, int assignmentNum, float grade) throws CanvasException {
        makeCanvasRequest(
                "PUT",
                "/courses/" + COURSE_NUMBER + "/assignments/" + assignmentNum + "/submissions/" + userId + "?submission[posted_grade]=" + grade,
                null,
                null);
    }

    /**
     * Gets the git repository url for the given user from their GitHub Repository assignment submission on canvas
     *
     * @param userId The canvas user id of the user to get the git repository url for
     * @return The git repository url for the given user
     * @throws CanvasException If there is an error with Canvas
     */
    private static String getGitRepo(int userId) throws CanvasException {
        CanvasSubmission submission = makeCanvasRequest(
                "GET",
                "/assignments/" + GIT_REPO_ASSIGNMENT_NUMBER + "/submissions/" + userId,
                null,
                CanvasSubmission.class
        );

        if (submission == null)
            throw new CanvasException("Error while accessing GitHub Repository assignment submission on canvas");

        if (submission.url() == null)
            throw new CanvasException(
                    "Please turn in the GitHub repository assignment on canvas before accessing the autograder");

        return submission.url();
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
    private static <T> T makeCanvasRequest(String method, String path, Object request, Class<T> responseClass) throws CanvasException {
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
