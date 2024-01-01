package edu.byu.cs.autograder;

import java.io.IOException;

public class PhaseZeroGrader extends PassoffTestGrader {
    /**
     * Creates a new grader for phase 1
     *
     * @param repoUrl        the url of the student repo
     * @param observer       the observer to notify of updates
     * @throws IOException if an IO error occurs
     */
    public PhaseZeroGrader(String repoUrl, Observer observer) throws IOException {
        super("./phases/phase0", repoUrl, observer);
    }
}
