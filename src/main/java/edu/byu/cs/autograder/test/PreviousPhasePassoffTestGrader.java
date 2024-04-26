package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class PreviousPhasePassoffTestGrader extends TestGrader{
    public PreviousPhasePassoffTestGrader(GradingContext gradingContext) {
        super(gradingContext);
    }

    @Override
    protected String name() {
        return "previous phase passoff";
    }

    @Override
    protected Set<String> excludedTests() {
        return new HashSet<>();
    }

    @Override
    protected Set<File> testsToCompile() {
        Set<File> files = new HashSet<>();
        Phase previous = gradingContext.phase();
        while((previous = PhaseUtils.getPreviousPhase(previous)) != null) {
            files.add(new File("./phases/phase" + PhaseUtils.getPhaseAsString(previous)));
        }
        return files;
    }

    @Override
    protected Set<String> packagesToTest() throws GradingException {
        Set<String> packages = new HashSet<>();
        Phase previous = gradingContext.phase();
        while((previous = PhaseUtils.getPreviousPhase(previous)) != null) {
            packages.addAll(PhaseUtils.passoffPackagesToTest(previous));
        }
        return packages;
    }

    @Override
    protected Set<String> extraCreditTests() {
        return new HashSet<>();
    }

    @Override
    protected String testName() {
        return "Previous Passoff Tests";
    }

    @Override
    protected float getScore(TestAnalyzer.TestAnalysis testResults) throws GradingException {
        if (testResults.root().numTestsFailed == 0) return 1f;
        throw new GradingException("Failed previous tests. Cannot pass off until previous tests pass", testResults);
    }

    @Override
    protected String getNotes(TestAnalyzer.TestAnalysis results) {
        if (results.root().numTestsFailed == 0) {
            return "All previous tests passed";
        }
        else {
            return "Failed previous tests. Cannot pass off until previous tests pass";
        }
    }

    @Override
    protected RubricConfig.RubricConfigItem rubricConfigItem(RubricConfig config) {
        return new RubricConfig.RubricConfigItem(null, null, 0);
    }
}
