package edu.byu.cs.dataAccess.base;

import java.util.Random;

import edu.byu.cs.model.Phase;
import edu.byu.cs.model.User;

public class DaoTestUtils {
    static Random random = new Random();

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

    public static Phase getRandomPhase(){
        return phases[random.nextInt(0,7)];
    }

    public static int generateID() {
        return random.nextInt();
    }

    public static String generateNetID(int id) {
        return "cosmo_" + id;
    }

    public static String generateRepo(int id) {
        return "https://github.com/cosmo_%s/chess".formatted(id);
    }

    public static User generateStudentUser(int id) {
        return new User(
                generateNetID(id),
                generateID(),
                "Cosmo",
                "Cougar",
                generateRepo(id),
                User.Role.STUDENT
        );
    }
}
