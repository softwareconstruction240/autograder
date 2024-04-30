package edu.byu.cs.canvas;

import edu.byu.cs.canvas.model.*;
import edu.byu.cs.controller.SubmissionController;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import org.eclipse.jgit.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.*;

public class CanvasIntegrationImpl implements CanvasIntegration {

    private static final String CANVAS_HOST = "https://byu.instructure.com";

    private static final String AUTHORIZATION_HEADER = ApplicationProperties.canvasAPIToken();

    // FIXME: set this dynamically or pull from config
    private static final int COURSE_NUMBER = 24410;

    // FIXME: set this dynamically or pull from config
    private static final int GIT_REPO_ASSIGNMENT_NUMBER = 880442;

    private record Enrollment(EnrollmentType type) { }

    private record CanvasUser(int id, String sortable_name, String login_id, Enrollment[] enrollments) {}

    private record CanvasSubmissionUser(String url, CanvasUser user) { }

    private record CanvasResponse<T>(T body, Map<String, List<String>> headers, int responseCode) {}

    @Override
    public User getUser(String netId) throws CanvasException {
        CanvasUser[] users = makeCanvasRequest(
                "GET",
                "/courses/" + COURSE_NUMBER + "/search_users?search_term=" + netId + "&include[]=enrollments",
                CanvasUser[].class).body();

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

                if (role == User.Role.STUDENT) {
                    try {
                        SubmissionController.getRemoteHeadHash(repoUrl);
                    } catch (RuntimeException e) {
                        throw new CanvasException("Invalid repo url. Please resubmit the GitHub Repository assignment on Canvas");
                    }
                }

                return new User(netId, user.id(), firstName, lastName, repoUrl, role);
            }
        }

        throw new CanvasException("User not found in Canvas: " + netId);
    }

    @Override
    public Collection<User> getAllStudents() throws CanvasException {
        return getMultipleStudents("/courses/" + COURSE_NUMBER + "/assignments/" +
                GIT_REPO_ASSIGNMENT_NUMBER + "/submissions?include[]=user");
    }

    @Override
    public Collection<User> getAllStudentsBySection(int sectionID) throws CanvasException {
        return getMultipleStudents("/sections/" + sectionID + "/assignments/" +
                GIT_REPO_ASSIGNMENT_NUMBER + "/submissions?include[]=user");
    }

    private static Collection<User> getMultipleStudents(String baseUrl) throws CanvasException {
        List<CanvasSubmissionUser> allSubmissions = makePaginatedCanvasRequest(baseUrl, CanvasSubmissionUser.class);

        Set<User> allStudents = new HashSet<>();
        for (CanvasSubmissionUser sub : allSubmissions) {
            if (sub.url == null) continue;
            CanvasUser user = sub.user;
            String[] names = user.sortable_name().split(",");
            String firstName = ((names.length >= 2) ? names[1] : "").trim();
            String lastName = ((names.length >= 1) ? names[0] : "").trim();
            allStudents.add(new User(user.login_id, user.id, firstName, lastName, sub.url, User.Role.STUDENT));
        }

        return allStudents;
    }

    /**
     * Submits the given grade for the given assignment for the given user
     *
     * @param userId        The canvas user id of the user to submit the grade for
     * @param assignmentNum The assignment number to submit the grade for
     * @param grade         The grade to submit (this is the total points earned, not a percentage)
     * @param comment       The comment to submit on the assignment
     * @throws CanvasException If there is an error with Canvas
     */
    @Override
    public void submitGrade(int userId, int assignmentNum, @Nullable Float grade, @Nullable String comment) throws CanvasException {
        if(grade == null && comment == null)
            throw new IllegalArgumentException("grade and comment should not both be null");
        StringBuilder path = new StringBuilder();
        path.append("/courses/").append(COURSE_NUMBER).append("/assignments/").append(assignmentNum)
                .append("/submissions/").append(userId).append("?");
        if(grade != null) path.append("submission[posted_grade]=").append(grade).append("&");
        if(comment != null) {
            String encodedComment = URLEncoder.encode(comment, Charset.defaultCharset());
            path.append("comment[text_comment]=").append(encodedComment);
        }
        else path.deleteCharAt(path.length() - 1);

        makeCanvasRequest(
                "PUT",
                path.toString(),
                null);
    }

    /**
     * Submits the given grade for the given assignment for the given user. Any grades or comments in the rubric not
     * included in the parameter maps are retrieved from the previous submission to prevent the loss of previous grades
     * and comments (The canvas API will set items not included to empty/black rather than grabbing the old data)
     *
     * @param userId            The canvas user id of the user to submit the grade for
     * @param assignmentNum     The assignment number to submit the grade for
     * @param assessment        Rubric assessment to put as the grade
     * @param assignmentComment A comment for the entire assignment, if necessary
     * @throws CanvasException If there is an error with Canvas
     */
    @Override
    public void submitGrade(int userId, int assignmentNum, CanvasRubricAssessment assessment, String assignmentComment) throws CanvasException {
        CanvasSubmission submission = getSubmission(userId, assignmentNum);
        if(submission.rubric_assessment() != null) {
            submission.rubric_assessment().items().putAll(assessment.items());
            assessment = submission.rubric_assessment();
        }
        String queryString = buildRubricSubmissionQueryString(assessment, assignmentComment);

        makeCanvasRequest(
                "PUT",
                "/courses/" + COURSE_NUMBER + "/assignments/" + assignmentNum + "/submissions/" + userId +
                        queryString,
                null);
    }

    private String buildRubricSubmissionQueryString(CanvasRubricAssessment assessment, String assignmentComment) {
        StringBuilder queryStringBuilder = new StringBuilder();
        for(Map.Entry<String, CanvasSubmissionRubricItem> entry : assessment.items().entrySet()) {
            queryStringBuilder.append("&rubric_assessment[").append(entry.getKey()).append("][points]=")
                    .append(entry.getValue().points());
            if(entry.getValue().comments() != null) {
                queryStringBuilder.append("&rubric_assessment[").append(entry.getKey()).append("][comments]=")
                        .append(URLEncoder.encode(entry.getValue().comments(), Charset.defaultCharset()));
            }
        }
        if(assignmentComment != null && !assignmentComment.isBlank()) {
            queryStringBuilder.append("&comment[text_comment]=")
                    .append(URLEncoder.encode(assignmentComment, Charset.defaultCharset()));
        }
        if(!queryStringBuilder.isEmpty() && queryStringBuilder.charAt(0) == '&') {
            queryStringBuilder.setCharAt(0, '?');
        }
        return queryStringBuilder.toString();
    }


    /**
     * Gets the submission details for a specific student's assignment
     *
     * @param userId            The canvas user id of the user to submit the grade for
     * @param assignmentNum     The assignment number to submit the grade for
     * @return                  Submission details for the assignment
     * @throws CanvasException  If there is an error with Canvas
     */
    @Override
    public CanvasSubmission getSubmission(int userId, int assignmentNum) throws CanvasException {
        return makeCanvasRequest(
                "GET",
                "/courses/" + COURSE_NUMBER + "/assignments/" + assignmentNum + "/submissions/" + userId + "?include[]=rubric_assessment",
                CanvasSubmission.class
        ).body();
    }

    /**
     * Gets the git repository url for the given user from their GitHub Repository assignment submission on canvas
     *
     * @param userId The canvas user id of the user to get the git repository url for
     * @return The git repository url for the given user
     * @throws CanvasException If there is an error with Canvas
     */
    @Override
    public String getGitRepo(int userId) throws CanvasException {
        CanvasSubmission submission = getSubmission(userId, GIT_REPO_ASSIGNMENT_NUMBER);

        if (submission == null)
            throw new CanvasException("Error while accessing GitHub Repository assignment submission on canvas");

        if (submission.url() == null)
            throw new CanvasException(
                    "The Github Repository assignment submission on Canvas must be submitted before accessing the autograder"
            );

        return submission.url();
    }

    @Override
    public User getTestStudent() throws CanvasException {
        return getTestStudent(COURSE_NUMBER);
    }

    public User getTestStudent(int courseNum) throws CanvasException {
        String testStudentName = "Test%20Student";

        CanvasUser[] users = makeCanvasRequest(
                "GET",
                "/courses/" + courseNum + "/search_users?search_term=" + testStudentName + "&include[]=test_student",
                CanvasUser[].class).body();

        if (users.length == 0)
            throw new CanvasException("Test Student not found in Canvas");

        String repoUrl = getGitRepo(users[0].id());

        if (repoUrl == null)
            throw new CanvasException("Test Student has not submitted the GitHub Repository assignment on Canvas");

        return new User(
                "test",
                users[0].id(),
                "Test",
                "Student",
                repoUrl,
                User.Role.STUDENT
        );
    }

    @Override
    public ZonedDateTime getAssignmentDueDateForStudent(int userId, int assignmentId) throws CanvasException {
        CanvasAssignment assignment = makeCanvasRequest(
                "GET",
                "/users/" + userId + "/courses/" + COURSE_NUMBER + "/assignments?assignment_ids[]=" + assignmentId,
                CanvasAssignment[].class
        ).body()[0];

        if (assignment == null || assignment.due_at() == null)
            throw new CanvasException("Unable to get due date for assignment");

        return assignment.due_at();
    }

    public List<CanvasAssignment> getAssignmentsForClass(int courseNum) throws CanvasException {
        int testStudentId = getTestStudent(courseNum).canvasUserId();
        String path = "/users/" + testStudentId + "/courses/" + courseNum + "/assignments";
        return makePaginatedCanvasRequest(path, CanvasAssignment.class);
    }

    @Override
    public CanvasSection[] getAllSections() throws CanvasException {
        return makeCanvasRequest("GET",
                "/courses/" + COURSE_NUMBER + "/sections",
                CanvasSection[].class).body();
    }

    private enum EnrollmentType {
        StudentEnrollment, TeacherEnrollment, TaEnrollment, DesignerEnrollment, ObserverEnrollment
    }

    /**
     * Sends a request to canvas and returns the requested response
     *
     * @param method        The request method to use (e.g. "GET", "PUT", etc.)
     * @param path          The path to the endpoint to use (e.g. "/courses/12345")
     * @param responseClass The class of the response to return (or null if there is no response body)
     * @param <T>           The type of the response to return
     * @return The response from canvas
     * @throws CanvasException If there is an error while contacting canvas
     */
    private static <T> CanvasResponse<T> makeCanvasRequest(String method, String path, Class<T> responseClass) throws CanvasException {
        try {
            URL url = new URI(CANVAS_HOST + "/api/v1" + path).toURL();
            HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
            https.setRequestMethod(method);
            https.addRequestProperty("Accept", "*/*");
            https.addRequestProperty("Accept-Encoding", "deflate");
            https.addRequestProperty("Authorization", AUTHORIZATION_HEADER);

            https.setDoOutput(false);

            https.connect();

            if (https.getResponseCode() < 200 || https.getResponseCode() >= 300) {
                throw new CanvasException("Response from canvas wasn't 2xx, was " + https.getResponseCode());
            }

            return new CanvasResponse<>(readBody(https, responseClass), https.getHeaderFields(), https.getResponseCode());
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
        if (https.getContentLength() < 0) {
            try (InputStream respBody = https.getInputStream()) {
                InputStreamReader reader = new InputStreamReader(respBody);
                if (responseClass != null) {
                    return new CanvasDeserializer<T>().deserialize(reader, responseClass);
                }
            }
        }
        return null;
    }

    /**
     * Sends a request to canvas and returns the requested response
     *
     * @param path          The path to the endpoint to use (e.g. "/courses/12345")
     * @param responseClass The class of the response to return
     * @param <T>           The type of the response to return
     * @return The list containing the responses from canvas
     * @throws CanvasException If there is an error while contacting canvas
     */
    private static <T> List<T> makePaginatedCanvasRequest(String path, Class<T> responseClass) throws CanvasException {
        List<T> list = new ArrayList<>();
        while (path != null) {
            @SuppressWarnings("unchecked")
            CanvasResponse<T[]> response = makeCanvasRequest("GET", path, (Class<T[]>) responseClass.arrayType());
            list.addAll(Arrays.asList(response.body()));
            List<String> links = response.headers().get("Link");
            path = (links != null) ? getNextPath(links.getFirst()) : null;
        }
        return list;
    }

    private static String getNextPath(String wholeLink) {
        for(String group : wholeLink.split(",")) {
            if (group.contains("rel=\"next\"")) {
                return group.substring(group.indexOf("/api/v1") + 7, group.indexOf('>'));
            }
        }
        return null;
    }

}
