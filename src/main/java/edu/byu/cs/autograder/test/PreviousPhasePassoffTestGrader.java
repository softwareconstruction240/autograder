package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.RubricConfig;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.Collection;
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
    protected Set<File> testsToCompile() throws GradingException {
        return allPreviousPhases((p) -> Set.of(new File("./phases/phase" + PhaseUtils.getPhaseAsString(p))));
    }

    @Override
    protected Set<String> packagesToTest() throws GradingException {
        return allPreviousPhases(PhaseUtils::passoffPackagesToTest);
    }

    private <T> Set<T> allPreviousPhases(PhaseFunction<T> func) throws GradingException {
        try {
            Set<T> set = new HashSet<>();
            Phase previous = gradingContext.phase();
            while ((previous = PhaseUtils.getPreviousPhase(previous)) != null) {
                RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(previous);
                if(rubricConfig.passoffTests() != null) {
                    set.addAll(func.apply(previous));
                }
            }
            return set;
        } catch (DataAccessException e) {
            throw new GradingException(e);
        }
    }

    @FunctionalInterface
    private interface PhaseFunction<T> {
        Collection<T> apply(Phase phase) throws GradingException;
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
        if (testResults.root().getNumTestsFailed() == 0) return 1f;
        throw new GradingException("Failed previous tests. Cannot pass off until previous tests pass", testResults);
    }

    @Override
    protected String getNotes(TestAnalyzer.TestAnalysis results) {
        if (results.root().getNumTestsFailed() == 0) {
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
