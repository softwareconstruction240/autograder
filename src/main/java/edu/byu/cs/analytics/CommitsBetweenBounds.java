package edu.byu.cs.analytics;

import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Contains all the (new) commits between the bounds of a head-tail has comparison.
 * <br>
 * Notably, the resulting commits can only be defined in terms of their commit ancestry,
 * and not by timestamp. Additional filtering is required down the line to exclude commits
 * based on time.
 *
 * @param commits An iterable of commits.
 * @param missingTail Reported as true when a tail hash was expected, but not found.
 *                    This should result in a warning/failed verification.
 *                    This can occur if a previous submission commit was lost in a rebase,
 *                    or simply discarded in a destructive repository restart performed by students.
 */
public record CommitsBetweenBounds(
        Iterable<RevCommit> commits,
        boolean missingTail
) {
}
