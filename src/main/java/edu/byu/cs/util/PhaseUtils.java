package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import org.eclipse.jgit.annotations.NonNull;

import java.util.Set;

public class PhaseUtils {

    /**
     * Given a phase, returns the phase before it, or null.
     *
     * @param phase the current phase
     * @return the previous phase chronologically
     */
    public static Phase getPreviousPhase(Phase phase) {
        return phase.previousPhase;
    }

    /**
     * Gives an integer representation of the Phase enum as a string
     *
     * @param phase the phase in question
     * @return the string
     */
    public static String getPhaseAsString(Phase phase) {
        return phase.stringName;
    }

    /**
     * Given an integer representation of a phase as a string, returns the phase
     *
     * @param str examples include "0", "3"
     * @return the phase as an enum
     */
    public static Phase getPhaseByString(String str) {
        if (str.equals(Phase.Quality.stringName)) return Phase.Quality;
        return Phase.valueOf("Phase" + str);
    }

    /**
     * Produces the Canvas assignment number for this phase
     *
     * @param phase the phase in question
     * @return its assignment number in Canvas
     */
    public static int getPhaseAssignmentNumber(Phase phase) {
        return phase.assignmentNumber;
    }

    public static Set<String> passoffPackagesToTest(Phase phase) throws GradingException {
        var val = phase.passoffPackagesToTest;
        if (val == null) {
            throw new GradingException("No passoff tests for this phase");
        }
        return val;
    }

    public static Set<String> unitTestPackagesToTest(Phase phase) throws GradingException {
        var val = phase.unitTestPackagesToTest;
        if (val == null) {
            throw new GradingException("No unit tests for this phase");
        }
        return val;
    }

    public static String unitTestCodeUnderTest(Phase phase) throws GradingException {
        var val = phase.unitTestCodeUnderTest;
        if (val == null) {
            throw new GradingException("No unit tests for this phase");
        }
        return val;
    }

    public static int minUnitTests(Phase phase) throws GradingException {
        var val = phase.minUnitTests;
        if (val < 0) {
            throw new GradingException("No unit tests for this phase");
        }
        return val;
    }

    @NonNull
    public static String getCanvasRubricId(Rubric.RubricType type, Phase phase) throws GradingException {
        var rubricIds = phase.rubricIds;
        if (rubricIds == null) {
            throw new GradingException("Phase is not graded in Canvas");
        }

        var val = rubricIds.get(type);
        if (val == null) {
            throw new GradingException(String.format("No %s item for this phase", type));
        }
        return val;
    }

    public static String getModuleUnderTest(Phase phase) {
        return phase.moduleUnderTest;
    }

    public static boolean isPhaseGraded(Phase phase) {
        return phase.isGraded;
    }
}
