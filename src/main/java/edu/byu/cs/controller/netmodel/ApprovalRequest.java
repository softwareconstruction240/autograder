package edu.byu.cs.controller.netmodel;

import edu.byu.cs.model.Phase;

public record ApprovalRequest(String netId, Phase phase, boolean penalize) {
}
