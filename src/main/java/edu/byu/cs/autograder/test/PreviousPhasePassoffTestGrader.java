package edu.byu.cs.autograder.test;

import edu.byu.cs.autograder.GradingContext;
import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.*;
import edu.byu.cs.util.PhaseUtils;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Runs and scores the passoff tests for all the phases previous to the phase
 * a submission is graded for
 */
public class PreviousPhasePassoffTestGrader extends TestGrader {
    private static final String ERROR_MESSAGE = "Failed previous phases' tests. Cannot pass off until previous tests pass.";

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

    /**
     * Takes a {@link PhaseFunction}, runs it's <code>apply</code> method for every phase
     * previous to the phase stored in the grading context and returns a set of {@code T}
     * resulted by running the <code>apply</code> method for those phases.
     *
     * @param func the {@link FunctionalInterface} that runs the <code>apply</code> method
     * @return A set of {@code T} resulted by running the <code>apply</code> method
     * @param <T> The type the previous phase tests are returned as
     * @throws GradingException if the {@link PhaseFunction} has trouble accessing the tests
     * for the phase or if {@link DaoService} is unable to get the {@link RubricConfig} for
     * the previous phase
     */
    private <T> Set<T> allPreviousPhases(PhaseFunction<T> func) throws GradingException {
        try {
            Set<T> set = new HashSet<>();
            Phase previous = gradingContext.phase();
            while ((previous = PhaseUtils.getPreviousPhase(previous)) != null) {
                RubricConfig rubricConfig = DaoService.getRubricConfigDao().getRubricConfig(previous);
                if(rubricConfig.items().get(Rubric.RubricType.PASSOFF_TESTS) != null) {
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
    protected String testName() {
        return "Previous Passoff Tests";
    }

    @Override
    protected float getScore(TestOutput testResults) throws GradingException {
        if (testResults.root().getNumTestsFailed() == 0) return 1f;
        StringBuilder errorBuilder = new StringBuilder(ERROR_MESSAGE).append(" \nFailing tests: \n");
        failingTests(testResults.root(), errorBuilder);
        Rubric.Results results = Rubric.Results.testError(errorBuilder.toString(), testResults);
        throw new GradingException("Failed previous phase tests", results);
    }

    private void failingTests(TestNode node, StringBuilder builder) {
        if(node.getPassed() != null && !node.getPassed()) {
            builder.append(node.getTestName()).append(" \n");
        }
        for(TestNode child : node.getChildren().values()) {
            failingTests(child, builder);
        }
    }

    @Override
    protected String getNotes(TestOutput results) {
        if (results.root().getNumTestsFailed() == 0) return "All previous tests passed";
        else return ERROR_MESSAGE;
    }

    @Override
    protected Rubric.RubricType rubricType() {
        return Rubric.RubricType.GRADING_ISSUE;
    }

    @Override
    protected Set<String> modulesToCheckCoverage() {
        return Set.of();
    }
}
