package edu.byu.cs.util;

import java.io.*;
import java.util.concurrent.*;

public class ProcessUtils {

    private static final long DEFAULT_TIMEOUT = 120000;

    /**
     * Runs a process given by a process builder and returns process output
     * @param processBuilder process to run
     * @return output from process standard out
     */
    public static ProcessOutput runProcess(ProcessBuilder processBuilder) throws ProcessException {
        return runProcess(processBuilder, null, DEFAULT_TIMEOUT);
    }

    /**
     * Runs a process given by a process builder and returns process output
     * @param processBuilder process to run
     * @param timeout length to wait for in ms
     * @return output from process standard out
     */
    public static ProcessOutput runProcess(ProcessBuilder processBuilder, long timeout) throws ProcessException {
        return runProcess(processBuilder, null, timeout);
    }

    /**
     * Runs a process given by a process builder and returns process output
     * @param processBuilder process to run
     * @param input string to write to standard in for process
     * @return output from process standard out
     */
    public static ProcessOutput runProcess(ProcessBuilder processBuilder, String input) throws ProcessException {
        return runProcess(processBuilder, input, DEFAULT_TIMEOUT);
    }

    /**
     * Runs a process given by a process builder and returns process output
     * @param processBuilder process to run
     * @param input string to write to standard in for process
     * @param timeout length to wait for in ms
     * @return output from process standard out
     */
    public static ProcessOutput runProcess(ProcessBuilder processBuilder, String input, long timeout)
            throws ProcessException {
        try (ExecutorService processOutputExecutor = Executors.newFixedThreadPool(2)){

            Process process = processBuilder.start();

            /*
            Grab the output from the process asynchronously. Without this concurrency, if this is computed
            synchronously after the process terminates, the pipe from the process may fill up, causing the process
            writes to block, resulting in the process never finishing. This is usually the result of the tested
            code printing out too many lines to stdout as a means of logging/debugging
             */
            Future<String> processOutputFuture = processOutputExecutor.submit(() -> getOutputFromInputStream(process.getInputStream()));
            Future<String> processErrorFuture = processOutputExecutor.submit(() -> getOutputFromInputStream(process.getErrorStream()));

            if(input != null) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(input.getBytes());
                }
            }

            if (!process.waitFor(timeout, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new ProcessException("Process timed out. Try again or come see a TA if this error persists");
            }
            String output = processOutputFuture.get(1000, TimeUnit.MILLISECONDS);
            String error = processErrorFuture.get(1000, TimeUnit.MILLISECONDS);

            return new ProcessOutput(output, error, process.waitFor());
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new ProcessException(e);
        }
    }

    /**
     * Extracts the output as a string from an input stream
     *
     * @param is The input stream to extract the output from
     * @return The output of the process as a string
     * @throws IOException If an error occurs while reading the output
     */
    private static String getOutputFromInputStream(InputStream is) throws IOException {
        try (InputStreamReader isr = new InputStreamReader(is);
             BufferedReader br = new BufferedReader(isr)){

            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        }
    }

    public record ProcessOutput(String stdOut, String stdErr, int statusCode){}

    public static class ProcessException extends Exception {
        public ProcessException(String message) {
            super(message);
        }

        public ProcessException(Throwable cause) {
            super(cause);
        }
    }
}
