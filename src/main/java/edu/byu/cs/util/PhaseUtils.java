package edu.byu.cs.util;

import edu.byu.cs.model.Phase;

public class PhaseUtils {

    // FIXME: dynamically get assignment numbers
    private static final int PHASE0_ASSIGNMENT_NUMBER = 880445;
    private static final int PHASE1_ASSIGNMENT_NUMBER = 880446;
    private static final int PHASE3_ASSIGNMENT_NUMBER = 880448;
    private static final int PHASE4_ASSIGNMENT_NUMBER = 880449;
    private static final int PHASE6_ASSIGNMENT_NUMBER = 880451;

    /**
     * Given a phase, returns the phase before it, or null.
     *
     * @param phase the current phase
     * @return the previous phase chronologically
     */
    public static Phase getPreviousPhase(Phase phase) {
        return switch (phase) {
            case Phase0 -> null;
            case Phase1 -> Phase.Phase0;
            case Phase3 -> Phase.Phase1;
            case Phase4 -> Phase.Phase3;
            case Phase6 -> Phase.Phase4;
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
            case Phase6 -> "6";
        };
    }

    /**
     * Given an integer representation of a phase as a string, returns the phase
     *
     * @param str examples include "0", "3"
     * @return the phase as an enum
     */
    public static Phase getStringAsPhase(String str) {
        return switch (str) {
            case "0" -> Phase.Phase0;
            case "1" -> Phase.Phase1;
            case "3" -> Phase.Phase3;
            case "4" -> Phase.Phase4;
            case "6" -> Phase.Phase6;
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
            case Phase6 -> PHASE6_ASSIGNMENT_NUMBER;
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
            case Phase0, Phase1, Phase4 -> 125.0F;
            case Phase3 -> 180.0F;
            case Phase6 -> 155.0F;
        };
    }
}
