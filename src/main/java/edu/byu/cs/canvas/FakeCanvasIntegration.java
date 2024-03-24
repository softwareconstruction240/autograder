package edu.byu.cs.canvas;

import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.model.User;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class FakeCanvasIntegration implements CanvasIntegration{
    @Override
    public User getUser(String netId) throws CanvasException {
        User user = DaoService.getUserDao().getUser(netId);
        if(user == null) {
            user = new User(netId, 0, "FirstName", "LastName", null, User.Role.ADMIN);
        }
        return user;
    }

    @Override
    public Collection<User> getAllStudents() throws CanvasException {
        return new HashSet<>();
    }

    @Override
    public Collection<User> getAllStudentsBySection(int sectionID) throws CanvasException {
        return new HashSet<>();
    }

    @Override
    public void submitGrade(int userId, int assignmentNum, Float grade, String comment) throws CanvasException {

    }

    @Override
    public void submitGrade(int userId, int assignmentNum, Map<String, Float> grades,
                            Map<String, String> rubricComments, String assignmentComment) throws CanvasException {

    }

    @Override
    public CanvasSubmission getSubmission(int userId, int assignmentNum) throws CanvasException {
        return null;
    }

    @Override
    public String getGitRepo(int userId) throws CanvasException {
        return null;
    }

    @Override
    public User getTestStudent() throws CanvasException {
        return null;
    }

    @Override
    public ZonedDateTime getAssignmentDueDateForStudent(int userId, int assignmentId) throws CanvasException {
        return null;
    }
}
