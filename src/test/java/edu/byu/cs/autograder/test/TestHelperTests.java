package edu.byu.cs.autograder.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestHelperTests {
    private final TestHelper testHelper = new TestHelper();

    @Test
    @DisplayName("Remove Spark Lines Test")
    public void remove_spark_lines_test() {
        String sparkLinesExample =
        """
        [main] INFO spark.staticfiles.StaticFilesConfiguration - StaticResourceHandler configured with folder = web
        [Thread-0] INFO org.eclipse.jetty.util.log - Logging initialized @447ms to org.eclipse.jetty.util.log.Slf4jLog
        [Thread-0] INFO spark.embeddedserver.jetty.EmbeddedJettyServer - == Spark has ignited ...
        [Thread-0] INFO spark.embeddedserver.jetty.EmbeddedJettyServer - >> Listening on 0.0.0.0:8080
        [Thread-0] INFO org.eclipse.jetty.server.Server - jetty-9.4.31.v20200723; built: 2020-07-23T17:57:36.812Z; git: 450ba27947e13e66baa8cd1ce7e85a4461cacc1d; jvm 21.0.3+7-LTS-152
        [Thread-0] INFO org.eclipse.jetty.server.session - DefaultSessionIdManager workerName=node0
        [Thread-0] INFO org.eclipse.jetty.server.session - No SessionScavenger set, using defaults
        [Thread-0] INFO org.eclipse.jetty.server.session - node0 Scavenging every 660000ms
        [Thread-0] INFO org.eclipse.jetty.server.handler.ContextHandler - Started o.e.j.s.ServletContextHandler@7564b469{/,null,AVAILABLE}
        [Thread-0] INFO org.eclipse.jetty.server.AbstractConnector - Started ServerConnector@158f462a{HTTP/1.1, (http/1.1)}{0.0.0.0:8080}
        [Thread-0] INFO org.eclipse.jetty.server.Server - Started @673ms
        Extra Lines of string should be kept.
        """;

        String result = TestHelper.removeSparkLines(sparkLinesExample);
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
