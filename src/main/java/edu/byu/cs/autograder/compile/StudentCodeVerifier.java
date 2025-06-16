package edu.byu.cs.autograder.compile;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;

/**
 * The {@code StudentCodeVerifier} interface provides a single method {@code verify}
 * for how a student's code and file structure should be verified for grading.
 * Implementing classes must define how the student's code and file structure should
 * be verified with this method.
 */
public interface StudentCodeVerifier {
    void verify(GradingContext context, StudentCodeReader reader) throws GradingException;
}
