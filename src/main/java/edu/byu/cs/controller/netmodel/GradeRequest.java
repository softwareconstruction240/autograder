package edu.byu.cs.controller.netmodel;

import edu.byu.cs.model.Phase;

public record GradeRequest(int phase) {
    public Phase getPhase() {
        return Phase.valueOf("Phase" + phase);
    }
}
