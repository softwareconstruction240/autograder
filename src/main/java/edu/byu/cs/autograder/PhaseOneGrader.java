package edu.byu.cs.autograder;

import java.io.IOException;

public class PhaseOneGrader extends PassoffTestGrader {
    /**
     * Creates a new grader for phase 1
     *
     * @param repoUrl  the url of the student repo
     * @param observer the observer to notify of updates
     * @throws IOException if an IO error occurs
     */
    public PhaseOneGrader(String repoUrl, Observer observer) throws IOException {
        super("./phases/phase1", repoUrl, observer);
    }
}
