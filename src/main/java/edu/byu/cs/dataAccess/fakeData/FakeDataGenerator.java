package edu.byu.cs.dataAccess.fakeData;

import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.dataAccess.SubmissionDao;
import edu.byu.cs.dataAccess.UserDao;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.Submission;
import edu.byu.cs.model.User;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.*;

/**
 * This class is really only to be run in a development environment, so that data access things can be tested.
 * The fake data generator loads data into the daos as provided by the DaoService
 */
public class FakeDataGenerator {

    private final String FIRST_NAME_FILE = "src/main/java/edu/byu/cs/dataAccess/fakeData/firstNames.txt";
    private final String LAST_NAME_FILE = "src/main/java/edu/byu/cs/dataAccess/fakeData/lastNames.txt";

    private final Random random = new Random();

    public static void main(String[] args) {
        FakeDataGenerator generator = new FakeDataGenerator();
        generator.run(100,5);
    }

    public void run(int numStudents, int numAdmin) {
        Collection<User> users = generateUsers(numStudents, numAdmin);
        Collection<Submission> submissions = generateSubmissions(users);

        UserDao userDao = DaoService.getUserDao();
        for (User user : users) {
            try {
                userDao.insertUser(user);
            } catch (DataAccessException e) {
                System.out.println("ERROR: netId: " + user.netId() + " generated twice, one user lost");
            }
        }
        SubmissionDao submissionDao = DaoService.getSubmissionDao();
        for (Submission submission : submissions) {
            submissionDao.insertSubmission(submission);
        }
    }

    private Collection<User> generateUsers(int numStudents, int numAdmin) {
        String[] firstNames = readNamesFromFile(FIRST_NAME_FILE);
        String[] lastNames = readNamesFromFile(LAST_NAME_FILE);
        ArrayList<User> users = new ArrayList<>();

        for (int i = 0; i < numStudents + numAdmin; i++) {
            String first = firstNames[randomNumberHelper(firstNames.length-1)];
            String last = lastNames[randomNumberHelper(lastNames.length-1)];
            String netId = netIdGenerator(first, last);
            User.Role role = i < numStudents ? User.Role.STUDENT : User.Role.ADMIN;
            String repo = role == User.Role.STUDENT ? repoUrlGenerator(netId) : null;
            int canvasUserId = randomNumberHelper(100000);
            users.add(new User(netId, canvasUserId, first, last, repo, role));
        }

        return users;
    }

    private Collection<Submission> generateSubmissions(Collection<User> users) {
        ArrayList<Submission> submissions = new ArrayList<>();
        for (User user : users) {
            if (user.repoUrl() == null) { continue; }
            for (Phase phase : Phase.values()) {
                submissions.add( new Submission(
                        user.netId(),
                        user.repoUrl(),
                        UUID.randomUUID().toString(),
                        getRandomTime(),
                        phase,
                        randomBool(),
                        (float) randomNumberHelper(100) / 100,
                        randomNumberHelper(50),
                        "This was a fake submission",
                        null
                        ));
                if (randomBool(1,5)) {
                    break;
                }
            }
        }
        return submissions;
    }

    private Rubric generateRubric() {
        return null;
    }

    private String netIdGenerator(String firstName, String lastName) {
        return lastName.toLowerCase().charAt(0) + firstName.toLowerCase().substring(0, Math.min(5, firstName.length())) + randomNumberHelper(100);
    }

    private String repoUrlGenerator(String netId) {
        return "https://github.com/" + netId + "/chess.git";
    }

    private boolean randomBool(int numeratorTrue, int total) {
        return randomNumberHelper(total-1) < numeratorTrue;
    }

    private boolean randomBool() {
        return randomBool(1,2);
    }

    private Instant getRandomTime() {
        long nowMillis = System.currentTimeMillis();
        long offset = random.nextLong() % (1000000000);
        return Instant.ofEpochMilli(nowMillis - offset);
    }

    private int randomNumberHelper(int max) {
        return Math.abs(random.nextInt() % max);
    }

    public static String[] readNamesFromFile(String fileName) {
        List<String> namesList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                namesList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return namesList.toArray(new String[0]);
    }
}
