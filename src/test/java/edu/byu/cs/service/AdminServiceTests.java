package edu.byu.cs.service;

import edu.byu.cs.canvas.CanvasIntegration;
import edu.byu.cs.canvas.model.CanvasSection;
import edu.byu.cs.dataAccess.daoInterface.SubmissionDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminServiceTests {
    private UserDao userDao;
    private SubmissionDao submissionDao;
    private CanvasIntegration canvasIntegration;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        submissionDao = mock(SubmissionDao.class);
        canvasIntegration = mock(CanvasIntegration.class);
        adminService = new AdminService(userDao, submissionDao, canvasIntegration);
    }

    @Test
    void getAllSections_usesInjectedCanvasIntegration() throws Exception {
        CanvasSection[] expected = {new CanvasSection(17, "Section", java.util.Set.of())};
        when(canvasIntegration.getAllSections()).thenReturn(expected);

        assertSame(expected, adminService.getAllSections());
    }
}
