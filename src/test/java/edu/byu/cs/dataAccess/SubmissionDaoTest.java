package edu.byu.cs.dataAccess;

import edu.byu.cs.dataAccess.daoInterface.SubmissionDao;
import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.dataAccess.sql.SqlDb;
import edu.byu.cs.dataAccess.sql.SubmissionSqlDao;
import edu.byu.cs.dataAccess.sql.UserSqlDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigInteger;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

class SubmissionDaoTest {
    static final Phase[] phases = {
            Phase.Phase0,
            Phase.Phase1,
            Phase.Phase3,
            Phase.Phase4,
            Phase.Phase5,
            Phase.Phase6,
            Phase.Quality,
            Phase.GitHub
    };
    static Random random;
    SubmissionDao dao;
    UserDao userDao;
    int userID;
    static final int NUM_STUDENTS = 2;
    static final int SUBMISSIONS_PER_PHASE = 2;
    static final int DAY_RANGE = 7;

    /**
     * See {@link DaoTestUtils} for information about how to set up the SQL database for testing
     */
    @BeforeAll
    static void prepareDatabase() throws DataAccessException {
        random = new Random();
        DaoTestUtils.prepareSQLDatabase();
    }

    static IntStream latestSubmissionRange() {
        return IntStream.concat(
                IntStream.rangeClosed(-1, phases.length + 1),
                IntStream.of(phases.length * NUM_STUDENTS * SUBMISSIONS_PER_PHASE +1)
        );
        //return IntStream.rangeClosed(-1, phases.length * NUM_STUDENTS * SUBMISSIONS_PER_PHASE + 1);
    }

    static IntStream dayTestRange() {
        return IntStream.rangeClosed(0, DAY_RANGE + 1);
    }

    static Stream<Arguments> multipleSubmissionsRange(){
        return Stream.of(phases).flatMap(phase ->
                IntStream.rangeClosed(0, SUBMISSIONS_PER_PHASE).mapToObj(i ->
                        Arguments.of(i, phase)
                ));
        //return IntStream.rangeClosed(0, SUBMISSIONS_PER_PHASE);
    }

    @BeforeEach
    void setup() {
        dao = new SubmissionSqlDao();
        //Submission table has a foreign key constraint, so we need to generate a user
        userDao = new UserSqlDao();
        userID = generateID();
        Assertions.assertDoesNotThrow(() -> userDao.insertUser(generateStudentUser(userID)),
                "Could not insert initial user");
    }

    @Test
    void submissionSerialization() throws DataAccessException {
        Submission firstSubmission = generateSubmission(userID);

        Assertions.assertDoesNotThrow(() -> dao.insertSubmission(firstSubmission),
                "Could not insert submission");
        var readSubmission = dao.getFirstPassingSubmission(firstSubmission.netId(), firstSubmission.phase());
        Assertions.assertEquals(firstSubmission, readSubmission,
                "Submission obtained was not equal to submission inserted");
    }

    @Test
    void insertSubmission() throws DataAccessException {
        Submission newSubmission = generateSubmission(userID);

        Assertions.assertDoesNotThrow(() -> dao.insertSubmission(newSubmission),
                "Could not insert submission");
        Collection<Submission> submissions = dao.getSubmissionsForUser(generateNetID(userID));
        Assertions.assertTrue(submissions.contains(newSubmission),
                "Could not find inserted user in database");
    }

    @ParameterizedTest
    @EnumSource(
            value = Phase.class,
            names = {"Commits"},
            mode = EnumSource.Mode.EXCLUDE
    )
    void getSubmissionsForPhase(Phase phase) throws DataAccessException {
        Collection<Submission> expectedUserSubmissions = generateSubmissionDummyData(userID);
        Collection<Submission> actualUserSubmissions = dao.getSubmissionsForPhase(generateNetID(userID), phase);

        Assertions.assertEquals(SUBMISSIONS_PER_PHASE, actualUserSubmissions.size(),
                "Did not get all the submissions for given phase");
        for (Submission s : actualUserSubmissions) {
            Assertions.assertEquals(phase, s.phase(),
                    "Got submission with a different phase");
            Assertions.assertEquals(generateNetID(userID), s.netId(),
                    "Got submission with different netID");
            Assertions.assertTrue(expectedUserSubmissions.contains(s),
                    "Got a unexpected submission");
        }
    }

