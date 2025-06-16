package edu.byu.cs.controller.netmodel;

import edu.byu.cs.model.Phase;

/**
 * A request to approve a submission with insufficient commits
 *
 * @param netId the netId of the student who sent the submission
 * @param phase the phase the submission is for
 * @param penalize whether the student should be penalized for insufficient commits
 */
public record ApprovalRequest(String netId, Phase phase, boolean penalize) {
}
