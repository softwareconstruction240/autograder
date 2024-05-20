package edu.byu.cs.model;

public enum Phase {
    Phase0,
    Phase1,
    Phase3,
    Phase4,
    Phase5,
    Phase6,
    Quality,
    /**
     * This special phase is never to be graded, but exists to
     * serve as the global {@link RubricConfig} for all phases.
     * <br>
     * Would be named <code>GitCommits</code>, but the SQL table
     * requires the phase to be no more than 9 chars.
     */
    Commits
}
