package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitVerificationConfig;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;

import java.util.HashSet;
import java.util.Set;

public class PhaseUtils {

    // FIXME: dynamically get assignment numbers
    private static final int PHASE0_ASSIGNMENT_NUMBER = 921303;
    private static final int PHASE1_ASSIGNMENT_NUMBER = 921304;
    private static final int PHASE3_ASSIGNMENT_NUMBER = 921306;
    private static final int PHASE4_ASSIGNMENT_NUMBER = 921307;

    private static final int PHASE5_ASSIGNMENT_NUMBER = 921308;
    private static final int PHASE6_ASSIGNMENT_NUMBER = 921309;

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

    public static Set<String> passoffPackagesToTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0 -> Set.of("passoff.chess", "passoff.chess.piece");
            case Phase1 -> Set.of("passoff.chess.game", "passoff.chess.extracredit");
            case Phase3, Phase4, Phase6 -> Set.of("passoff.server");
            case Phase5, Quality -> throw new GradingException("No passoff tests for this phase");
        };
    }

    public static Set<String> unitTestPackagesToTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> Set.of("service");
            case Phase4 -> Set.of("dataaccess");
            case Phase5 -> Set.of("client");
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
                case UNIT_TESTS, QUALITY -> throw new GradingException(String.format("No %s item for this phase", type));
                case GIT_COMMITS -> "90342_649";
            };
            case Phase1 -> switch (type) {
                case PASSOFF_TESTS -> "_1958";
                case UNIT_TESTS, QUALITY -> throw new GradingException(String.format("No %s item for this phase", type));
                case GIT_COMMITS -> "90342_7800";
            };
            case Phase3 -> switch (type) {
                case PASSOFF_TESTS -> "_5202";
                case UNIT_TESTS -> "90344_776";
                case QUALITY -> "_3003";
                case GIT_COMMITS -> "90344_2520";
            };
            case Phase4 -> switch (type) {
                case PASSOFF_TESTS -> "_2614";
                case UNIT_TESTS -> "90346_5755";
                case QUALITY -> "90346_8398";
                case GIT_COMMITS -> "90346_6245";
            };
            case Phase5 -> switch (type) {
                case UNIT_TESTS -> "90347_2215";
                case PASSOFF_TESTS -> throw new GradingException(String.format("No %s item for this phase", type));
                case QUALITY -> "90347_9378";
                case GIT_COMMITS -> "90347_8497";
            };
            case Phase6 -> switch (type) {
                case PASSOFF_TESTS -> "90348_899";
                case QUALITY -> "90348_3792";
                case UNIT_TESTS -> throw new GradingException(String.format("No %s item for this phase", type));
                case GIT_COMMITS -> "90348_9048";
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

    public static Set<String> extraCreditTests(Phase phase) {
        if(phase == Phase.Phase1) return Set.of("CastlingTests", "EnPassantTests");
        return new HashSet<>();
    }

    public static float extraCreditValue(Phase phase) {
        if(phase == Phase.Phase1) return .04f;
        return 0;
    }

    public static CommitVerificationConfig verificationConfig(Phase phase) throws GradingException {
        int minimumLinesChanged = 5;
        int penaltyPct = 10;
        int forgivenessMinutesHead = 3;
        return new CommitVerificationConfig(10, 3, minimumLinesChanged, penaltyPct, forgivenessMinutesHead);
        // TODO: Enable this functionality to change the values next semester.
        // Don't forget to also update the rubrics in the phase specs and the grade-book rubrics.
//        return switch (phase) {
//            case Phase0, Phase1 -> new CommitVerificationConfig(8, 2, minimumLinesChanged, penaltyPct, forgivenessMinutesHead);
//            case Phase3, Phase4, Phase5, Phase6 -> new CommitVerificationConfig(12, 3, minimumLinesChanged, penaltyPct, forgivenessMinutesHead);
//            case Quality -> throw new GradingException("No commit verification for this phase");
//        };
    }

    public static String getPassoffPackagePath(Phase phase) {
        return switch (phase) {
            case Phase0 -> "phases/phase0.passoff.chess";
            case Phase1 -> "phases/phase1.passoff.chess";
            case Phase3 -> "phases/phase3.passoff.server";
            case Phase4 -> "phases/phase4.passoff.server";
            case Phase6 -> "phases/phase6.passoff.server";
            case Phase5, Quality -> null;
        };
    }

}
