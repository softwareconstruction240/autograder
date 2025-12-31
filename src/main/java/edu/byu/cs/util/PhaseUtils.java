package edu.byu.cs.util;

import edu.byu.cs.autograder.GradingException;
import edu.byu.cs.autograder.git.CommitValidation.CommitVerificationConfig;
import edu.byu.cs.dataAccess.daoInterface.ConfigurationDao;
import edu.byu.cs.dataAccess.DaoService;
import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A utility class that provides methods for several phase-related operations
 */
public class PhaseUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseUtils.class);

    /**
     * Determines whether the phase is enabled for student submission
     *
     * @param phase the phase to check
     * @return a boolean indicating if the phase is enabled
     * @throws DataAccessException if an occurred getting the configuration for the phase
     */
    public static boolean isPhaseEnabled(Phase phase) throws DataAccessException {
        boolean phaseEnabled;

        try {
            phaseEnabled = DaoService.getConfigurationDao()
                    .getConfiguration(ConfigurationDao.Configuration.STUDENT_SUBMISSIONS_ENABLED, String.class)
                    .contains(phase.toString());
        } catch (DataAccessException e) {
            LOGGER.error("Error getting configuration for live phase", e);
            throw e;
        }

        return phaseEnabled;
    }

    /**
     * Given a phase, returns the phase before it, or null.
     *
     * @param phase the current phase
     * @return the previous phase chronologically
     */
    public static Phase getPreviousPhase(Phase phase) {
        return switch (phase) {
            case Phase0, Quality, GitHub -> null;
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
            case GitHub -> "GitHub";
        };
    }

    /**
     * Produces the Canvas assignment number for this phase
     *
     * @param phase the phase in question
     * @return its assignment number in Canvas
     */
    public static int getPhaseAssignmentNumber(Phase phase) throws DataAccessException {
        if (!PhaseUtils.isPhaseGraded(phase)) {
            throw new IllegalArgumentException("No phase assignment number for " + phase);
        }
        return DaoService.getConfigurationDao().getConfiguration(
                PhaseUtils.getConfigurationAssignmentNumber(phase),
                Integer.class);
    }

    /**
     * Gets the Canvas rubric id for a given phase and rubric type
     *
     * @param type the rubric type (e.g. "Pass-off Tests", "Git Commits", "Code Quality")
     * @param phase the phase to get the Canvas rubric id for
     * @return the rubric id for the rubric item
     * @throws DataAccessException if an error occurs getting the configuration for the rubric item
     */
    public static String getCanvasRubricId(Rubric.RubricType type, Phase phase) throws DataAccessException {
        if (!PhaseUtils.isPhaseGraded(phase)) {
            throw new IllegalArgumentException("No canvas rubric for ungraded phase: " + phase);
        }
        RubricConfig.RubricConfigItem configItem = DaoService.getRubricConfigDao()
                .getRubricConfig(phase)
                .items()
                .get(type);
        if (configItem == null) {
            throw new IllegalArgumentException(String.format("No '%s' item for phase '%s'", type, phase));
        }
        return configItem.rubric_id();
    }

    /**
     * Gets the packages for where passoff tests for a given phase is
     *
     * @param phase the phase to get the passoff test packages for
     * @return the packages for the passoff tests
     * @throws GradingException if there aren't passoff tests for the given phase
     */
    public static Set<String> passoffPackagesToTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0 -> Set.of("passoff.chess", "passoff.chess.piecemoves");
            case Phase1 -> Set.of("passoff.chess.game");
            case Phase3, Phase4, Phase6 -> Set.of("passoff.server");
            case Phase5, Quality, GitHub -> throw new GradingException("No passoff tests for this phase");
        };
    }

    /**
     * Gets the packages for where student-written unit tests for a given phase is
     *
     * @param phase the phase to get the unit test packages for
     * @return the packages for the unit tests
     * @throws GradingException if there aren't unit tests for the given phase
     */
    public static Set<String> unitTestPackagesToTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality, GitHub -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> Set.of("service");
            case Phase4 -> Set.of("dataaccess");
            case Phase5 -> Set.of("client");
        };
    }

    /**
     * Gets the paths for where the required test packages for a given phase are
     *
     * @param phase the phase to get the required test packages for
     * @return the paths of the packages for the required tests for the phase
     */
    public static Set<String> requiredTestPackagePaths(Phase phase) {
        return switch (phase) {
            case Phase0, Phase6, Quality, GitHub -> new HashSet<>();
            case Phase1 -> Set.of("shared/src/test/java/passoff/chess/game");
            case Phase3 -> Set.of("server/src/test/java/service", "server/src/test/java/passoff/server");
            case Phase4 -> Set.of("server/src/test/java/dataaccess");
            case Phase5 -> Set.of("client/src/test/java/client");
        };
    }

    /**
     * Gets the name of the class needed to unit test for a given phase
     *
     * @param phase the phase to determine what to unit test for
     * @return the name of the class needed to unit test code
     * @throws GradingException if the phase does not contain unit tests
     */
    public static String unitTestCodeUnderTest(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality, GitHub -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> "service";
            case Phase4 -> "dao";
            case Phase5 -> "server facade";
        };
    }

    /**
     * Gets the minimum number of unit tests required for a given phase
     *
     * @param phase the phase to get the number of unit tests for
     * @return the minimum number of units required for the phase
     * @throws GradingException if the phase does not contain unit tests
     */
    public static int minUnitTests(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality, GitHub -> throw new GradingException("No unit tests for this phase");
            case Phase3 -> 13;
            case Phase4 -> 18;
            case Phase5 -> 12;
        };
    }

    /**
     * Gets the modules needed to check for code coverage while testing student-written unit tests
     *
     * @param phase the phase that contains unit tests to check code coverage for
     * @return the module to check for code coverage
     * @throws GradingException if the phase does not contain unit tests to check for code coverage
     */
    public static Set<String> unitTestModulesToCheckCoverage(Phase phase) throws GradingException {
        return switch (phase) {
            case Phase0, Phase1, Phase6, Quality, GitHub -> throw new GradingException("No unit tests for this phase");
            case Phase3, Phase4 -> Set.of("server");
            case Phase5 -> Set.of("client"); //In case anyone tries this in the future, Jacoco won't like
                                                // trying to get the server and client at the same time
        };
    }

    /**
     * Gets the module where tests are located for a particular phase
     *
     * @param phase the phase to test
     * @return the module where tests are located or null if there aren't tests for the phase
     */
    public static String getModuleUnderTest(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1 -> "shared";
            case Phase3, Phase4, Phase6 -> "server";
            case Phase5 -> "client";
            case Quality, GitHub -> null;
        };
    }

    /**
     * Determines if the phase is graded and can modify a student's score in Canvas
     *
     * @param phase the phase to check
     * @return a boolean indicating if the phase is enabled
     */
    public static boolean isPhaseGraded(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4, Phase5, Phase6, GitHub -> true;
            case Quality -> false;
        };
    }

    /**
     * Returns the required rubric types to submit a phase
     *
     * @param phase phase to check
     * @return the required rubric types for the provided phase
     */
    public static Collection<Rubric.RubricType> requiredRubricTypes(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4, Phase6 -> Set.of(Rubric.RubricType.PASSOFF_TESTS);
            case GitHub -> Set.of(Rubric.RubricType.GITHUB_REPO);
            case Phase5, Quality -> Set.of();
        };
    }

    /**
     * Gets the packages for where extra credit tests for a given phase is
     *
     * @param phase the phase that may have extra credit tests
     * @return the packages for the extra credit tests
     * @throws GradingException if there aren't extra credit tests for the given phase
     */
    public static Set<String> extraCreditPackagesToTest(Phase phase) throws GradingException {
        if (phase == Phase.Phase1) return Set.of("passoff.chess.extracredit");
        throw new GradingException("No extra credit tests for this phase");
    }

    /**
     * Provides the value (as a float) to assign to extra credit tests given the phase
     *
     * @param phase the phase that may have extra credit tests
     * @return the value to assign to extra credit tests if any, otherwise returns zero
     */
    public static float extraCreditValue(Phase phase) {
        if(phase == Phase.Phase1) return .04f;
        return 0;
    }

    /**
     * Gets the {@link CommitVerificationConfig} for a given phase
     *
     * @param phase the phase to check regarding commit verification
     * @return several values regarding the commit verification system as a {@link CommitVerificationConfig}
     * @throws GradingException if there was an error getting the git commit config
     * or there is no commit verification for the given phase
     */
    public static CommitVerificationConfig verificationConfig(Phase phase) throws GradingException {
        ConfigurationDao dao = DaoService.getConfigurationDao();
        int minimumLinesChanged;
        int penaltyPct;
        int forgivenessMinutesHead;
        try {
            penaltyPct = Math.round(dao.getConfiguration(ConfigurationDao.Configuration.GIT_COMMIT_PENALTY, Float.class) * 100);
            minimumLinesChanged = dao.getConfiguration(ConfigurationDao.Configuration.LINES_PER_COMMIT_REQUIRED, Integer.class);
            forgivenessMinutesHead = dao.getConfiguration(ConfigurationDao.Configuration.CLOCK_FORGIVENESS_MINUTES, Integer.class);
        } catch (DataAccessException e) {
            throw new GradingException("Error getting git commit config", e);
        }

        return switch (phase) {
            case Phase0, Phase1, Phase4 -> new CommitVerificationConfig(8, 2, minimumLinesChanged, penaltyPct, forgivenessMinutesHead);
            case Phase3, Phase5, Phase6 -> new CommitVerificationConfig(12, 3, minimumLinesChanged, penaltyPct, forgivenessMinutesHead);
            case GitHub -> new CommitVerificationConfig(2, 0, 0, 0, forgivenessMinutesHead);
            case Quality -> throw new GradingException("No commit verification for this phase");
        };
    }

    /**
     * Determines whether the phase should require a TA passoff if a student submits that
     * phase with insufficient commits
     *
     * @param phase the phase to determine
     * @return a boolean indicating whether the phase requires TA passoff
     */
    public static boolean requiresTAPassoffForCommits(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4, Phase5, Phase6 -> true;
            case Quality, GitHub -> false;
        };
    }

    /**
     * Determines whether the phase should count and verify commits when submitted
     *
     * @param phase the phase to determine
     * @return a boolean indicating if the phase should count and verify commits
     */
    public static boolean shouldVerifyCommits(Phase phase) {
        return switch (phase) {
            case Phase0, Phase1, Phase3, Phase4, Phase5, Phase6, GitHub -> true;
            case Quality -> false;
        };
    }

    /**
     * Determines whether the phase should get a penalty for insufficient commits when submitted
     *
     * @param phase the phase to determine
     * @return a boolean indicating if the phase has a commit penalty
     */
    public static boolean phaseHasCommitPenalty(Phase phase) {
        return switch (phase) {
            case GitHub, Quality -> false;
            case Phase0, Phase1, Phase3, Phase4, Phase5, Phase6 -> true;
        };
    }

    /**
     * Converts a string representing a phase to a {@link Phase} enum
     *
     * @param phaseString the string representing the phase
     * @return the {@link Phase} enum converted from the {@code phaseString}
     * @throws IllegalArgumentException if the string could not be converted to a phase
     */
    public static Phase getPhaseFromString(String phaseString) throws IllegalArgumentException {
        phaseString = phaseString.toLowerCase().replaceAll("\\s", "");
        if (phaseString.contains("phase0")) {
            return Phase.Phase0;
        } else if (phaseString.contains("phase1")) {
            return Phase.Phase1;
        } else if (phaseString.contains("phase3")) {
            return Phase.Phase3;
        } else if (phaseString.contains("phase4")) {
            return Phase.Phase4;
        } else if (phaseString.contains("phase5")) {
            return Phase.Phase5;
        } else if (phaseString.contains("phase6")) {
            return Phase.Phase6;
        } else if (phaseString.contains("github")) {
            return Phase.GitHub;
        }
        throw new IllegalArgumentException("Could not convert string to phase given '" + phaseString + "'");
    }

    /**
     * Gets the {@link Rubric.RubricType} items needed for a given phase for grading
     *
     * @param phase the phase to grade
     * @return the {@link Rubric.RubricType} items for the provided phase
     */
    public static Collection<Rubric.RubricType> getRubricTypesFromPhase(Phase phase) {
        return switch (phase) {
            case GitHub -> Set.of(Rubric.RubricType.GITHUB_REPO);
            case Quality -> Set.of(Rubric.RubricType.QUALITY);
            case Phase0, Phase1 -> Set.of(Rubric.RubricType.GIT_COMMITS, Rubric.RubricType.PASSOFF_TESTS);
            case Phase3, Phase4 -> Set.of(Rubric.RubricType.GIT_COMMITS, Rubric.RubricType.PASSOFF_TESTS, Rubric.RubricType.QUALITY, Rubric.RubricType.UNIT_TESTS);
            case Phase5 -> Set.of(Rubric.RubricType.QUALITY, Rubric.RubricType.GIT_COMMITS, Rubric.RubricType.UNIT_TESTS);
            case Phase6 -> Set.of(Rubric.RubricType.GIT_COMMITS, Rubric.RubricType.PASSOFF_TESTS, Rubric.RubricType.QUALITY);
            default -> Collections.emptySet();
        };
    }

    /**
     * Gets a {@link ConfigurationDao.Configuration} key used to get the assignment number
     * for a given phase in Canvas
     *
     * @param phase the phase to be graded
     * @return a {@link ConfigurationDao.Configuration} enum key used to get the assignment number
     */
    public static ConfigurationDao.Configuration getConfigurationAssignmentNumber(Phase phase) {
        return switch (phase) {
            case GitHub -> ConfigurationDao.Configuration.GITHUB_ASSIGNMENT_NUMBER;
            case Phase0 -> ConfigurationDao.Configuration.PHASE0_ASSIGNMENT_NUMBER;
            case Phase1 -> ConfigurationDao.Configuration.PHASE1_ASSIGNMENT_NUMBER;
            case Phase3 -> ConfigurationDao.Configuration.PHASE3_ASSIGNMENT_NUMBER;
            case Phase4 -> ConfigurationDao.Configuration.PHASE4_ASSIGNMENT_NUMBER;
            case Phase5 -> ConfigurationDao.Configuration.PHASE5_ASSIGNMENT_NUMBER;
            case Phase6 -> ConfigurationDao.Configuration.PHASE6_ASSIGNMENT_NUMBER;
            default -> throw new IllegalArgumentException("No configuration assignment number for " + phase);
        };
    }

}
