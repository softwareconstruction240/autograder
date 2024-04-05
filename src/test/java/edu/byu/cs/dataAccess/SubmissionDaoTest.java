package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.sql.SubmissionSqlDao;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SubmissionDaoTest {

    static SubmissionDao submissionDao = new SubmissionSqlDao();
    String studentId1 = "student_id_1";
    String studentId2 = "student_id_2";

    @AfterAll
    static void cleanup() {
        submissionDao.clear();
    }

    @BeforeEach
    void setUp() {
        // Insert several submissions
        // Multiple for one student in the same phase
        // Submissions for multiple phases
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void insertSubmission() {
    }

    @Test
    void getSubmissionsForPhase() {
    }

    @Test
    void getSubmissionsForUser() {
    }

    @Test
    void getAllLatestSubmissions() {
    }

    @Test
    void testGetAllLatestSubmissions() {
    }

    @Test
    void removeSubmissionsByNetId() {
    }

    @Test
    void getFirstPassingSubmission() {
    }

    @Test
    void getBestScoreForPhase() {
    }
}