    @Test
    void getSubmissionsForUser() throws DataAccessException {
        Collection<Submission> expectedUserSubmissions = generateSubmissionDummyData(userID);
        Collection<Submission> otherSubmissions = generateStudentDummyData(generateID());
        Collection<Submission> actualSubmissions = dao.getSubmissionsForUser(generateNetID(userID));

        Assertions.assertEquals(expectedUserSubmissions.size(), actualSubmissions.size(),
                "Did not get the correct number of submissions for given netID");
        for (Submission s : actualSubmissions) {
            Assertions.assertEquals(generateNetID(userID), s.netId(),
                    "netID on submissions did not match");
            Assertions.assertTrue(expectedUserSubmissions.contains(s),
                    "Got a different submission from what was inserted");
            Assertions.assertFalse(otherSubmissions.contains(s),
                    "Got a submission for a different user");
        }
    }

    @ParameterizedTest(name = "batch of {0}")
    @MethodSource("latestSubmissionRange")
    void getAllLatestSubmissions(int batch) throws DataAccessException {
        clearSubmissions();
        Collection<Submission> allSubmissions = generateSubmissionDummyData(userID);
        allSubmissions.addAll(generateStudentDummyData(generateID()));
        Collection<Submission> obtainedSubmissions = dao.getAllLatestSubmissions(batch);

        if (batch != -1) {
            Assertions.assertTrue(batch >= obtainedSubmissions.size(),
                    "Got more submissions than the given batch size");
        }

        Assertions.assertTrue(allSubmissions.containsAll(obtainedSubmissions),
                "Got submissions that were not generated. Was the DB cleared out?");
        ArrayList<Submission> obtainedSubmissionsByTime = new ArrayList<>(obtainedSubmissions);
        for (int i = 0; i < obtainedSubmissionsByTime.size(); i++) {
            Submission submission = obtainedSubmissionsByTime.get(i);
            for (int j = i + 1; j < obtainedSubmissionsByTime.size(); j++) {
                Submission previousSubmission = obtainedSubmissionsByTime.get(j);
                //FIXME: this test could fail if the timestamps are the same
                Assertions.assertTrue(previousSubmission.timestamp().isBefore(submission.timestamp()),
                        "Previous time:" + previousSubmission.timestamp() + ", After time:"
                                + submission.timestamp());
                if (previousSubmission.netId().equals(submission.netId())) {
                    Assertions.assertNotEquals(previousSubmission.phase(), submission.phase(),
                            "Returned multiple phases for a user");
                }
            }
        }
    }

    @Test
    void removeSubmissionsByNullNetID() throws DataAccessException {
        clearSubmissions();
        Collection<Submission> expectedSubmissions = generateSubmissionDummyData(userID);

        dao.removeSubmissionsByNetId(null, 0);

        Collection<Submission> actualSubmissions = dao.getSubmissionsForUser(generateNetID(userID));
        Assertions.assertEquals(expectedSubmissions.size(), actualSubmissions.size(),
                "Removing with null netID deleted some submissions");
        Assertions.assertTrue(actualSubmissions.containsAll(expectedSubmissions),
                "Submissions returned where not the same as submissions generated");
    }

    @Test
    void removeSubmissionsByNetIDWithNoSubmissions() throws DataAccessException {
        clearSubmissions();
        int otherID = generateID();
        Collection<Submission> expectedSubmissions = generateStudentDummyData(otherID);

        dao.removeSubmissionsByNetId(generateNetID(userID), 0);

        Collection<Submission> actualSubmissions = dao.getSubmissionsForUser(generateNetID(otherID));
        Assertions.assertEquals(expectedSubmissions.size(), actualSubmissions.size(),
                "Removed submissions with netID that did not match");
        Assertions.assertTrue(actualSubmissions.containsAll(expectedSubmissions),
                "Submissions returned where not the same as submissions generated");
    }

    @ParameterizedTest(name = "{0} days old")
    @MethodSource("dayTestRange")
    void removeSubmissionsByNetID(int days) throws DataAccessException {
        clearSubmissions();
        int secondUserID = generateID();
        Collection<Submission> userSubmissions = generateSubmissionDummyData(userID);
        Collection<Submission> secondUserSubmissions = generateStudentDummyData(secondUserID);
        userSubmissions.removeIf(submission ->
                submission.timestamp().compareTo(Instant.now().minus(days, ChronoUnit.DAYS)) < 0);

        dao.removeSubmissionsByNetId(generateNetID(userID), days);

        Collection<Submission> obtainedSubmissions = dao.getSubmissionsForUser(generateNetID(userID));
        Assertions.assertTrue(userSubmissions.containsAll(obtainedSubmissions),
                "Did not remove the correct number of submissions");
        obtainedSubmissions = dao.getSubmissionsForUser(generateNetID(secondUserID));
        Assertions.assertTrue(secondUserSubmissions.containsAll(obtainedSubmissions),
                "Removed submissions for a user with a different netID");
    }

