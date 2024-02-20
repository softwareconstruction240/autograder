package edu.byu.cs.autograder;

import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.Phase;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

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
    protected Rubric.Results runCustomTests() {
        Set<String> excludedTests = new TestHelper().getTestFileNames(phaseTests);
        new TestHelper().compileTests(
                stageRepo,
                "server",
                new File(stageRepo, "server/src/test/java"),
                stagePath,
                excludedTests);

        TestAnalyzer.TestNode results = new TestHelper().runJUnitTests(
                new File(stageRepo, "/server/target/server-jar-with-dependencies.jar"),
                new File(stagePath, "tests"),
                new HashSet<>()

        );

        results.testName = CUSTOM_TESTS_NAME;

//        return new Rubric.Results("", getScore(results), results, null);
        return null;
    }

    @Override
    protected boolean passed(Rubric rubric) {
        boolean passed = true;

        if (rubric.passoffTests() != null && rubric.passoffTests().results() != null)
            if (rubric.passoffTests().results().score() < 1)
                passed = false;

        if (rubric.unitTests() != null && rubric.unitTests().results() != null)
            if (rubric.unitTests().results().score() < 1)
                passed = false;

        // TODO: enable quality check
//        if (rubric.quality() != null && rubric.quality().results() != null) {
//            if (rubric.quality().results().score() < 1)
//                passed = false;
//        }

        return passed;
    }

    /* Rubric Items Winter 2024:
    "_5202": "Web API Works"
    "_3003": "Code Quality"
    "90344_5657": "Web Page Loads"
    "90344_776": "Unit Tests"
     */
}
