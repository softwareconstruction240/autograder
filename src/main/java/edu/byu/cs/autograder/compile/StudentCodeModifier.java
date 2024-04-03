package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;

import java.io.File;

public interface StudentCodeModifier {
    void modifyCode(GradingContext context) throws GradingException;
}
