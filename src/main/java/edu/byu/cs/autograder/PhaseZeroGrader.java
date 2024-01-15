package edu.byu.cs.autograder;

import edu.byu.cs.model.Phase;

import java.io.IOException;

public class PhaseZeroGrader extends PassoffTestGrader {
    /**
     * Creates a new grader for phase 0
     *
     * @param netId    the netId of the student
     * @param repoUrl  the url of the student repo
     * @param observer the observer to notify of updates
     * @throws IOException if an IO error occurs
     */
    public PhaseZeroGrader(String netId, String repoUrl, Observer observer) throws IOException {
        super("./phases/phase0", netId, repoUrl, observer, Phase.Phase0);
    }
}
