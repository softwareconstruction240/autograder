package edu.byu.cs.canvas;

import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.model.User;
import org.eclipse.jgit.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CanvasIntegration {

    /**
     * Queries canvas for the user with the given netId
     *
     * @param netId The netId of the user to query for
     * @return The user with the given netId, or null if no such user exists
     * @throws CanvasException If there is an error with Canvas' response
     */
    User getUser(String netId) throws CanvasException;

    /**
     * Queries Canvas for every student with a Git repo URL submission
     *
     * @return A set of user objects
     * @throws CanvasException If there is an error with Canvas' response
     */
    Collection<User> getAllStudents() throws CanvasException;

    Collection<User> getAllStudentsBySection(int sectionID) throws CanvasException;


    /**
     * Submits the given grade for the given assignment for the given user
     *
     * @param userId        The canvas user id of the user to submit the grade for
     * @param assignmentNum The assignment number to submit the grade for
     * @param grade         The grade to submit (this is the total points earned, not a percentage)
     * @param comment       The comment to submit on the assignment
     * @throws CanvasException If there is an error with Canvas
     */
    void submitGrade(int userId, int assignmentNum, @Nullable Float grade, @Nullable String comment)
            throws CanvasException;

    /**
     * Submits the given grade for the given assignment for the given user. Any grades or comments in the rubric not
     * included in the parameter maps are retrieved from the previous submission to prevent the loss of previous grades
     * and comments (The canvas API will set items not included to empty/black rather than grabbing the old data)
     *
     * @param userId            The canvas user id of the user to submit the grade for
     * @param assignmentNum     The assignment number to submit the grade for
     * @param assessment        The rubric assessment to grade with
     * @param assignmentComment A comment for the entire assignment, if necessary
     * @throws CanvasException If there is an error with Canvas
     * @requires The maps passed in must support the putIfAbsent method (Map.of() does not)
     */
    void submitGrade(int userId, int assignmentNum, CanvasRubricAssessment assessment, String assignmentComment)
            throws CanvasException;


    /**
     * Gets the submission details for a specific student's assignment
     *
     * @param userId        The canvas user id of the user to submit the grade for
     * @param assignmentNum The assignment number to submit the grade for
     * @return Submission details for the assignment
     * @throws CanvasException If there is an error with Canvas
     */
    CanvasSubmission getSubmission(int userId, int assignmentNum) throws CanvasException;

    /**
     * Gets the git repository url for the given user from their GitHub Repository assignment submission on canvas
     *
     * @param userId The canvas user id of the user to get the git repository url for
     * @return The git repository url for the given user
     * @throws CanvasException If there is an error with Canvas
     */
    String getGitRepo(int userId) throws CanvasException;


    User getTestStudent() throws CanvasException;

    ZonedDateTime getAssignmentDueDateForStudent(int userId, int assignmentId) throws CanvasException;

    CanvasSection[] getAllSections() throws CanvasException;

}
