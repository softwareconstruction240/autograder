package edu.byu.cs.analytics;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

import java.time.Instant;

public record CommitThreshold(
        @NonNull Instant timestamp,
        @Nullable String commitHash
) {
}
