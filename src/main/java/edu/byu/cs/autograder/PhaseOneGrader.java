package edu.byu.cs.autograder;

import edu.byu.cs.model.Phase;

import java.io.IOException;

public class PhaseOneGrader extends PassoffTestGrader {
    /**
     * Creates a new grader for phase 1
     *
     * @param netId    the netId of the student
     * @param repoUrl  the url of the student repo
     * @param observer the observer to notify of updates
     * @throws IOException if an IO error occurs
     */
    public PhaseOneGrader(String netId, String repoUrl, Observer observer) throws IOException {
        super("./phases/phase1", netId, repoUrl, observer, Phase.Phase1);
    }
}
