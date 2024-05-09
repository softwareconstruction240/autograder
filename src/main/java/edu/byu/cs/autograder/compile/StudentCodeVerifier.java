package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;

public interface StudentCodeVerifier {
    void verify(GradingContext context, StudentCodeReader reader) throws GradingException;
}
