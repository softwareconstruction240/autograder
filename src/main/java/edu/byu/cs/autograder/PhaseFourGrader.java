package edu.byu.cs.autograder;

import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;

import java.io.IOException;

import static edu.byu.cs.autograder.TestHelper.checkIfPassedPassoffTests;

public class PhaseFourGrader extends PassoffTestGrader {
    /**
     * Creates a new grader for phase X
     *
     * @param netId          the netId of the student
     * @param repoUrl        the url of the student repo
     * @param observer       the observer to notify of updates
     * @throws IOException if an IO error occurs
     */
    public PhaseFourGrader(String netId, String repoUrl, Observer observer) throws IOException {
        super("./phases/phase4", netId, repoUrl, observer, Phase.Phase4);
    }

    @Override
    protected boolean passed(Rubric rubric) {
        return checkIfPassedPassoffTests(rubric);
    }

    @Override
    protected String getCanvasRubricId(Rubric.RubricType type) {
        return switch (type) {
            case PASSOFF_TESTS -> "_2614";
            case UNIT_TESTS -> "_930";
            default -> throw new RuntimeException(String.format("No %s item for this phase", type));
        };
    }
}
