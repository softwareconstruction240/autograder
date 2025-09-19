package edu.byu.cs.autograder.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestHelperTests {
    private final TestHelper testHelper = new TestHelper();

    @Test
    @DisplayName("Remove Javalin Lines Test")
    public void remove_javalin_lines_test() {
        String javalinLinesExample =
        """
        [main] INFO io.javalin.Javalin - Starting Javalin ...
        [main] INFO org.eclipse.jetty.server.Server - jetty-11.0.24; built: 2024-08-26T18:11:22.448Z; git: 5dfc59a691b748796f922208956bd1f2794bcd16; jvm 23.0.1+11-39
        [main] INFO org.eclipse.jetty.server.session.DefaultSessionIdManager - Session workerName=node0
        [main] INFO org.eclipse.jetty.server.handler.ContextHandler - Started o.e.j.s.ServletContextHandler@765f05af{/,null,AVAILABLE}
        [main] INFO org.eclipse.jetty.server.AbstractConnector - Started ServerConnector@78383390{HTTP/1.1, (http/1.1)}{0.0.0.0:12345}
        [main] INFO org.eclipse.jetty.server.Server - Started Server@2eee3069{STARTING}[11.0.24,sto=0] @1026ms
        [main] INFO io.javalin.Javalin -\s
               __                  ___           _____
              / /___ __   ______ _/ (_)___      / ___/
        __  / / __ `/ | / / __ `/ / / __ \\    / __ \\
        / /_/ / /_/ /| |/ / /_/ / / / / / /   / /_/ /
        \\____/\\__,_/ |___/\\__,_/_/_/_/ /_/    \\____/

               https://javalin.io/documentation

        [main] INFO io.javalin.Javalin - Javalin started in 270ms \\o/
        [main] INFO io.javalin.Javalin - Listening on http://localhost:12345/
        [main] INFO io.javalin.Javalin - You are running Javalin 6.4.0 (released December 17, 2024).
        Extra Lines of string should be kept.
        """;

        String result = TestHelper.removeJavalinLines(javalinLinesExample);
        String expected = "Extra Lines of string should be kept.";

        assertEquals(result, expected);
    }

    @Test
    @DisplayName("Trim Error Output Test")
    public void trim_error_output_test() {

        //Add Characters to equal max_error_output, add extra past this size to make sure it is truncated
        String fullExample = "THIS IS A TEST LINE\n".repeat(Math.max(0, testHelper.GET_MAX_ERROR_OUTPUT_CHARS() / 20)) + "MORE\n".repeat(200);

        String result = testHelper.trimErrorOutput(fullExample);

        String truncationMsg = "...\n(Error Output Truncated)";

        assertTrue(result.endsWith(truncationMsg));
        assertFalse(result.contains("MORE\n"));
        assertEquals(result.length() - truncationMsg.length(), testHelper.GET_MAX_ERROR_OUTPUT_CHARS());
    }
}
