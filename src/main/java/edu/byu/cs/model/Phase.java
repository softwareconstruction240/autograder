package edu.byu.cs.model;

/**
 * Represent the phases that can be graded.
 */
public enum Phase {
    Phase0,
    Phase1,
    Phase3,
    Phase4,
    Phase5,
    Phase6,
    /**
     * This special phase is never graded for credit, but
     * allows students to receive the code-quality checking
     * feedback from the system without evaluating the project.
     */
    Quality,
    /**
     * This special phase is never to be graded, but exists to
     * serve as the global {@link RubricConfig} for all phases.
     * <br>
     * Would be named <code>GitCommits</code>, but the SQL table
     * requires the phase to be no more than 9 chars.
     */
    Commits,
    GitHub
}
