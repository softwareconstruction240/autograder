package integration;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasIntegrationImpl;
import edu.byu.cs.canvas.CanvasUtils;
import edu.byu.cs.canvas.model.CanvasAssignment;
import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasRubricItem;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.PhaseUtils;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CanvasIntegrationImplIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanvasIntegrationImplIT.class);

    private CanvasIntegration canvasIntegration;
    private CanvasIntegrationImpl.CourseInfoRetriever retriever;
    private static int courseID;

    @BeforeAll
    public static void setUp() throws CanvasException {
        loadApplicationProperties();
        courseID = new CanvasIntegrationImpl.CourseInfoRetriever().getCurrentCourseIDFromCanvas();
    }

    @BeforeEach
    public void setUpEach() throws DataAccessException {
        DaoService.initializeMemoryDAOs();
        DaoService.getConfigurationDao().setConfiguration(
                ConfigurationDao.Configuration.COURSE_NUMBER,
                courseID,
                Integer.class
        ); // FIXME: ??? Maybe get Course Number dynamically
        // TODO: this can be easily fixed. Use the Autograder user's API token to get all courses, then grab the most recent one.
            // actually, we'll need to get the one with the next end date. There are future courses in Canvas that haven't started yet.
        canvasIntegration = new CanvasIntegrationImpl();
        retriever = new CanvasIntegrationImpl.CourseInfoRetriever();
    }
    // TODO: should test the following:
    // 1) can get user by net id?
    // 2) can get all net ids in a section?
    // 3) can submit a grade (both overloads)
    // 4) can get a submission for a specific student and assignment?
    // 5) can get the test student?
    // 6) can get an assignment's due date for a particular student?
    // 7) can get all sections in class?
    // 8) and all of these CourseInfoRetriever test cases:
        // a) getCanvasAssignments
        // b) getAssignmentIds
        // c) getRubricInfo
        // d) useCourseRelatedInfoFromCanvas
        // e) loadCourseRelatedItems
    // actually CourseInfoRetriever shouldn't be tested since it doesn't exist in the fake Canvas Integration
    // should use logger.error() to notify that something is wrong if the API changes
    @Test
    @DisplayName("Can get the course number of the current course")
    public void getMostRecentCourseNumber() {
        int courseID;
        try {
            courseID = retriever.getCurrentCourseIDFromCanvas();
        } catch (CanvasException e) {
            LOGGER.error("Could not get current course id from canvas: {}", e.getMessage());
            fail("Exception thrown: ", e);
        }
    }
    @Test
    @DisplayName("Can get a user by Net ID")
    public void getUserByNetID() {
        // throw new RuntimeException("Cannot implement until a Test User is created");
        User randomStudent = null;
        try {
            randomStudent = retriever.getRandomEnrolledStudent(courseID);
        } catch (CanvasException e) {
            LOGGER.error("Could not get test user from Canvas: {}", e.getMessage());
            fail("Exception thrown: ", e);
        }
        assertNotNull(randomStudent, "Student should not be null. Ensure the current course has at least one user.");
        assertNotEquals(0, randomStudent.canvasUserId(), "Student's user ID should be non-zero");
    }

    @Test
    @DisplayName("Can get all net IDs in a section")
    public void getNetIdsFromSection() {
        Collection<String> netids = null;
        try {
            int sectionID = retriever.getSectionIDFromCanvas(courseID);
            netids = canvasIntegration.getAllStudentNetIdsBySection(sectionID);
        } catch (CanvasException e) {
            LOGGER.error("Could not get Net IDs from current section: {}", e.getMessage());
            fail("Exception thrown: ", e);
        }
        assertNotNull(netids);
    }

    @Test
    @DisplayName("Can submit a grade by number")
    public void submitGradeNumber() {
//        throw new RuntimeException("Not implemented");
        try {
            User testStudent = canvasIntegration.getTestStudent();
            int phase0ID = retriever.getPhase0IDFromCanvas(courseID);

            float expectedScore = new Random().nextFloat(125.0f);

            canvasIntegration.submitGrade(testStudent.canvasUserId(), phase0ID, expectedScore, "Canvas Integration Tests");
            float actualScore = canvasIntegration.getSubmission(testStudent.canvasUserId(), phase0ID).score();
            Assertions.assertEquals(expectedScore, actualScore);
        } catch (CanvasException e) {
            LOGGER.error("Could not submit a grade: {}", e.getMessage());
            fail("Exception thrown: ", e);
        }
    }

    @Test
    @DisplayName("Can submit a grade by rubric")
    public void submitGradeRubric() {
        throw new RuntimeException("Not implemented");
//        try {
//            User testStudent = canvasIntegration.getTestStudent();
//            int phase0ID = retriever.getPhase0IDFromCanvas(courseID);
//
//            float expectedScore = new Random().nextFloat(125.0f);
//            canvasIntegration.submitGrade(testStudent.canvasUserId(), phase0ID, assessment, "Canvas Integration Tests");
//            float actualScore = canvasIntegration.getSubmission(testStudent.canvasUserId(), phase0ID).score();
//            Assertions.assertEquals(expectedScore, actualScore);
//        } catch (CanvasException e) {
//            LOGGER.error("Could not submit a grade: {}", e.getMessage());
//            fail("Exception thrown: ", e);
//        }
    }

    @Test
    @DisplayName("Can get an assignment submission from a student")
    public void getAssignmentSubmission() {
        try {
            User testStudent = canvasIntegration.getTestStudent();
            int phase0ID = retriever.getPhase0IDFromCanvas(courseID);
            canvasIntegration.getSubmission(testStudent.canvasUserId(), phase0ID);
        } catch (CanvasException e) {
            LOGGER.error("Could not get assignment submission from a student: {}", e.getMessage());
            fail("Exception thrown: ", e);
        }
    }

    @Test
    @DisplayName("Can get the due date based on assignment and student")
    public void getDueDate() {
        try {
            User testStudent = canvasIntegration.getTestStudent();
            int phase0ID = retriever.getPhase0IDFromCanvas(courseID);
            canvasIntegration.getAssignmentDueDateForStudent(testStudent.canvasUserId(), phase0ID);
        } catch (CanvasException e) {
            LOGGER.error("Could not get Phase 0 Due Date for test student: {}", e.getMessage());
            fail("Exception thrown: ", e);
        }

    }

    @Test
    @DisplayName("Can get all sections in a class")
    public void getAllSectionsInClass() {
        CanvasSection[] sections = null;
        try {
            sections = canvasIntegration.getAllSections();
        } catch (CanvasException e) {
            LOGGER.error("Could not get all sections from class");
            fail("Exception thrown: ", e);
        }
        assertNotNull(sections);
    }

    @Test
    @Order(1)
    @DisplayName("Auto-graded Canvas Assignments can be Retrieved")
    public void getCanvasAssignments() {
        Collection<CanvasAssignment> canvasAssignments = null;
        try {
            canvasAssignments = retriever.getCanvasAssignments();
        } catch (CanvasException e) {
            fail("Unexpected exception thrown: ", e);
        }

        assertEquals(
                CanvasIntegrationImpl.CourseInfoRetriever.CANVAS_AUTO_GRADED_ASSIGNMENT_NAMES.size(),
                canvasAssignments.size()
        );
    }

    @Test
    @Order(2)
    @DisplayName("Assignment Ids and Rubric Ids, Description Points from Canvas are Not Null")
    void getRubricInfo() {
        Map<Phase, Integer> assignmentIds = null;
        Map<Phase, Map<Rubric.RubricType, CanvasAssignment.CanvasRubric>> rubricInfo = null;
        try {
            assignmentIds = retriever.getAssignmentIds();
            rubricInfo = retriever.getRubricInfo();
        } catch (CanvasException e) {
            fail("Unexpected exception thrown: ", e);
        }

        for (Phase phase : Phase.values()) {
            if (PhaseUtils.isPhaseGraded(phase)) {
                assertNotNull(assignmentIds.get(phase));
                assertNotNull(rubricInfo.get(phase));
                for (Rubric.RubricType type : PhaseUtils.getRubricTypesFromPhase(phase)) {
                    CanvasAssignment.CanvasRubric rubric = rubricInfo.get(phase).get(type);
                    assertNotNull(rubric.id());
                    assertNotNull(rubric.description());
                    assertNotNull(rubric.points());
                }
            }
        }

    }

    @Test
    @DisplayName("Course Related Information From Canvas is Stored in the Database")
    @Order(3)
    public void storeCourseInfoInDb() {
        ConfigurationDao configurationDao = DaoService.getConfigurationDao();
        RubricConfigDao rubricConfigDao = DaoService.getRubricConfigDao();

        try {
            retriever.useCourseRelatedInfoFromCanvas();
            Map<Phase, Integer> canvasAssignments = retriever.getAssignmentIds();
            Map<Phase, Map<Rubric.RubricType, CanvasAssignment.CanvasRubric>> rubricInfo = retriever.getRubricInfo();

            for (Phase phase : Phase.values()) {
                if (PhaseUtils.isPhaseGraded(phase)) {
                    Integer actualId = configurationDao.getConfiguration(
                            PhaseUtils.getConfigurationAssignmentNumber(phase),
                            Integer.class
                    );
                    assertEquals(canvasAssignments.get(phase), actualId);
                    RubricConfig rubricConfig = rubricConfigDao.getRubricConfig(phase);
                    for (Rubric.RubricType type : PhaseUtils.getRubricTypesFromPhase(phase)) {
                        RubricConfig.RubricConfigItem configItem = rubricConfig.items().get(type);

                        String referenceId = rubricInfo.get(phase).get(type).id();
                        Integer referencePoints = rubricInfo.get(phase).get(type).points();

                        assertEquals(referenceId, configItem.rubric_id());
                        assertEquals(referencePoints, configItem.points());
                    }
                }
            }
        } catch (Exception e) {
            fail("Unexpected exception thrown: ", e);
        }

    }

    @Test
    @DisplayName("Test Student can be retrieved")
    @Order(4)
    public void getTestStudent() {
        User testStudent = null;
        try {
            retriever.useCourseRelatedInfoFromCanvas();
            testStudent = canvasIntegration.getTestStudent();
        } catch (CanvasException | DataAccessException e) {
            fail("Unexpected exception thrown: ", e);
        }

        assertNotNull(testStudent, "Test student should not be null");
        assertNotEquals(0, testStudent.canvasUserId(), "Test student canvas id should not be null");

    }

    private static void loadApplicationProperties() {

        String canvasToken = System.getenv("CANVAS_API_TOKEN");
        if (canvasToken == null) {
            throw new RuntimeException("Environment variable CANVAS_API_TOKEN not set");
        }

        Properties testProperties = new Properties();
        testProperties.setProperty("db-host", "unused");
        testProperties.setProperty("db-port", "unused");
        testProperties.setProperty("db-name", "unused");
        testProperties.setProperty("db-user", "unused");
        testProperties.setProperty("db-pass", "unused");
        testProperties.setProperty("frontend-url", "unused");
        testProperties.setProperty("cas-callback-url", "unused");
        testProperties.setProperty("canvas-token", canvasToken);
        testProperties.setProperty("use-canvas", "true");

        ApplicationProperties.loadProperties(testProperties);
    }
}
