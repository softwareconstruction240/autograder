package edu.byu.cs.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Set;

public class DateTimeUtils {

    /**
     * Generates a String representation of a timestamp
     *
     * @param timestamp a timestamp in Unix seconds
     * @param includeTime whether the time is included
     * @return a string formatted like "yyyy-MM-dd HH:mm:ss"
     */
    public static String getDateString(long timestamp, boolean includeTime) {
        ZonedDateTime zonedDateTime = Instant.ofEpochSecond(timestamp).atZone(ZoneId.of("America/Denver"));
        String pattern = includeTime ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return zonedDateTime.format(formatter);
    }

    /**
     * Gets the number of days late the submission is. This excludes weekends and public holidays
     *
     * @param handInDate the date the submission was handed in
     * @param dueDate    the due date of the phase
     * @return the number of days late or 0 if the submission is not late
     */
    public static int getNumDaysLate(ZonedDateTime handInDate, ZonedDateTime dueDate) {
        // end of day
        dueDate = dueDate.withHour(23).withMinute(59).withSecond(59);

        int daysLate = 0;

        while (handInDate.isAfter(dueDate)) {
            if (handInDate.getDayOfWeek().getValue() < 6 && !isPublicHoliday(handInDate)) {
                daysLate++;
            }
            handInDate = handInDate.minusDays(1);
        }

        return daysLate;
    }

    /**
     * Checks if the given date is a public holiday
     *
     * @param zonedDateTime the date to check
     * @return true if the date is a public holiday, false otherwise
     */
    private static boolean isPublicHoliday(ZonedDateTime zonedDateTime) {
        Date date = Date.from(zonedDateTime.toInstant());
        // TODO: use non-hardcoded list of public holidays
        Set<Date> publicHolidays = Set.of(
                Date.from(ZonedDateTime.parse("2023-02-19T00:00:00.000Z").toInstant()),
                Date.from(ZonedDateTime.parse("2023-03-15T00:00:00.000Z").toInstant())
        );

        return publicHolidays.contains(date);
    }
}
