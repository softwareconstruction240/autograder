package edu.byu.cs.service;

import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.daoInterface.QueueDao;
import edu.byu.cs.dataAccess.daoInterface.SubmissionDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubmissionServiceTests {
    private QueueDao queueDao;
    private SubmissionService submissionService;

    @BeforeEach
    void setUp() {
        queueDao = mock(QueueDao.class);
        submissionService = new SubmissionService(
                queueDao,
                mock(SubmissionDao.class),
                mock(UserDao.class),
                mock(ConfigurationDao.class),
                mock(ConfigService.class),
                mock(CanvasIntegration.class));
    }

    @Test
    void isAlreadyInQueue_usesInjectedQueueDao() throws Exception {
        when(queueDao.isAlreadyInQueue("student")).thenReturn(true);

        assertTrue(submissionService.isAlreadyInQueue("student"));
    }
}
