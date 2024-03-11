package edu.byu.cs.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilsTest {

    @Test
    void getNumDaysLate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a z");
        ZonedDateTime dueDate = ZonedDateTime.parse("2024-03-07 11:59:00 PM -07:00", formatter);
        ExpectedDaysLate[] expectedDaysLate = {
                // On time submissions
                new ExpectedDaysLate("2024-02-03 02:00:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-03-03 02:00:00 PM -17:00", 0),
                new ExpectedDaysLate("2024-03-06 11:59:00 AM -07:00", 0),
                new ExpectedDaysLate("2024-03-07 03:00:00 PM -07:00", 0),
                new ExpectedDaysLate("2024-03-08 01:12:00 AM -05:00", 0),

                // Edge case, late submissions
                new ExpectedDaysLate("2024-03-07 11:59:13 PM -07:00", 1),
                new ExpectedDaysLate("2024-03-07 11:59:45 PM -07:00", 1),

                // Late submissions (1 day to 1 week)
                new ExpectedDaysLate("2024-03-08 12:15:00 AM -07:00", 1),
                new ExpectedDaysLate("2024-03-08 08:15:00 AM -07:00", 1),
                new ExpectedDaysLate("2024-03-08 02:15:00 PM -07:00", 1),
                new ExpectedDaysLate("2024-03-08 11:58:45 PM -07:00", 1),

                new ExpectedDaysLate("2024-03-08 11:59:03 PM -07:00", 2),
                new ExpectedDaysLate("2024-03-09 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2024-03-10 02:15:00 PM -07:00", 2),
                new ExpectedDaysLate("2024-03-11 02:15:00 PM -07:00", 2),

                new ExpectedDaysLate("2024-03-12 02:15:00 PM -07:00", 3),
                new ExpectedDaysLate("2024-03-13 02:15:00 PM -07:00", 4),
                new ExpectedDaysLate("2024-03-14 02:15:00 PM -07:00", 5),

                // Late submissions (1 week to 3 weeks)
                new ExpectedDaysLate("2024-03-15 02:15:00 PM -07:00", 6),
                new ExpectedDaysLate("2024-03-16 02:15:00 PM -07:00", 7),
                new ExpectedDaysLate("2024-03-17 02:15:00 PM -07:00", 7),
                new ExpectedDaysLate("2024-03-18 02:15:00 PM -07:00", 7),
                new ExpectedDaysLate("2024-03-19 02:15:00 PM -07:00", 8),
                new ExpectedDaysLate("2024-03-20 02:15:00 PM -07:00", 9),
                new ExpectedDaysLate("2024-03-21 02:15:00 PM -07:00", 10),
                new ExpectedDaysLate("2024-03-22 02:15:00 PM -07:00", 11),
                new ExpectedDaysLate("2024-03-23 02:15:00 PM -07:00", 12),
                new ExpectedDaysLate("2024-03-24 02:15:00 PM -07:00", 12),
                new ExpectedDaysLate("2024-03-25 02:15:00 PM -07:00", 12),

                // Late submissions (1+ months)
                new ExpectedDaysLate("2024-04-15 02:15:00 PM -07:00", 27),
        };

        DateTimeUtils dateTimeUtils = new DateTimeUtils();
        dateTimeUtils.initializePublicHolidays();

        // Evaluate all the test cases above
        ZonedDateTime handInTime;
        for (var expectedResult : expectedDaysLate) {
            handInTime = ZonedDateTime.parse(expectedResult.handInDate, formatter);
            Assertions.assertEquals(expectedResult.daysLate, dateTimeUtils.getNumDaysLate(handInTime, dueDate));
        }
    }

    private record ExpectedDaysLate(String handInDate, int daysLate){}
}
