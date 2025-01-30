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
        if(context.phase()!=Phase.Phase1&&context.phase()!=Phase.Phase0){
            String filePath = "server/src/main/resources";
            System.out.println(reader.filesMatching(filePath).count());
        }
    }
}
