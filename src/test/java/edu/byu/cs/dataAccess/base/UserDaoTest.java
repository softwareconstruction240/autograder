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

import java.util.Collection;
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
                "Rookie Dee",
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
    void getDao() throws DataAccessException{
        dao = newUserDao();
        clearUsers();
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
        Assertions.assertDoesNotThrow(() -> dao.insertUser(user));
        User obtained = dao.getUser(user.netId());
        Assertions.assertEquals(user, obtained);
    }

    @Test
    void insertUserTwiceFails() {
        Assertions.assertDoesNotThrow(() -> dao.insertUser(student));
        Assertions.assertThrows(DataAccessException.class, () -> dao.insertUser(student));
    }

    @ParameterizedTest
    @MethodSource("provideTestUsers")
    void getUser(User user) throws DataAccessException {
        addAllTestUsers();
        User obtained = dao.getUser(user.netId());
        Assertions.assertEquals(user, obtained);
    }

    @Test
    void getNoUsers() throws DataAccessException {
        User obtained = dao.getUser("Does_Not_Exist");
        Assertions.assertNull(obtained);
        addAllTestUsers();
        obtained = dao.getUser("Still_Not_Here");
        Assertions.assertNull(obtained);
    }

    @ParameterizedTest
    @MethodSource("provideTestUsers")
    void setFirstName(User user) throws DataAccessException{
        dao.insertUser(user);
        String changed = "Changed";
        User expected = new User(
                user.netId(),
                user.canvasUserId(),
                changed,
                user.lastName(),
                user.repoUrl(),
                user.role()
        );
        Assertions.assertDoesNotThrow(() -> dao.setFirstName(user.netId(), changed));
        User obtained = dao.getUser(expected.netId());
        Assertions.assertEquals(expected, obtained);
    }

    @Test
    public void setFirstNameOnUserThatDoesNotExist() throws DataAccessException{
        dao.insertUser(student);
        dao.setFirstName("DNE", "DNE");
        User obtained = dao.getUser(student.netId());
        Assertions.assertEquals(student, obtained);
    }

    @ParameterizedTest
    @MethodSource("provideTestUsers")
    void setLastName(User user) throws DataAccessException{
        dao.insertUser(user);
        String changed = "Changed";
        User expected = new User(
                user.netId(),
                user.canvasUserId(),
                user.firstName(),
                changed,
                user.repoUrl(),
                user.role()
        );
        Assertions.assertDoesNotThrow(() -> dao.setLastName(user.netId(), changed));
        User obtained = dao.getUser(expected.netId());
        Assertions.assertEquals(expected, obtained);
    }

    @Test
    public void setLastNameOnUserThatDoesNotExist() throws DataAccessException{
        dao.insertUser(student);
        dao.setLastName("DNE", "DNE");
        User obtained = dao.getUser(student.netId());
        Assertions.assertEquals(student, obtained);
    }

    @ParameterizedTest
    @MethodSource("provideTestUsers")
    void setRepoUrl(User user) throws DataAccessException{
        dao.insertUser(user);
        String changed = DaoTestUtils.generateRepo(0);
        User expected = new User(
                user.netId(),
                user.canvasUserId(),
                user.firstName(),
                user.lastName(),
                changed,
                user.role()
        );
        Assertions.assertDoesNotThrow(() -> dao.setRepoUrl(user.netId(), changed));
        User obtained = dao.getUser(expected.netId());
        Assertions.assertEquals(expected, obtained);
    }

    @Test
    public void setRepoUrlOnUserThatDoesNotExist() throws DataAccessException{
        dao.insertUser(student);
        dao.setRepoUrl("DNE", "https://github.com/fake/fake");
        User obtained = dao.getUser(student.netId());
        Assertions.assertEquals(student, obtained);
    }

    @ParameterizedTest
    @MethodSource("provideTestUsers")
    void setRole(User user) throws DataAccessException {
        dao.insertUser(user);
        for (User.Role role : User.Role.values()){
            User expected = new User(
                    user.netId(),
                    user.canvasUserId(),
                    user.firstName(),
                    user.lastName(),
                    user.repoUrl(),
                    role
            );
            Assertions.assertDoesNotThrow(() -> dao.setRole(user.netId(), role));
            User obtained = dao.getUser(expected.netId());
            Assertions.assertEquals(expected, obtained);
        }
    }

    @Test
    public void setRoleOnUserThatDoesNotExist() throws DataAccessException{
        Collection<User> allUsers = provideTestUsers();
        addAllTestUsers();
        for(User.Role role : User.Role.values()) {
            dao.setRole("DNE", role);
            for (User user: allUsers){
                User obtained = dao.getUser(user.netId());
                Assertions.assertEquals(user, obtained);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("provideTestUsers")
    void setCanvasUserId(User user) throws DataAccessException {
        dao.insertUser(user);
        User expected = new User(
                user.netId(),
                0,
                user.firstName(),
                user.lastName(),
                user.repoUrl(),
                user.role()
        );
        Assertions.assertDoesNotThrow(() -> dao.setCanvasUserId(user.netId(), 0));
        User obtained = dao.getUser(expected.netId());
        Assertions.assertEquals(expected, obtained);
    }

    @Test
    public void setCanvasUserIdOnUserThatDoesNotExist() throws DataAccessException{
        dao.insertUser(student);
        dao.setCanvasUserId("DNE", 0);
        User obtained = dao.getUser(student.netId());
        Assertions.assertEquals(student, obtained);
    }

    @Test
    void getUsers() throws DataAccessException{
        Collection<User> obtained = dao.getUsers();
        Assertions.assertTrue(obtained.isEmpty());
        addAllTestUsers();
        Collection<User> expected = provideTestUsers();
        obtained = dao.getUsers();
        Assertions.assertEquals(expected.size(), obtained.size());
        for (User user : expected){
            Assertions.assertTrue(obtained.contains(user));
        }
    }

    @Test
    void repoUrlClaimed() throws DataAccessException{
        addAllTestUsers();
        Assertions.assertTrue(dao.repoUrlClaimed(student.repoUrl()));
        Assertions.assertFalse(dao.repoUrlClaimed("https://github.com/notclaimed/repo"));
        Assertions.assertFalse(dao.repoUrlClaimed(null));
    }

    private void addAllTestUsers() throws DataAccessException{
        for (User u : provideTestUsers()){
            dao.insertUser(u);
        }
    }


}
