package edu.byu.cs.controller.netmodel;

import edu.byu.cs.model.Phase;

public record GradeRequest(int phase, String repoUrl) {
    public Phase getPhase() {
        if(phase == 42) return Phase.Quality;
        return Phase.valueOf("Phase" + phase);
    }
}
