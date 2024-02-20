package edu.byu.cs.autograder;

import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;

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

    @Override
    protected boolean passed(Rubric rubric) {
        if (rubric.passoffTests() == null || rubric.passoffTests().results() == null || rubric.passoffTests().results().testResults() == null)
            throw new RuntimeException("Passoff tests are null");

        return rubric.passoffTests().results().testResults().numTestsFailed == 0;
    }
}
