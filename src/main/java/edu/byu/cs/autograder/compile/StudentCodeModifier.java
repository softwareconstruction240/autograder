package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;

public interface StudentCodeModifier {
    void modify(GradingContext context) throws GradingException;
}
