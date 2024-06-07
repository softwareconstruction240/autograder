package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.verifers.TestLocationVerifier;
import edu.byu.cs.model.Phase;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class TestLocationVerifierTest {

    /*
     * Placeholder for this test in the future
     * Example repos used in testing:
     * https://github.com/Fiwafoofa/chessTA/tree/missing-test-directories
     * Deletes: client/src/test/java/client, server/src/test/java/passoff
     *
     * https://github.com/Fiwafoofa/chessTA/tree/bad-test-locations
     * Wrong package name: client/src/test/java/clientImABadStudent/ServerFacadeTests.java,
     *    server/src/test/java/serviceImABadStudent/ClearServiceTests.java,
     *    server/src/test/java/serviceImABadStudent/UserServiceTests.java
     * Missing package name: server/src/test/java/serviceImABadStudent/GameServiceTests.java
     *
     * Random package: client/src/test/java/someRandomTestPackage/yoooooooooooo.java
     *
     * Wrongly named packages: server/src/test/java/serviceImABadStudent
     */

    TestLocationVerifier customTestLocationVerifier;

    @Test
    void test() throws GradingException, IOException {
        GradingContext gradingContext = new GradingContext(
                "isaih",
                Phase.Phase5,
                "/IdeaProjects/autograder/phases",
                "/IdeaProjects/autograder/tmp-2009831688-1716239637",
                null,
                new File("/IdeaProjects/autograder/tmp-2009831688-1716239637/repo"),
                0,
                0,
                0,
                0,
                null,
                false
        );
        customTestLocationVerifier = new TestLocationVerifier();
        customTestLocationVerifier.verify(gradingContext, StudentCodeReader.from(gradingContext));
    }

}
