package edu.byu.cs.canvas;

import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.model.User;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;

/**
 * A fake {@link CanvasIntegration} if using Canvas is disabled
 */
public class FakeCanvasIntegration implements CanvasIntegration {

    private final UserDao userDao;

    public FakeCanvasIntegration(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public void updateCourseRelatedInfo(RubricConfigDao rubricConfigDao) throws CanvasException {
        throw new CanvasException("Cannot update course metadata while Canvas integration is disabled");
    }

    private static final User TEST_USER = new User(
            "test",
            159732,
            "Test",
            "Student",
            null,
            User.Role.STUDENT
    );

    @Override
    public User getUser(String netId) throws CanvasException {
        User user = null;
        try {
            user = userDao.getUser(netId);
        } catch (DataAccessException e) {
            throw new CanvasException("Error getting user from database", e);
        }
        if(user == null) {
            user = new User(netId, 0, "FirstName", "LastName", null, User.Role.ADMIN);
        }
        return user;
    }


    @Override
    public Collection<String> getAllStudentNetIdsBySection(int sectionID) {
        return new HashSet<>();
    }

    @Override
    public void submitGrade(int userId, int assignmentNum, Float grade, String comment) {

    }

    @Override
    public void submitGrade(int userId, int assignmentNum, CanvasRubricAssessment assessment, String assignmentComment) {

    }


    @Override
    public CanvasSubmission getSubmission(int userId, int assignmentNum) {
        return null;
    }

    @Override
    public User getTestStudent() {
        return TEST_USER;
    }

    @Override
    public ZonedDateTime getAssignmentDueDateForStudent(int userId, int assignmentId) {
        return null;
    }

    @Override
    public CanvasSection[] getAllSections() {
        return new CanvasSection[]{new CanvasSection(0, "Fake Section", new HashSet<>())};
    }
}
