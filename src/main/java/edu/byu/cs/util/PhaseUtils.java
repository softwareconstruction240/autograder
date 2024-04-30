package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.dataAccess.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;

import java.util.Set;

public class PhaseUtils {

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
    public static int getPhaseAssignmentNumber(Phase phase) throws GradingException {
        ConfigurationDao.Configuration config = switch (phase) {
            case Phase0 -> ConfigurationDao.Configuration.PHASE0_ASSIGNMENT_NUMBER;
            case Phase1 -> ConfigurationDao.Configuration.PHASE1_ASSIGNMENT_NUMBER;
            case Phase3 -> ConfigurationDao.Configuration.PHASE3_ASSIGNMENT_NUMBER;
            case Phase4 -> ConfigurationDao.Configuration.PHASE4_ASSIGNMENT_NUMBER;
            case Phase5 -> ConfigurationDao.Configuration.PHASE5_ASSIGNMENT_NUMBER;
            case Phase6 -> ConfigurationDao.Configuration.PHASE6_ASSIGNMENT_NUMBER;
            default -> throw new GradingException("Phase not graded");
        };
        ConfigurationDao configurationDao = DaoService.getConfigurationDao();
        try {
            return configurationDao.getConfiguration(config, Integer.class);
        } catch (DataAccessException e) {
            throw new GradingException("Could not access configuration database", e);
        }
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
            case Phase0, Phase1 -> switch (type) {
                case PASSOFF_TESTS -> "_1958";
                case UNIT_TESTS, QUALITY -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Phase3 -> switch (type) {
                case PASSOFF_TESTS -> "_5202";
                case UNIT_TESTS -> "90344_776";
                case QUALITY -> "_3003";
            };
            case Phase4 -> switch (type) {
                case PASSOFF_TESTS -> "_2614";
                case UNIT_TESTS -> "_930";
                case QUALITY -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Phase5 -> switch (type) {
                case UNIT_TESTS -> "_8849";
                case PASSOFF_TESTS, QUALITY -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Phase6 -> switch (type) {
                case PASSOFF_TESTS -> "90348_899";
                case QUALITY -> "90348_3792";
                case UNIT_TESTS -> throw new GradingException(String.format("No %s item for this phase", type));
            };
            case Quality -> throw new GradingException("Not graded");
        };
    }

    public static String getModuleUnderTest(Phase phase) {
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