    @ParameterizedTest(name = "{1} with {0} submissions passing")
    @MethodSource("multipleSubmissionsRange")
    void getFirstPassingSubmission(int numPassing, Phase phase) throws DataAccessException {
        Collection<Submission> submissions = generatePassingSubmissions(numPassing, phase);
        Submission expectedPassing = findFirstPassing(submissions);

        Submission actualPassing = dao.getFirstPassingSubmission(generateNetID(userID), phase);
        Assertions.assertEquals(expectedPassing, actualPassing,
                "Did not get the same first passed submission:");

    }

    private Submission findFirstPassing(Collection<Submission> submissions) {
        long min = Long.MAX_VALUE;
        Submission earliest = null;
        for (Submission s : submissions) {
            if (s.passed() && s.timestamp().getEpochSecond() < min) {
                earliest = s;
                min = s.timestamp().getEpochSecond();
            }
        }
        return earliest;
    }

    @ParameterizedTest(name = "{1} with {0} submissions passing")
    @MethodSource("multipleSubmissionsRange")
    void getBestSubmissionForPhase(int numPassing, Phase phase) throws DataAccessException {
        Collection<Submission> submissions = generatePassingSubmissions(numPassing, phase);
        Submission expected = findBestSubmission(submissions);

        Submission actual = dao.getBestSubmissionForPhase(generateNetID(userID), phase);
        Assertions.assertEquals(expected, actual, "Did not obtain the best submission");
    }

    private Submission findBestSubmission(Collection<Submission> submissions) {
        Submission bestSubmission = null;
        for (Submission s : submissions) {
            if (s.passed()) {
                if (bestSubmission == null) {
                    bestSubmission = s;
                } else if (s.score() > bestSubmission.score()) {
                    bestSubmission = s;
                }
            }
        }
        return bestSubmission;
    }

    @ParameterizedTest
    @EnumSource(
            value = Phase.class,
            names = {"Commits"},
            mode = EnumSource.Mode.EXCLUDE
    )
    @Disabled
    void getBestSubmissionWithDuplicateScore(Phase phase) throws DataAccessException {
        Collection<Submission> submissions = new ArrayList<>();
        for (int i = 0; i < SUBMISSIONS_PER_PHASE; i++) {
            Submission duplicate = generateSubmission(userID, true, 3.1415f, phase);
            Assertions.assertDoesNotThrow(() -> dao.insertSubmission(duplicate),
                    "Could not insert submission");
            submissions.add(duplicate);
        }

        //FIXME: what is the tie-breaker here? because it's not time and the code doesn't seem to care
        Submission expected = findFirstPassing(submissions);
        Submission actual = dao.getBestSubmissionForPhase(generateNetID(userID), phase);
        Assertions.assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "{1} with {0} submissions passing")
    @MethodSource("multipleSubmissionsRange")
    void getAllPassingSubmissions(int numPassing, Phase phase) throws DataAccessException {
        Collection<Submission> allSubmissions = generatePassingSubmissions(numPassing, phase);
        Collection<Submission> expected = new ArrayList<>(allSubmissions);
        expected.removeIf(submission -> !submission.passed());

        Collection<Submission> actual = dao.getAllPassingSubmissions(generateNetID(userID));
        Assertions.assertEquals(expected.size(), actual.size(),
                "Did not get expected number of passing submissions");
        Assertions.assertTrue(actual.containsAll(expected),
                "Submissions returned were not equal");
    }

    @Test
    void manuallyApproveSubmission() throws DataAccessException {
        generateSubmissionDummyData(userID);
        Submission approved = generateSubmission(userID);
        Submission.ScoreVerification verification = new Submission.ScoreVerification(
                approved.score(),
                "cosmo_prof",
                Instant.now(),
                10
        );
        approved = approved.updateApproval(
                6.6f,
                Submission.VerifiedStatus.ApprovedManually,
                verification);
        Submission unapproved = approved.updateApproval(2.7182f,
                Submission.VerifiedStatus.Unapproved,
                null);

        dao.insertSubmission(unapproved);
        Assertions.assertDoesNotThrow(() -> dao.manuallyApproveSubmission(unapproved, 6.6f, verification),
                "Exception thrown on valid manual approval:");
        Collection<Submission> actual = dao.getSubmissionsForUser(generateNetID(userID));
        Assertions.assertTrue(actual.contains(approved),
                "Approved submission was not found in the user's submissions");
    }

