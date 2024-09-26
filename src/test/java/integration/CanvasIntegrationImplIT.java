package integration;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasIntegrationImpl;
import edu.byu.cs.canvas.model.CanvasAssignment;
import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.RubricConfigDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import edu.byu.cs.util.PhaseUtils;
import org.junit.jupiter.api.*;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class CanvasIntegrationImplIT {

    private CanvasIntegration canvasIntegration;
    private CanvasIntegrationImpl.CourseInfoRetriever retriever;

    @BeforeAll
    public static void setUp() {
        loadApplicationProperties();
    }

    @BeforeEach
    public void setUpEach() throws DataAccessException {
        DaoService.initializeMemoryDAOs();
        DaoService.getConfigurationDao().setConfiguration(
                ConfigurationDao.Configuration.COURSE_NUMBER,
                26822,
                Integer.class
        ); // FIXME: ??? Maybe get Course Number dynamically

        canvasIntegration = new CanvasIntegrationImpl();
        retriever = new CanvasIntegrationImpl.CourseInfoRetriever();
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
