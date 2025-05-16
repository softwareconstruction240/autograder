package edu.byu.cs.controller.netmodel;

import edu.byu.cs.model.Phase;

/**
 * A request to grade a submission
 *
 * @param phase the phase the submission will be graded for
 * @param repoUrl the repo url to use for the submission
 */
public record GradeRequest(Phase phase, String repoUrl) {}
