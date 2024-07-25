package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;
import edu.byu.cs.model.Phase;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerFacadeTestPortVerifier implements StudentCodeVerifier {
    private static final String DYNAMIC_PORT_LINE = "port = server.run(0)";
    private static final int CHECK_HARDCODED_PORT = 8080;
    private static final String EXPLANATION = "A port argument of 0 lets Spark/Jetty choose a port. The run method " +
            "then returns the port the server started on. Use this return value to set up your server facade " +
            "(probably \"var port\").";

    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        if(context.phase() != Phase.Phase5) return;

        try {
            Set<File> testFiles = reader.filesMatching(".*client/src/test/java/.*\\.java").collect(Collectors.toSet());
            boolean hardCodedPortFound = false;
            boolean dynamicPortLineMissing = true;
            for(File file : testFiles) {
                for(String line : reader.getFileContents(file)) {
                    if (line.contains(String.valueOf(CHECK_HARDCODED_PORT))) {
                        hardCodedPortFound = true;
                    }
                    if(line.contains(DYNAMIC_PORT_LINE)) {
                        dynamicPortLineMissing = false;
                    }
                }
            }

            if(hardCodedPortFound) {
                context.observer().notifyWarning(String.format("Found hardcoded number (probably port) %d in client " +
                        "tests. Please adjust to pass in 0 (zero) as the port. %s", CHECK_HARDCODED_PORT, EXPLANATION));
            }
            else if(dynamicPortLineMissing) {
                context.observer().notifyWarning(String.format("Could not locate a line matching %s in client tests " +
                        "from the starter code. Please ensure server run parameter is 0. %s", DYNAMIC_PORT_LINE, EXPLANATION));
            }
        } catch (IOException e) {
            throw new GradingException("Could not read file contents", e);
        }

    }
}
