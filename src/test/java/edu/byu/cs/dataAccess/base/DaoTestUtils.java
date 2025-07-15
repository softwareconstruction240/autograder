package edu.byu.cs.dataAccess.base;

import java.util.Random;

import edu.byu.cs.model.User;

public class DaoTestUtils {
    static Random random = new Random();

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
