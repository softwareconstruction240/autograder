package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;

/**
 * The {@code StudentCodeModifier} interface provides a single method {@code modify}
 * for how a student's code and files should be modified for grading. Implementing classes
 * must define how the student's code and files should be modified with this method.
 */
public interface StudentCodeModifier {
    void modify(GradingContext context) throws GradingException;
}
