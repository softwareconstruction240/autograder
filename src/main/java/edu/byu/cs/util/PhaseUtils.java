package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;

import java.util.Set;

public class PhaseUtils {

    // FIXME: dynamically get assignment numbers
    private static final int PHASE0_ASSIGNMENT_NUMBER = 880445;
    private static final int PHASE1_ASSIGNMENT_NUMBER = 880446;
    private static final int PHASE3_ASSIGNMENT_NUMBER = 880448;
    private static final int PHASE4_ASSIGNMENT_NUMBER = 880449;

    private static final int PHASE5_ASSIGNMENT_NUMBER = 880450;
    private static final int PHASE6_ASSIGNMENT_NUMBER = 880451;

    /**
     * Given a phase, returns the phase before it, or null.
     *
     * @param phase the current phase
     * @return the previous phase chronologically
     */
    public static Phase getPreviousPhase(Phase phase) {
        return switch (phase) {
            case Phase0, Quality -> null;
            case Phase1 -> Phase.Phase0;
            case Phase3 -> Phase.Phase1;
            case Phase4 -> Phase.Phase3;
            case Phase5 -> Phase.Phase4;
            case Phase6 -> Phase.Phase5;
        };
    }

    /**
     * Gives an integer representation of the Phase enum as a string
     *
     * @param phase the phase in question
     * @return the string
     */
    public static String getPhaseAsString(Phase phase) {
        return switch (phase) {
            case Phase0 -> "0";
            case Phase1 -> "1";
            case Phase3 -> "3";
            case Phase4 -> "4";
            case Phase5 -> "5";
            case Phase6 -> "6";
            case Quality -> "Quality";
        };
    }

    /**
     * Given an integer representation of a phase as a string, returns the phase
     *
     * @param str examples include "0", "3"
     * @return the phase as an enum
     */
    public static Phase getPhaseByString(String str) {
        return switch (str) {
            case "0" -> Phase.Phase0;
            case "1" -> Phase.Phase1;
            case "3" -> Phase.Phase3;
            case "4" -> Phase.Phase4;
            case "5" -> Phase.Phase5;
            case "6" -> Phase.Phase6;
            case "42" -> Phase.Quality;
            default -> null;
        };
    }

    /**
     * Produces the Canvas assignment number for this phase
     *
     * @param phase the phase in question
     * @return its assignment number in Canvas
     */
    public static int getPhaseAssignmentNumber(Phase phase) {
        return switch (phase) {
            case Phase0 -> PHASE0_ASSIGNMENT_NUMBER;
            case Phase1 -> PHASE1_ASSIGNMENT_NUMBER;
            case Phase3 -> PHASE3_ASSIGNMENT_NUMBER;
            case Phase4 -> PHASE4_ASSIGNMENT_NUMBER;
            case Phase5 -> PHASE5_ASSIGNMENT_NUMBER;
            case Phase6 -> PHASE6_ASSIGNMENT_NUMBER;
            case Quality -> 0;
        };
    }

    /**
     * Returns the number of points the given phase is worth
     *
     * @param phase the phase in question
     * @return the total points in Canvas as a float
     */
    public static float getTotalPoints(Phase phase) {
        // FIXME
        return switch (phase) {
            case Phase0, Phase1, Phase4, Phase5 -> 125.0F;
            case Phase3 -> 180.0F;
            case Phase6 -> 155.0F;
            case Quality -> 0.0F;
        };
    }

    public static Set<String> passoffPackagesToTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0 -> Set.of("passoffTests.chessTests", "passoffTests.chessTests.chessPieceTests");
            case Phase1 -> Set.of("passoffTests.chessTests", "passoffTests.chessTests.chessExtraCredit");
            case Phase3, Phase4, Phase6 -> Set.of("passoffTests.serverTests");
            case Phase5, Quality -> throw new GradingException("No passoff tests for this phase");
        };
    }

    public static Set<String> unitTestPackagesToTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> Set.of("serviceTests");
            case Phase4 -> Set.of("dataAccessTests");
            case Phase5 -> Set.of("clientTests");
        };
    }

    public static String unitTestCodeUnderTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> "service";
            case Phase4 -> "dao";
            case Phase5 -> "server facade";
        };
    }

    public static int minUnitTests(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> 13;
            case Phase4 -> 18;
            case Phase5 -> 12;
        };
    }

    public static String getCanvasRubricId(Rubric.RubricType type, Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0 -> switch (type) {
                case PASSOFF_TESTS -> "_1958";
                case GIT_COMMITS -> "90342_649";
                case UNIT_TESTS, QUALITY -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Phase1 -> switch (type) {
                case PASSOFF_TESTS -> "_1958";
                case GIT_COMMITS -> "90342_7800";
                case UNIT_TESTS, QUALITY -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Phase3 -> switch (type) {
                case PASSOFF_TESTS -> "_5202";
                case UNIT_TESTS -> "90344_776";
                case QUALITY -> "_3003";
                case GIT_COMMITS -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Phase4 -> switch (type) {
                case PASSOFF_TESTS -> "_2614";
                case UNIT_TESTS -> "_930";
                case GIT_COMMITS -> "90346_6985";
                case QUALITY -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Phase5 -> switch (type) {
                case UNIT_TESTS -> "_8849";
                case GIT_COMMITS -> "90347_6127";
                case PASSOFF_TESTS, QUALITY -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Phase6 -> switch (type) {
                case PASSOFF_TESTS -> "90348_899";
                case QUALITY -> "90348_3792";
                case GIT_COMMITS -> "90348_9048";
                case UNIT_TESTS -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Quality -> throw new GradingException("Not graded");
        };
    }

    public static String getModuleUnderTest(Phase phase) {
        //FIXME : Not sure what's wrong with this but there was a empty fixme comment when I refactored -Michael
        return switch (phase) {
            case Phase0, Phase1 -> "shared";
            case Phase3, Phase4, Phase6 -> "server";
            case Phase5 -> "client";
            case Quality -> null;
        };
    }

    public static boolean isPhaseGraded(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4, Phase5, Phase6 -> true;
            case Quality -> false;
        };
    }

    /**
     * Check if passoff tests are required for a given phase
     *
     * @param phase phase to check
     * @return true (passoff results are all or nothing), false (passoffs can be partial)
     */
    public static boolean isPassoffRequired(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4 -> true;
            case Phase5, Phase6, Quality -> false;
        };
    }
}
