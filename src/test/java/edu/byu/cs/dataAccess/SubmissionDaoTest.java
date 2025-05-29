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

import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Random;
import java.util.Properties;

class SubmissionDaoTest {
    static Random random;
    SubmissionDao dao;
    int userID;

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
        userID = generateID();
        System.out.print(generateRandomHash());
        //Submission table has a foreign key constraint, so we need to generate a user as well
        User cosmo = generateStudentUser(userID);
        firstSubmission = generateSubmission(userID);
        Assertions.assertDoesNotThrow(()-> new UserSqlDao().insertUser(cosmo));
        Assertions.assertDoesNotThrow(() -> dao.insertSubmission(firstSubmission));
    }

    @Test
    void submissionSerialization() throws DataAccessException {
        var readSubmission = dao.getFirstPassingSubmission(firstSubmission.netId(), firstSubmission.phase());
        Assertions.assertEquals(firstSubmission, readSubmission);
    }

    @Test
    void insertSubmission() {
        Submission newSubmission = generateSubmission(userID);
        Assertions.assertDoesNotThrow(()-> dao.insertSubmission(newSubmission));
        Assertions.assertDoesNotThrow(() -> {
            Collection<Submission> submissions = dao.getSubmissionsForUser(generateNetID(userID));
            Assertions.assertTrue(submissions.contains(newSubmission));
        });
    }

    @Test
    @Disabled
    void getSubmissionsForPhase() {
    }

    private User generateStudentUser(int id){
        return new User(
                generateNetID(id),
                generateID(),
                "Cosmo",
                "Cougar",
                generateRepo(id),
                User.Role.STUDENT
        );
    }

    private Submission generateSubmission(int id){
        return generateSubmission(
                id,
                random.nextInt(3),
                random.nextBoolean(),
                random.nextFloat(3.1415f),
                generateRandomHash(),
                Phase.Phase0
        );
    }

    private Submission generateSubmission(int id, String hash){
        return generateSubmission(
                id,
                random.nextInt(3),
                random.nextBoolean(),
                random.nextFloat(3.1415f),
                hash,
                Phase.Phase0
        );
    }

    private Submission generateSubmission(int id, Phase phase){
        return generateSubmission(
                id,
                random.nextInt(3),
                random.nextBoolean(),
                random.nextFloat(3.1415f),
                generateRandomHash(),
                phase
        );
    }

    private Submission generateSubmission(int id, int daysOld, boolean passed, Float score, String hash, Phase phase) {
        return new Submission(
                generateNetID(id),
                generateRepo(id),
                hash,
                Instant.now().minusSeconds(60L * random.nextInt(1, 100)).minus(daysOld, ChronoUnit.DAYS),
                phase,
                passed,
                score,
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

    private String generateNetID(int id){
        return "cosmo_" + id;
    }

    private String generateRepo(int id){
        return "https://github.com/cosmo_%s/chess".formatted(id);
    }

    private String generateRandomHash(){
        byte[] randomBytes = new byte[20];
        random.nextBytes(randomBytes);
        BigInteger no = new BigInteger(1,randomBytes);
        StringBuilder hash = new StringBuilder(no.toString(16));
        while (hash.length() < 40) {
            hash.insert(0, "0");
        }
        return hash.toString();
    }
}
