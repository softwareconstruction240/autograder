package edu.byu.cs.autograder;

import edu.byu.cs.model.Phase;

import java.io.IOException;

public class PhaseThreeGrader extends PassoffTestGrader {
    /**
     * Creates a new grader for phase 3
     *
     * @param netId    the netId of the student
     * @param repoUrl  the url of the student repo
     * @param observer the observer to notify of updates
     * @throws IOException if an IO error occurs
     */
    public PhaseThreeGrader(String netId, String repoUrl, Observer observer) throws IOException {
        super("./phases/phase3", netId, repoUrl, observer, Phase.Phase3);
    }

    @Override
    protected void runCustomTests() {

    }

    /* Rubric Items Winter 2024:
    "_5202": "Web API Works"
    "_3003": "Code Quality"
    "90344_5657": "Web Page Loads"
    "90344_776": "Unit Tests"
     */
}
