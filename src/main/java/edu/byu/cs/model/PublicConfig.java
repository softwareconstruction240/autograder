package edu.byu.cs.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record PublicConfig(
    BannerConfig banner,
    ShutdownConfig shutdown,
    List<Phase> livePhases
) {
    public record BannerConfig(
            String message,
            String link,
            String color,
            String expiration
    ){}
    public record ShutdownConfig(
            String timestamp,
            Integer warningMilliseconds
    ){}
}
