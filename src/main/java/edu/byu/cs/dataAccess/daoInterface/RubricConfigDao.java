package edu.byu.cs.dataAccess.daoInterface;

import edu.byu.cs.dataAccess.DataAccessException;
import edu.byu.cs.model.Phase;
import edu.byu.cs.model.Rubric;
import edu.byu.cs.model.RubricConfig;

import java.util.EnumMap;
import java.util.Map;

import static edu.byu.cs.model.Rubric.RubricType.*;

/**
 * A data access object interface for {@link RubricConfig} objects to maintain the configuration
 * for rubric items for a particular phase
 */
public interface RubricConfigDao {
    RubricConfig defaultGitHubConfig = new RubricConfig(Phase.GitHub, new EnumMap<>(Map.of(
            GITHUB_REPO, new RubricConfig.RubricConfigItem("GitHub Repository", "Two Git commits: one for creating the repository and another for `notes.md`.", 15, "_6829")
    )));
    RubricConfig defaultPhase0Config = new RubricConfig(Phase.Phase0, new EnumMap<>(Map.of(
            GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90342_649"),
            PASSOFF_TESTS, new RubricConfig.RubricConfigItem("Functionality", "All pass off test cases succeed", 125, "_1958")
    )));
    RubricConfig defaultPhase1Config = new RubricConfig(Phase.Phase1, new EnumMap<>(Map.of(
            GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90342_7800"),
            EXTRA_CREDIT, new RubricConfig.RubricConfigItem("Extra Credit", "Castling and En Passant", 10, "90342_7835"),
            PASSOFF_TESTS, new RubricConfig.RubricConfigItem("Functionality", "All pass off test cases succeed", 125, "_1958")
    )));
    RubricConfig defaultPhase3Config = new RubricConfig(Phase.Phase3, new EnumMap<>(Map.of(
            GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90344_2520"),
            PASSOFF_TESTS, new RubricConfig.RubricConfigItem("Web API Works", "All pass off test cases in StandardAPITests.java succeed", 125, "_5202"),
            QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, "_3003"),
            UNIT_TESTS, new RubricConfig.RubricConfigItem(
                    "Unit Tests",
                    "All test cases pass\nEach public method on your Service classes has two test cases, one positive test and one negative test\nEvery test case includes an Assert statement of some type",
                    25,
                    "90344_776")
    )));
    RubricConfig defaultPhase4Config =
            new RubricConfig(Phase.Phase4, new EnumMap<>(Map.of(
                    GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90346_6245"),
                    PASSOFF_TESTS, new RubricConfig.RubricConfigItem("Functionality", "All pass off test cases succeed", 100, "_2614"),
                    QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, "90346_8398"),
                    UNIT_TESTS, new RubricConfig.RubricConfigItem(
                            "Unit Tests",
                            "All test cases pass\nEach public method on DAO classes has two test cases, one positive test and one negative test\nEvery test case includes an Assert statement of some type",
                            25,
                            "90346_5755")
            )));
    RubricConfig defaultPhase5Config = new RubricConfig(Phase.Phase5, new EnumMap<>(Map.of(
            GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90347_8497"),
            QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, "90347_9378"),
            UNIT_TESTS, new RubricConfig.RubricConfigItem(
                    "Unit Tests",
                    "All test cases pass\nEach public method on the Server Facade class has two test cases, one positive test and one negative test\nEvery test case includes an Assert statement of some type",
                    25,
                    "90347_2215")
    )));
    RubricConfig defaultPhase6Config = new RubricConfig(Phase.Phase6, new EnumMap<>(Map.of(
            GIT_COMMITS, new RubricConfig.RubricConfigItem("Git Commits", "Necessary commit amount", 0, "90348_9048"),
            PASSOFF_TESTS, new RubricConfig.RubricConfigItem(
                    "Automated Pass Off Test Cases",
                    "Each provided test case passed is worth a proportional number of points ((passed / total) * 50).",
                    50,
                    "90348_899"),
            QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, "90348_3792")
    )));
    RubricConfig defaultQualityConfig = new RubricConfig(Phase.Quality, new EnumMap<>(Map.of(
            QUALITY, new RubricConfig.RubricConfigItem("Code Quality", "Chess Code Quality Rubric (see GitHub)", 30, null)
    )));
    /**
     * Gets the rubric for the given phase
     *
     * @param phase the phase of the rubric
     * @return the rubric for the given phase
     */
    RubricConfig getRubricConfig(Phase phase) throws DataAccessException;

    default int getPhaseTotalPossiblePoints(Phase phase) throws DataAccessException {
        RubricConfig rubricConfig = getRubricConfig(phase);
        int total = 0;
        if (rubricConfig == null) {
            return total;
        }
        for(RubricConfig.RubricConfigItem item : rubricConfig.items().values()) {
            if(item != null) {
                total += item.points();
            }
        }
        return total;
    }

    void setRubricConfig(Phase phase, RubricConfig rubricConfig) throws DataAccessException;

    void setRubricIdAndPoints(Phase phase, Rubric.RubricType type, Integer points, String rubric_id) throws DataAccessException;
}
