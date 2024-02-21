package edu.byu.cs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.*;

public class ProcessUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessUtils.class);

    /**
     * Runs a process given by a process builder and returns process output
     * @param processBuilder process to run
     * @return output from process standard out
     */
    public static String runProcess(ProcessBuilder processBuilder) {
        try (ExecutorService processOutputExecutor = Executors.newSingleThreadExecutor()){

            Process process = processBuilder.start();

            /*
            Grab the output from the process asynchronously. Without this concurrency, if this is computed
            synchronously after the process terminates, the pipe from the process may fill up, causing the process
            writes to block, resulting in the process never finishing. This is usually the result of the tested
            code printing out too many lines to stdout as a means of logging/debugging
             */
            Future<String> processOutputFuture = processOutputExecutor.submit(() -> getOutputFromProcess(process));

            if (!process.waitFor(30000, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                LOGGER.error("Submission took too long to grade, come see a TA for more info");
                throw new RuntimeException("Submission took too long to grade, come see a TA for more info");
            }

            return processOutputFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extracts the output as a string from a process
     *
     * @param process The process to extract the output from
     * @return The output of the process as a string
     * @throws IOException If an error occurs while reading the output
     */
    private static String getOutputFromProcess(Process process) throws IOException {
        String output;

        InputStream is = process.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        {
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }

            output = sb.toString();
        }
        return output;
    }
}
