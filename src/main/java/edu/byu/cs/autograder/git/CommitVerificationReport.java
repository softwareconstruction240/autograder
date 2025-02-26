package edu.byu.cs.autograder.git;

import edu.byu.cs.autograder.git.CommitValidation.CommitVerificationContext;
import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

/**
 * Holds both the context and the result used to arrive at a decision.
 *
 * @param context The {@link CommitVerificationContext} used to arrive at a decision.
 *                Null represents a skipped evaluation where commits were not actually evaluated.
 * @param result The {@link CommitVerificationResult} containing the result.
 *               Never null.
 */
public record CommitVerificationReport(
        @Nullable CommitVerificationContext context,
        @NonNull CommitVerificationResult result
) { }
