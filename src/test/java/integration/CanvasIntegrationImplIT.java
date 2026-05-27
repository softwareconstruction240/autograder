package integration;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasIntegrationImpl;
import edu.byu.cs.canvas.CanvasUtils;
import edu.byu.cs.canvas.model.CanvasAssignment;
import edu.byu.cs.canvas.model.CanvasRubricAssessment;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.canvas.model.CanvasSubmission;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.RubricConfigDao;
import edu.byu.cs.model.*;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.PhaseUtils;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CanvasIntegrationImplIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CanvasIntegrationImplIT.class);

    private CanvasIntegration canvasIntegration;
    private CanvasIntegrationImpl.CourseInfoRetriever retriever;

    @BeforeAll
    public static void setUp() {
        loadApplicationProperties();
    }

    @BeforeEach
    public void setUpEach() throws DataAccessException {
        DaoService.initializeMemoryDAOs();
        DaoService.getConfigurationDao().setConfiguration(ConfigurationDao.Configuration.COURSE_NUMBER, 33692, Integer.class);
        //TODO: get the course number dynamically, esp. when running on GitHub Actions. Needs to work locally, in GH Actions, and in deployment.
        // possible solutions:
        // 1. query the live autogrder's MySQL Config DAO for the course ID (this would require a lot of workarounds)
        // 2. query the API at cs240.click/api/admin/config (needs OAuth to work)
        // 3. make a new public autograder endpoint to get the course ID specifically (this would be pretty easy, but should we expose the course ID publicly?)
        // 4. keep manually updating it either here or as an environment variable in GH Actions
        canvasIntegration = new CanvasIntegrationImpl();
        retriever = new CanvasIntegrationImpl.CourseInfoRetriever();
    }

    @Test
    @DisplayName("Can get a user by Net ID")
    public void getUserByNetID() {
        User randomStudent = null;
        try {
            randomStudent = retriever.getRandomEnrolledStudent();
            canvasIntegration.getUser(randomStudent.netId());
        } catch (CanvasException e) {
            LOGGER.error("Could not get a user from Canvas: {}", e.getMessage());
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
            int sectionID = retriever.getSectionIDFromCanvas();
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
        try {
            User testStudent = canvasIntegration.getTestStudent();
            for(int phaseID : retriever.getAssignmentIds().values()) {
                float expectedScore = new Random().nextFloat(155.0f);
                canvasIntegration.submitGrade(testStudent.canvasUserId(), phaseID, expectedScore, "Canvas Integration Tests");
                float actualScore = canvasIntegration.getSubmission(testStudent.canvasUserId(), phaseID).score();
                Assertions.assertEquals(expectedScore, actualScore);

            }

        } catch (CanvasException e) {
            LOGGER.error("Could not submit a grade: {}", e.getMessage());
            fail("Exception thrown: ", e);
        }
    }

    @Test
    @DisplayName("Can submit a grade by rubric")
    public void submitGradeRubric() {
        try {
            retriever.useCourseRelatedInfoFromCanvas();
            User testStudent = canvasIntegration.getTestStudent();
            Map<Phase, Integer> assignmentIDs = retriever.getAssignmentIds();
            for(Phase phase : Phase.values()) {
                if(phase == Phase.Quality || phase == Phase.GitHub)
                    continue;
                int phaseID = assignmentIDs.get(phase);
                float expectedScore = new Random().nextFloat(10.0f);
                CanvasRubricAssessment rubricAssessment = spoofRubricAssessment(expectedScore, phase);
                canvasIntegration.submitGrade(testStudent.canvasUserId(), phaseID, rubricAssessment, "Canvas Integration Tests");

                float actualScore = canvasIntegration.getSubmission(testStudent.canvasUserId(), phaseID).score();
                Assertions.assertEquals(Math.round(expectedScore), Math.round(actualScore));
            }
        } catch (CanvasException | GradingException | DataAccessException e) {
            LOGGER.error("Could not submit a grade: {}", e.getMessage());
            fail("Exception thrown: ", e);
        }
    }

    CanvasRubricAssessment spoofRubricAssessment(float expectedScore, Phase phase) throws DataAccessException, GradingException {
        RubricConfigDao rubricConfigDao = DaoService.getRubricConfigDao();
        RubricConfig rubricConfig = rubricConfigDao.getRubricConfig(phase);
        RubricConfig.RubricConfigItem configItem = rubricConfig.items().get(Rubric.RubricType.GIT_COMMITS);

        EnumMap<Rubric.RubricType, Rubric.RubricItem> rubricItems = new EnumMap<>(Rubric.RubricType.class);
        rubricItems.put(Rubric.RubricType.GIT_COMMITS,
                new Rubric.RubricItem(configItem.category(),
                        new Rubric.Results(
                                "Canvas Integration Tests",
                                expectedScore,
                                configItem.points(),
                                new TestOutput(new TestNode(), null, null),
                                "text results"),
                        "test criteria"
                )
        );
        Rubric rubric = new Rubric(rubricItems, true, "test notes");
        return CanvasUtils.convertToAssessment(rubric, rubricConfig, phase);
    }

    @Test
    @DisplayName("Can get an assignment submission from a student")
    public void getAssignmentSubmission() {
        try {
            User testStudent = canvasIntegration.getTestStudent();
            for(Phase phase: Phase.values()) {
                if(phase == Phase.Quality || phase == Phase.GitHub)
                    continue;
                int phaseID = retriever.getAssignmentIds().get(phase);
                CanvasSubmission submission = canvasIntegration.getSubmission(testStudent.canvasUserId(), phaseID);
                Assertions.assertNotNull(submission);
            }
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
            for(Phase phase: Phase.values()) {
                if(phase == Phase.Quality || phase == Phase.GitHub)
                    continue;
                int phaseID = retriever.getAssignmentIds().get(phase);
                ZonedDateTime dueDate = canvasIntegration.getAssignmentDueDateForStudent(testStudent.canvasUserId(), phaseID);
                Assertions.assertNotNull(dueDate);
            }
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
