package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.daoInterface.SubmissionDao;
import edu.byu.cs.dataAccess.sql.SqlDb;
import edu.byu.cs.dataAccess.sql.SubmissionSqlDao;
import edu.byu.cs.dataAccess.sql.UserSqlDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import edu.byu.cs.properties.ApplicationProperties;

import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.Random;
import java.util.Properties;

class SubmissionDaoTest {
    static Random random;
    SubmissionDao dao;

    Submission firstSubmission;

    /**
     * In order to successfully run this test on your machine, you're going to need some environment variables
     * 
     * 
    */
    @BeforeAll
    static void prepareDatabase() throws DataAccessException {
        random = new Random();
        Properties props = new Properties();
        props.setProperty("db-host", System.getenv("DB_HOST"));
        props.setProperty("db-port", System.getenv("DB_PORT"));
        props.setProperty("db-name", System.getenv("DB_NAME"));
        props.setProperty("db-user", System.getenv("DB_USER"));
        props.setProperty("db-pass", System.getenv("DB_PASS"));
        ApplicationProperties.loadProperties(props);
        SqlDb.setUpDb();
    }

    @BeforeEach
    void setup() {
        dao = new SubmissionSqlDao();
        int cosmo_id = generateID();
        //Submission table has a foreign key constraint, so we need to generate a user as well
        User cosmo = generateStudentUser(cosmo_id);
        firstSubmission = generateSubmission(cosmo_id);
        Assertions.assertDoesNotThrow(()-> new UserSqlDao().insertUser(cosmo));
        Assertions.assertDoesNotThrow(() -> dao.insertSubmission(firstSubmission));
    }

    @Test
    void submissionSerialization() throws DataAccessException {
        var readSubmission = dao.getFirstPassingSubmission(firstSubmission.netId(), firstSubmission.phase());
        Assertions.assertEquals(firstSubmission, readSubmission);
    }

    @Test
    @Disabled
    void insertSubmission() {
    }

    @Test
    @Disabled
    void getSubmissionsForPhase() {
    }

    private User generateStudentUser(int id){
        return new User(
                "cosmo_" + id,
                generateID(),
                "Cosmo",
                "Cougar",
                generateRepo(id),
                User.Role.STUDENT
        );
    }

    private Submission generateSubmission(int id) {
        return new Submission(
                "cosmo_" + id,
                generateRepo(id),
                "fc80e76ee5bfa331840bc75d1a5efec2cc7a874c",
                Instant.now().minusSeconds(60L * random.nextInt(1, 100)),
                Phase.Phase0,
                true,
                1.618f,
                3.1415f,
                "This is only a testing submission for Cosmo Cougar (#%s).".formatted(id),
                null,
                true,
                Submission.VerifiedStatus.ApprovedManually,
                null, null,
                new Submission.ScoreVerification(100.1f, "cosmo_boss", Instant.now(),10)
        );
    }

    private int generateID(){
        return random.nextInt();
    }

    private String generateRepo(int id){
        return "https://github.com/cosmo_%s/chess".formatted(id);
    }
}
