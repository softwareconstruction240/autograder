package edu.byu.cs.analytics;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

import java.time.Instant;

/**
 * Either the head or the tail commit threshold.
 * <ul>
 * <li>The head commit threshold is the latest commit in the phase
 * (<STRONG>inclusively</STRONG> being graded during a submission).</li>
 * <li>The tail commit threshold is the last commit already graded in a previous phase. This
 * <STRONG>exclusively</STRONG> marks the end of the range of commits that will be considered.</li>
 * </ul>
 *
 * @param timestamp the timestamp the commit was authored
 * @param commitHash the hash of the commit that uniquely identifies it
 */
public record CommitThreshold(
        @NonNull Instant timestamp,
        @Nullable String commitHash
) {
}
