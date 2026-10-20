package edu.byu.cs.service;

import edu.byu.cs.controller.exception.BadRequestException;
import edu.byu.cs.controller.exception.InternalServerException;
import edu.byu.cs.controller.exception.UnprocessableEntityException;
import edu.byu.cs.controller.exception.WordOfWisdomViolationException;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.daoInterface.RepoUpdateDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.model.RepoUpdate;
import edu.byu.cs.util.RepoUrlValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTests {

    private static final String INPUT_URL = "https://github.com/student/repo.git";
    private static final String CLEAN_URL = "https://github.com/student/repo";

    private UserDao userDao;
    private RepoUpdateDao repoUpdateDao;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        repoUpdateDao = mock(RepoUpdateDao.class);
        userService = new UserService(userDao, repoUpdateDao);
    }

    @Test
    void updateRepoUrl_savesCleanUrlAndHistory() throws Exception {
        try (MockedStatic<RepoUrlValidator> validator = mockStatic(RepoUrlValidator.class)) {
            validator.when(() -> RepoUrlValidator.clean(INPUT_URL)).thenReturn(CLEAN_URL);
            validator.when(() -> RepoUrlValidator.isValid(CLEAN_URL)).thenReturn(true);
            when(repoUpdateDao.getUpdatesForRepo(CLEAN_URL)).thenReturn(List.of());

            userService.updateRepoUrl("student", INPUT_URL, null);

            verify(userDao).setRepoUrl("student", CLEAN_URL);
            ArgumentCaptor<RepoUpdate> update = ArgumentCaptor.forClass(RepoUpdate.class);
            verify(repoUpdateDao).insertUpdate(update.capture());
            assertEquals("student", update.getValue().netId());
            assertEquals(CLEAN_URL, update.getValue().repoUrl());
            assertFalse(update.getValue().adminUpdate());
            assertNull(update.getValue().adminNetId());
        }
    }

    @Test
    void updateRepoUrl_rejectsMalformedUrl() {
        assertThrows(BadRequestException.class,
                () -> userService.updateRepoUrl("student", "not-a-github-url", null));

        verifyNoInteractions(userDao, repoUpdateDao);
    }

    @Test
    void updateRepoUrl_rejectsPreviouslyClaimedUrl() throws Exception {
        RepoUpdate priorClaim = new RepoUpdate(Instant.now(), "another-student", CLEAN_URL, false, null);
        try (MockedStatic<RepoUrlValidator> validator = mockStatic(RepoUrlValidator.class)) {
            validator.when(() -> RepoUrlValidator.clean(INPUT_URL)).thenReturn(CLEAN_URL);
            validator.when(() -> RepoUrlValidator.isValid(CLEAN_URL)).thenReturn(true);
            when(repoUpdateDao.getUpdatesForRepo(CLEAN_URL)).thenReturn(List.of(priorClaim));

            assertThrows(WordOfWisdomViolationException.class,
                    () -> userService.updateRepoUrl("student", INPUT_URL, null));

            verify(userDao, never()).setRepoUrl(anyString(), anyString());
            verify(repoUpdateDao, never()).insertUpdate(any());
        }
    }

    @Test
    void updateRepoUrl_allowsOwnerToReuseAndRecordsAdminUpdate() throws Exception {
        RepoUpdate priorClaim = new RepoUpdate(Instant.now(), "student", CLEAN_URL, false, null);
        try (MockedStatic<RepoUrlValidator> validator = mockStatic(RepoUrlValidator.class)) {
            validator.when(() -> RepoUrlValidator.clean(INPUT_URL)).thenReturn(CLEAN_URL);
            validator.when(() -> RepoUrlValidator.isValid(CLEAN_URL)).thenReturn(true);
            when(repoUpdateDao.getUpdatesForRepo(CLEAN_URL)).thenReturn(List.of(priorClaim));

            userService.updateRepoUrl("student", INPUT_URL, "admin");

            verify(userDao).setRepoUrl("student", CLEAN_URL);
            ArgumentCaptor<RepoUpdate> update = ArgumentCaptor.forClass(RepoUpdate.class);
            verify(repoUpdateDao).insertUpdate(update.capture());
            assertTrue(update.getValue().adminUpdate());
            assertEquals("admin", update.getValue().adminNetId());
        }
    }

    @Test
    void adminGetRepoHistory_combinesRepoAndUserResults() throws Exception {
        RepoUpdate repoResult = new RepoUpdate(Instant.now(), "student", CLEAN_URL, false, null);
        RepoUpdate userResult = new RepoUpdate(Instant.now(), "student", "https://github.com/student/old", false, null);
        when(repoUpdateDao.getUpdatesForRepo(CLEAN_URL)).thenReturn(List.of(repoResult));
        when(repoUpdateDao.getUpdatesForUser("student")).thenReturn(List.of(userResult));

        assertEquals(List.of(repoResult, userResult), userService.adminGetRepoHistory(CLEAN_URL, "student"));
        verify(repoUpdateDao).getUpdatesForRepo(CLEAN_URL);
        verify(repoUpdateDao).getUpdatesForUser("student");
    }

    @Test
    void adminGetRepoHistory_requiresAtLeastOneFilter() {
        assertThrows(UnprocessableEntityException.class,
                () -> userService.adminGetRepoHistory(null, null));

        verifyNoInteractions(repoUpdateDao);
    }

    @Test
    void adminGetRepoHistory_wrapsDaoErrors() throws Exception {
        DataAccessException daoError = new DataAccessException("lookup failed");
        when(repoUpdateDao.getUpdatesForRepo(CLEAN_URL)).thenThrow(daoError);

        InternalServerException exception = assertThrows(InternalServerException.class,
                () -> userService.adminGetRepoHistory(CLEAN_URL, null));

        assertSame(daoError, exception.getCause());
    }
}
