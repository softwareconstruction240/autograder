package edu.byu.cs.autograder;

import edu.byu.cs.autograder.git.CommitValidation.CommitVerificationConfig;
import edu.byu.cs.model.Phase;

import java.io.File;

/**
 * Contains the necessary information to grade a submission
 *
 * @param netId           The netId of the student
 * @param phase           The phase to grade
 * @param phasesPath      The path where the official tests are stored
 * @param stagePath       The path for the student repo to be put in and tested
 * @param repoUrl         The url of the student repo
 * @param stageRepo       The path for the student repo (child of stagePath)
 * @param verificationConfig Several variables related to commit verification
 * @param observer        Used to notify the user of changes and issues during the grading process
 * @param admin           If the submission is an admin submission
 */
public record GradingContext(
        String netId,
        Phase phase,
        String phasesPath,
        String stagePath,
        String repoUrl,
        File stageRepo,

        // Commit Configuration
        CommitVerificationConfig verificationConfig,

        // Others
        GradingObserver observer,
        boolean admin
) { }
