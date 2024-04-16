package integration;

import edu.byu.cs.canvas.CanvasException;
import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.CanvasIntegrationImpl;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.memory.QueueMemoryDao;
import edu.byu.cs.dataAccess.memory.RubricConfigMemoryDao;
import edu.byu.cs.dataAccess.memory.SubmissionMemoryDao;
import edu.byu.cs.dataAccess.memory.UserMemoryDao;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class CanvasIntegrationImplIT {

    @BeforeAll
    public static void setUp() {
        loadApplicationProperties();
    }

    @BeforeEach
    public void setUpEach() {
        DaoService.setRubricConfigDao(new RubricConfigMemoryDao());
        DaoService.setUserDao(new UserMemoryDao());
        DaoService.setQueueDao(new QueueMemoryDao());
        DaoService.setSubmissionDao(new SubmissionMemoryDao());
    }

    @Test
    @DisplayName("Test Student can be retrieved")
    public void getTestStudent() {
        CanvasIntegration canvasIntegration = new CanvasIntegrationImpl();
        User testStudent = null;
        try {
            testStudent = canvasIntegration.getTestStudent();
        } catch (CanvasException e) {
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
