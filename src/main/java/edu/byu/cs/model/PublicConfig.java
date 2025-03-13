package edu.byu.cs.model;

import java.util.List;

/**
 * Represents the configuration values that can be read by any user
 *
 * @param banner A {@link BannerConfig} containing information to display
 *               a dynamic message to be displayed at the top of the AutoGrader
 * @param shutdown A {@link ShutdownConfig} containing information about when all
 *                 phases for the AutoGrader will shut down
 * @param livePhases A list of phases that are currently active on the AutoGrader
 *                   and students can submit to
 */
public record PublicConfig(
    BannerConfig banner,
    ShutdownConfig shutdown,
    List<Phase> livePhases
) {
    /**
     * Represents the configuration information needed to display a dynamic message
     * at the top of the AutoGrader that students can see
     *
     * @param message the string containing the message
     * @param link the url the user will be taken to if they click on the banner
     * @param color the background color
     * @param expiration the time the banner message will expire (in MDT time), if expires.
     *                   The format of the expiration is in
     *                   <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO-8601 format</a>.
     */
    public record BannerConfig(
            String message,
            String link,
            String color,
            String expiration
    ){ }

    /**
     * Represents the configuration information needed to shut down all phases
     * for the AutoGrader at the end of the semester
     *
     * @param timestamp the time all phases will shut down (in MDT time). The format of the timestamp
     *                  is in <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO-8601 format</a>.
     * @param warningMilliseconds The amount of time before the shutdown the AutoGrader
     *                            will show a warning to students
     */
    public record ShutdownConfig(
            String timestamp,
            Integer warningMilliseconds
    ){ }
}
