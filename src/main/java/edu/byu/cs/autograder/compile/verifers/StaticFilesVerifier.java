package edu.byu.cs.autograder.compile.verifers;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.autograder.compile.StudentCodeReader;
import edu.byu.cs.autograder.compile.StudentCodeVerifier;

import java.io.File;

public class StaticFilesVerifier implements StudentCodeVerifier {

    @Override
    public void verify(GradingContext context, StudentCodeReader reader) throws GradingException {
        if(context.phase()==Phase.Phase3){
            String filePath = "server/src/main/resources/web";
            File staticFiles = new File(context.stageRepo(), filePath);
            if(!staticFiles.exists() || !staticFiles.isDirectory()){
                context.observer().notifyWarning("It looks like you won't pass the static files test. " +
                        "Are the static files located in %s?".formatted(filePath));
            }
        }
    }
}
