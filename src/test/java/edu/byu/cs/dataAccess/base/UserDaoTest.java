package edu.byu.cs.dataAccess.base;

import edu.byu.cs.dataAccess.DataAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.byu.cs.dataAccess.daoInterface.UserDao;
import edu.byu.cs.model.User;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashSet;

public abstract class UserDaoTest {
    protected abstract UserDao newUserDao();
    protected abstract void clearUsers() throws DataAccessException;

    protected UserDao dao;

    static User student;
    static User noRepoStudent;
    static User admin;

    @BeforeAll
    static void setup(){
        student = DaoTestUtils.generateStudentUser(DaoTestUtils.generateID());
        noRepoStudent = new User(
                DaoTestUtils.generateNetID(DaoTestUtils.generateID()),
                DaoTestUtils.generateID(),
                "Rookie D",
                "Noob",
                null,
                User.Role.STUDENT
        );
        admin = new User(
                DaoTestUtils.generateNetID(DaoTestUtils.generateID()),
                DaoTestUtils.generateID(),
                "Cosmo Da",
                "Boss",
                null,
                User.Role.ADMIN
        );
    }

    @BeforeEach
    void getDao(){
        dao = newUserDao();
    }

    private static HashSet<User> provideTestUsers(){
        HashSet<User> users = new HashSet<>();
        users.add(student);
        users.add(admin);
        users.add(noRepoStudent);
        return users;
    }

    @ParameterizedTest
    @MethodSource("provideTestUsers")
    void insertUserTest(User user) throws DataAccessException {
        clearUsers();
        Assertions.assertDoesNotThrow(() -> dao.insertUser(user));
        User obtained = dao.getUser(user.netId());
        Assertions.assertEquals(user, obtained);
    }

    @Test
    void insertUserTwiceFails() throws DataAccessException {
        clearUsers();
        Assertions.assertDoesNotThrow(() -> {dao.insertUser(student);});

    }

    
}
