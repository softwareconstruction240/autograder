package edu.byu.cs.model;

import java.time.Instant;
import java.util.ArrayList;

public record PublicConfig(
    BannerConfig banner,
    ShutdownConfig shutdown,
    String livePhases
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
