package edu.byu.cs.autograder.compile.modifiers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.compile.StudentCodeModifier;
import edu.byu.cs.util.FileUtils;

import java.io.File;

/**
 * Modifies the <code>getMessageTime()</code> method in the test factory in the student's
 * code for phase 6 so the WebSocketTests only wait 1 second, but keeps the student's
 * <code>getGsonBuilder()</code> implementation the same
 */
public class TestFactoryModifier implements StudentCodeModifier {

    private static final String GET_MESSAGE_TIME_REGEX = "return \\d+[Ll];";

    private static final String REPLACEMENT = "return 1000L;";

    @Override
    public void modify(GradingContext context) throws GradingException {
        File phaseTestFactory = new File(context.phasesPath(), "phase6/passoff/server/TestFactory.java");
        File backupTestFactory = new File(context.phasesPath(), "backup/BackupTestFactory.java");
        File studentTestFactory = new File(context.stageRepo(), "server/src/test/java/passoff/server/TestFactory.java");

        if(!backupTestFactory.exists()) {
            if(!phaseTestFactory.exists()) throw new GradingException("Could not find phase test factory");
            FileUtils.copyFile(backupTestFactory, phaseTestFactory);
        }

        String contents = FileUtils.readStringFromFile(studentTestFactory.exists() ? studentTestFactory : backupTestFactory);

        if(contents.lines().noneMatch(s -> s.trim().matches(GET_MESSAGE_TIME_REGEX))) {
            throw new GradingException(String.format("Could not find line matching %s (like return 3000L;) in TestFactory (should be in getMessageTime() method)", GET_MESSAGE_TIME_REGEX));
        }

        contents = contents.replaceAll(GET_MESSAGE_TIME_REGEX, REPLACEMENT);
        FileUtils.writeStringToFile(contents, phaseTestFactory);
    }
}
