package edu.byu.cs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessUtilsTest {

    @Test
    void runProcess__noInput() {
        ProcessBuilder processBuilder = new ProcessBuilder("echo", "Hello, World!");
        String input = null;
        long timeout = 1000;

        ProcessUtils.ProcessOutput processOutput = null;
        try {
            processOutput = ProcessUtils.runProcess(processBuilder, input, timeout);
        } catch (ProcessUtils.ProcessException e) {
            fail("Process failed to run", e);
        }

        assertNotNull(processOutput);
        assertEquals("Hello, World!\n", processOutput.stdOut());
        assertEquals("", processOutput.stdErr());
        assertEquals(0, processOutput.statusCode());
    }

    @Test
    void runProcess__input() {
        ProcessBuilder processBuilder = new ProcessBuilder("cat");
        String input = "Hello, World!";
        long timeout = 1000;

        ProcessUtils.ProcessOutput processOutput = null;
        try {
            processOutput = ProcessUtils.runProcess(processBuilder, input, timeout);
        } catch (ProcessUtils.ProcessException e) {
            fail("Process failed to run", e);
        }

        assertNotNull(processOutput);
        assertEquals("Hello, World!\n", processOutput.stdOut());
        assertEquals("", processOutput.stdErr());
        assertEquals(0, processOutput.statusCode());
    }

    @Test
    void runProcess__timeout() {
        ProcessBuilder processBuilder = new ProcessBuilder("sleep", "2");
        String input = null;
        long timeout = 500;

        assertThrows(ProcessUtils.ProcessException.class, () -> ProcessUtils.runProcess(processBuilder, input, timeout));
    }

    @Test
    void runProcess__massiveStdOut() {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "for i in {1..1000000}; do echo $i; done");
        String input = null;
        long timeout = 15000;

        ProcessUtils.ProcessOutput processOutput = null;
        try {
            processOutput = ProcessUtils.runProcess(processBuilder, input, timeout);
        } catch (ProcessUtils.ProcessException e) {
            fail("Process failed to run", e);
        }

        assertNotNull(processOutput);
        assertEquals(1000000, processOutput.stdOut().split("\n").length);
        assertEquals("", processOutput.stdErr());
        assertEquals(0, processOutput.statusCode());
    }

    @Test
    void runProcess__massiveStdErr() {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "for i in {1..1000000}; do echo $i 1>&2; done");
        String input = null;
        long timeout = 15000;

        ProcessUtils.ProcessOutput processOutput = null;
        try {
            processOutput = ProcessUtils.runProcess(processBuilder, input, timeout);
        } catch (ProcessUtils.ProcessException e) {
            fail("Process failed to run", e);
        }

        assertNotNull(processOutput);
        assertEquals(1000000, processOutput.stdErr().split("\n").length);
        assertEquals("", processOutput.stdOut());
        assertEquals(0, processOutput.statusCode());
    }

    @Test
    void runProcess__massiveStdErrAndStdOut() {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "for i in {1..1000000}; do echo $i; echo $i 1>&2; done");
        String input = null;
        long timeout = 15000;

        ProcessUtils.ProcessOutput processOutput = null;
        try {
            processOutput = ProcessUtils.runProcess(processBuilder, input, timeout);
        } catch (ProcessUtils.ProcessException e) {
            fail("Process failed to run", e);
        }

        assertNotNull(processOutput);
        assertEquals(1000000, processOutput.stdOut().split("\n").length);
        assertEquals(1000000, processOutput.stdErr().split("\n").length);
        assertEquals(0, processOutput.statusCode());
    }

    @Test
    void runProcess__invalidCommand() {
        ProcessBuilder processBuilder = new ProcessBuilder("this_command_does_not_exist");
        String input = null;
        long timeout = 1000;

        assertThrows(ProcessUtils.ProcessException.class, () -> ProcessUtils.runProcess(processBuilder, input, timeout));
    }

    @Test
    void runProcess__nonZeroExitStatus() {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "exit 1");
        String input = null;
        long timeout = 1000;

        ProcessUtils.ProcessOutput processOutput = null;
        try {
            processOutput = ProcessUtils.runProcess(processBuilder, input, timeout);
        } catch (ProcessUtils.ProcessException e) {
            fail("Process failed to run", e);
        }

        assertNotNull(processOutput);
        assertEquals("", processOutput.stdOut());
        assertEquals("", processOutput.stdErr());
        assertEquals(1, processOutput.statusCode());
    }
}
