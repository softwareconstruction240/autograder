package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;

import java.io.File;

public class StaticFilesVerifier implements StudentCodeVerifier {
    private static final String FILE_PATH = "server/src/main/resources/web";

    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        if(context.phase()==Phase.Phase3){
            File staticFiles = new File(context.stageRepo(), FILE_PATH);
            if(!staticFiles.exists() || !staticFiles.isDirectory()){
                context.observer().notifyWarning("Could not locate folder %s. ".formatted(FILE_PATH) +
                        "This may lead to the autograder giving different results for the \"static files\" test." +
                        "Ensure the files are in the specified location and committed and pushed to your repository.");
            }
        }
    }
}
