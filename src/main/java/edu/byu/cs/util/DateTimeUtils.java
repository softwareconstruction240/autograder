package edu.byu.cs.util;

import org.eclipse.jgit.annotations.NonNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Has helper methods for handling dates.
 */
public class DateTimeUtils {

    /**
     * Generates a String representation of a timestamp in American/Denver time zone.
     *
     * @param timestamp a timestamp in Unix seconds
     * @param includeTime whether the time is included
     * @return a string formatted like "yyyy-MM-dd HH:mm:ss"
     */
    public static String getDateString(@NonNull long timestamp, boolean includeTime) {
        ZonedDateTime zonedDateTime = Instant.ofEpochSecond(timestamp).atZone(ZoneId.of("America/Denver"));
        String pattern = includeTime ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return zonedDateTime.format(formatter);
    }
}