    @Test
    void manuallyApproveSubmissionThrowsItemNotFound() {
        generateSubmissionDummyData(userID);
        Submission doesNotExist = generateSubmission(userID);
        doesNotExist = doesNotExist.updateApproval(0f, Submission.VerifiedStatus.Unapproved, null);
        Submission finalDoesNotExist = doesNotExist;

        Assertions.assertThrows(ItemNotFoundException.class,
                () -> {
                    dao.manuallyApproveSubmission(
                            finalDoesNotExist,
                            100.4f,
                            new Submission.ScoreVerification(3.1415f,
                                    "cosmo_not",
                                    Instant.now(),
                                    50));
                },
                "Did not throw ItemNotFoundException when trying to approve a non-existent submission");
    }

    private Collection<Submission> generatePassingSubmissions(int numPassing, Phase phase) {
        ArrayList<Submission> submissions = new ArrayList<>();
        for (int i = 0; i < SUBMISSIONS_PER_PHASE; i++) {
            Submission submission = i < numPassing ? generateSubmission(userID, true, phase) :
                    generateSubmission(userID, false, phase);
            Assertions.assertDoesNotThrow(() -> dao.insertSubmission(submission),
                    "Unable to insert submission");
            submissions.add(submission);
        }
        return submissions;
    }

    private User generateStudentUser(int id) {
        return new User(
                generateNetID(id),
                generateID(),
                "Cosmo",
                "Cougar",
                generateRepo(id),
                User.Role.STUDENT
        );
    }

    private Collection<Submission> generateSubmissionDummyData(int id) {
        HashSet<Submission> submissions = new HashSet<>();
        for (int i = 0; i < SUBMISSIONS_PER_PHASE; i++) {
            for (Phase phase : phases) {
                Submission s = generateSubmission(id, phase);
                Assertions.assertDoesNotThrow(() -> dao.insertSubmission(s));
                submissions.add(s);
            }
        }
        return submissions;
    }

    private Collection<Submission> generateStudentDummyData(int id) {
        User user = generateStudentUser(id);
        Assertions.assertDoesNotThrow(() -> userDao.insertUser(user), "Could not insert user");
        return generateSubmissionDummyData(id);
    }

    private Submission generateSubmission(int id) {
        return generateSubmission(id, true, Phase.Phase0);
    }

    private Submission generateSubmission(int id, boolean passed, Phase phase) {
        return generateSubmission(
                id,
                passed,
                random.nextFloat(3.1415f),
                phase
        );
    }

    private Submission generateSubmission(int id, Phase phase) {
        return generateSubmission(
                id,
                random.nextBoolean(),
                random.nextFloat(3.1415f),
                phase
        );
    }

    private Submission generateSubmission(int id, boolean passed, Float score, Phase phase) {
        return new Submission(
                generateNetID(id),
                generateRepo(id),
                generateRandomHash(),
                Instant.now().minusSeconds(random.nextLong(1, 86399))
                        .minus(random.nextInt(DAY_RANGE), ChronoUnit.DAYS),
                phase,
                passed,
                score,
                3.1415f,
                "This is only a testing submission for Cosmo Cougar (#%s).".formatted(id),
                null,
                true,
                Submission.VerifiedStatus.ApprovedManually,
                null, null,
                new Submission.ScoreVerification(100.1f, "cosmo_boss", Instant.now(), 0)
        );
    }

    private int generateID() {
        return random.nextInt();
    }

    private String generateNetID(int id) {
        return "cosmo_" + id;
    }

    private String generateRepo(int id) {
        return "https://github.com/cosmo_%s/chess".formatted(id);
    }

    private String generateRandomHash() {
        byte[] randomBytes = new byte[20];
        random.nextBytes(randomBytes);
        BigInteger no = new BigInteger(1, randomBytes);
        StringBuilder hash = new StringBuilder(no.toString(16));
        while (hash.length() < 40) {
            hash.insert(0, "0");
        }
        return hash.toString();
    }

    private void clearSubmissions() throws DataAccessException {
        try (var connection = SqlDb.getConnection();
             var statement = connection.prepareStatement("TRUNCATE submission")) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not clear database", e);
        }
    }
}
