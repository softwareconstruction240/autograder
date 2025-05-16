package edu.byu.cs.service;

import edu.byu.cs.analytics.CommitAnalyticsRouter;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasService;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.controller.exception.ResourceNotFoundException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.ItemNotFoundException;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.honorChecker.HonorCheckerCompiler;
import edu.byu.cs.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

/**
 * Contains service logic for the {@link edu.byu.cs.controller.AdminController}.
 * <br><br>
 * The {@code AdminService} provides the following features:
 * <ul>
 *     <li>Getting a collection of users</li>
 *     <li>Updating information for a user</li>
 *     <li>Update and get the test student</li>
 *     <li>Get the commit analytics. See {@link edu.byu.cs.analytics.CommitAnalytics}
 *     for more information on commit analytics</li>
 *     <li>Compile and get a .zip file of students' code in a section for the Honor Checker</li>
 *     <li>Gets all sections and all students in each section</li>
 * </ul>
 */
public class AdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);

    /**
     * Gets all users stored in database of the AutoGrader
     *
     * @return the collection of users
     * @throws DataAccessException if there was an issue accessing the database
     */
    public static Collection<User> getUsers() throws DataAccessException {
        UserDao userDao = DaoService.getUserDao();

        Collection<User> users;
        try {
            users = userDao.getUsers();
        } catch (DataAccessException e) {
            LOGGER.error("Error getting users", e);
            throw e;
        }

        return users;
    }

    /**
     * Update the information (first name, last name, repo URL, role) of a user via their netId.
     *
     * @param user the updated user
     * @throws DataAccessException if there was an issue accessing the database
     * @throws ResourceNotFoundException if the user could not be found in the database
     */
    public static void updateUser(User user) throws DataAccessException, ResourceNotFoundException {
        UserDao userDao = DaoService.getUserDao();
        User existingUser;
        try {
            existingUser = userDao.getUser(user.netId());
        } catch (DataAccessException e) {
            LOGGER.error("Error getting user", e);
            throw e;
        }

        if (existingUser == null) {
            ResourceNotFoundException e = new ResourceNotFoundException("user not found");
            LOGGER.error("user not found", e);
            throw e;
        }

        try {
            if (user.firstName() != null) {
                userDao.setFirstName(user.netId(), user.firstName());
            }

            if (user.lastName() != null) {
                userDao.setLastName(user.netId(), user.lastName());
            }

            if (user.repoUrl() != null) {
                userDao.setRepoUrl(user.netId(), user.repoUrl());
            }

            if (user.role() != null) {
                userDao.setRole(user.netId(), user.role());
            }
        } catch (DataAccessException e) {
            LOGGER.error("Error updating user", e);
            throw e;
        }

    }

    /**
     * Get the test student from Canvas, update the test student information in the
     * database, then return the test student
     *
     * @return the test student
     * @throws CanvasException if there was an issue getting the test student from Canvas
     * @throws DataAccessException if there was an issue accessing the database
     */
    public static User updateTestStudent() throws CanvasException, DataAccessException {
        User latestTestStudent;
        try {
            latestTestStudent = CanvasService.getCanvasIntegration().getTestStudent();
        } catch (CanvasException e) {
            LOGGER.error("Error getting test student", e);
            throw e;
        }

        UserDao userDao = DaoService.getUserDao();
        User user;
        try {
            user = userDao.getUser("test");
        } catch (DataAccessException e) {
            LOGGER.error("Error getting user", e);
            throw e;
        }

        try {

            if (user == null) {
                user = latestTestStudent;
                userDao.insertUser(latestTestStudent);
            } else {
                userDao.setRepoUrl(user.netId(), latestTestStudent.repoUrl());
                userDao.setCanvasUserId(user.netId(), latestTestStudent.canvasUserId());
            }

        } catch (DataAccessException e) {
            LOGGER.error("Error updating user", e);
            throw e;
        }

        try {
            DaoService.getSubmissionDao().removeSubmissionsByNetId(user.netId(), 0);
        } catch (DataAccessException e) {
            LOGGER.error("Error removing submissions", e);
            throw e;
        }

        return user;

    }

    /**
     * Gets the commit analytics
     *
     * @param option a string indicating what option to choose from to return (1) updating
     *               and getting the csv file commit data, (2) getting the most recently
     *               cached commit data, and (3) getting the timestamp of the most recently
     *               cached commit data
     * @return the commit data, or the timestamp of the most recently cached commit data
     * @throws CanvasException if there was an issue getting students from Canvas
     * @throws DataAccessException if there was an issue getting students in the database
     */
    public static String getCommitAnalytics(String option) throws CanvasException, DataAccessException {
        return switch (option) {
            case "update" -> CommitAnalyticsRouter.update();
            case "cached" -> CommitAnalyticsRouter.cached();
            case "when" -> CommitAnalyticsRouter.when();
            default -> throw new IllegalStateException("Not found (invalid option: " + option + ")");
        };
    }

    /**
     * Compile and return a .zip file of students' code in a section for the Honor Checker
     *
     * @param sectionStr the section of student's to create a .zip file for
     * @param os where to output the data of the .zip file
     * @throws CanvasException if there was an issue getting the Canvas section
     * @throws IOException if there was an issue either reading from the .zip file or
     * writing to the output
     * @throws DataAccessException if there was an issue getting a student in the database
     */
    public static void streamHonorCheckerZip(String sectionStr, OutputStream os) throws CanvasException, IOException, DataAccessException {
        String filePath = HonorCheckerCompiler.compileSection(Integer.parseInt(sectionStr));

        try (FileInputStream fis = new FileInputStream(filePath)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            fis.close();
            new File(filePath).delete();
        }

    }

    /**
     * Gets all sections and the students in those sections
     *
     * @return an array of {@link CanvasSection}
     * @throws CanvasException if there was an issue getting the sections in Canvas
     */
    public static CanvasSection[] getAllSections() throws CanvasException {
        return CanvasService.getCanvasIntegration().getAllSections();
    }

}
