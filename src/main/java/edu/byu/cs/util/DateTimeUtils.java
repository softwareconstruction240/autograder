package edu.byu.cs.util;

import org.eclipse.jgit.annotations.NonNull;
import org.eclipse.jgit.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Has helper methods for handling dates.
 * Note that some methods are available statically.
 * but some methods require configuration ahead of time and are available
 * only on instances of the class.
 * TODO: Design a more intentional DateTimeUtils API for consistently referencing methods.
 */
public class DateTimeUtils {
    private Set<LocalDate> publicHolidays;

    /**
     * Generates a String representation of a timestamp
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

    /**
     * Gets the number of days late the submission is. This excludes weekends and public holidays.
     *
     * In the event that the due date also happens to be a holiday, the assignment not receive
     * a late penalty until AFTER the holiday and weekends following it. While this behavior
     * may be surprising, it can be controlled by professors being careful to never assign a
     * due date on a holiday. This behavior is illustrated in the provided test cases.
     *
     * @param handInDate the date the submission was handed in
     * @param dueDate    the due date of the phase
     * @return the number of days late or 0 if the submission is not late
     */
    public int getNumDaysLate(@NonNull ZonedDateTime handInDate, @NonNull ZonedDateTime dueDate) {
        if (publicHolidays == null) {
            throw new RuntimeException("Public Holidays have not yet been initialized. "
                    + "Call `dateTimeUtils.initializePublicHolidays()` before attempting to count the days late.");
        }

        int daysLate = 0;

        while (handInDate.isAfter(dueDate)) {
            if (dueDate.getDayOfWeek().getValue() < 6 && !isPublicHoliday(dueDate)) {
                daysLate++;
            }
            dueDate = dueDate.plusDays(1);
        }

        return daysLate;
    }

    /**
     * Initializes the public holidays to an empty value.
     * Useful for testing when it's recognized that no holidays exist.
     */
    public void initializePublicHolidays() {
        publicHolidays = new HashSet<>();
    }
    /**
     * Initializes our public holidays with a common formatting string
     * that will accept strings matching this example: "9/16/2024"
     *
     * @see #initializePublicHolidays(String, String)
     *
     * @param encodedPublicHolidays A string representing the encoded data.
     */
    public Set<LocalDate> initializePublicHolidays(@NonNull String encodedPublicHolidays) {
        return initializePublicHolidays(encodedPublicHolidays, "M/d/yyyy");
    }

    /**
     * Initializes our internal public holidays data set with the given string and a given date format.
     * This method also returns the constructed set of dates.
     *
     * This must be called before calls to {@code getNumDaysLate()} will properly account for the holidays.
     *
     * @see #interpretPublicHolidays(String, String)
     *
     * @param encodedPublicHolidays @non-nullable A string representing the configured data
     * @param dateEncodingFormat A string representing the intended date format within the data
     */
    public Set<LocalDate> initializePublicHolidays(@NonNull String encodedPublicHolidays, @NonNull String dateEncodingFormat) {
        if (encodedPublicHolidays == null || dateEncodingFormat == null) {
            throw new RuntimeException("Error initializing public holidays. Received null as a parameter. " +
                    "If some data isn't available, explicitly call the no argument initializer instead.");
        }

        publicHolidays = interpretPublicHolidays(encodedPublicHolidays, dateEncodingFormat);
        // TODO: Validate that some holidays are configured for the current calendar year and throw an error otherwise
        return publicHolidays;
    }

    /**
     * <h1>Public Holiday Decoding</h1>
     * Interprets a string of encoded public holidays and returns a set of objects that
     * can be used to efficiently query against the dataset.
     * <br>
     *
     * <h2>Encoding Approaches</h2>
     * The input should be only one of the two approaches. It should either be a single-line entry with
     * zero or more dates, or it should be a multi-line string which can include comments following the date string.
     * <br>
     *
     * <h3>Single Line Encoding</h3>
     * Dates can be stored on a single line, separated by any combination of the following:
     * <ul>
     *  <li>spaces " ", or</li>
     *  <li>commas ",", or </li>
     *  <li>semicolon ";" </li>
     * </ul>
     * <br>
     *
     * Each word will be interpreted individually, and any that do not parse into a date properly
     * will be excluded individually.
     * <br>
     *
     * <h3>Multi-line Encoding</h3>
     * Additionally, the dates can each appear on a separate line.
     * Only the first word of the line will be interpreted as a date,
     * and the rest of the line will be ignored as a comment.
     * Any line that does not contain a date format at the beginning
     * (i.e. an empty line) will be ignored and not considered for interpretation.
     * Lines beginning with {@code #} are always ignored as a comment; however, a comment
     * need not begin with a special symbol. Lines where a date cannot be interpreted
     * from the first word will simply be skipped.
     * <br>
     *
     * <h2>Encoding Date Formats</h2>
     * Since both approaches use whitespace to delimit words, no acceptable date format can ever contain
     * a whitespace character. This is the only restraint for the multi-line approach; however,
     * however, in the single line approach, the format cannot contain any of the delimiting characters.
     * <br>
     *
     * @param encodedPublicHolidays A string representing the public holidays.
     *                              This could be read in from a file or from a cell in a table
     *                              depending on the most convenient form of maintaining this information.
     * @param dateEncodingFormat A string representing the date format.
     *                           This will be interpreted by {@link DateTimeFormatter}.
     * @return A {@code Set<Date>} that will be used to efficiently comparing against this dataset
     */
    private Set<LocalDate> interpretPublicHolidays(@Nullable String encodedPublicHolidays, @NonNull String dateEncodingFormat) {
        String[] dateStrings;
        if (encodedPublicHolidays == null) {
            dateStrings = new String[]{};
        } else if (encodedPublicHolidays.contains("\n")) {
            dateStrings = extractPublicHolidaysMultiline(encodedPublicHolidays);
        } else {
            dateStrings = extractPublicHolidaysSingleline(encodedPublicHolidays);
        }

        return parsePublicHolidayStrings(dateEncodingFormat, dateStrings);
    }
    private String[] extractPublicHolidaysSingleline(String singleLineEncodedHolidays) {
        String DELIMITERS = " ,;";
        return singleLineEncodedHolidays.split("["+DELIMITERS+"]");
    }
    private String[] extractPublicHolidaysMultiline(String multilineEncodedHolidays) {
        ArrayList<String> holidays = new ArrayList<>();
        Scanner scanner = new Scanner(multilineEncodedHolidays);
        while (scanner.hasNext()) {
            holidays.add(scanner.next()); // The first word in interpreted as a date
            scanner.nextLine(); // Skip the rest of the line
        }
        return holidays.toArray(new String[0]);
    }
    private Set<LocalDate> parsePublicHolidayStrings(@NonNull String dateFormat, String[] holidayDateStrings) {
        Set<LocalDate> publicHolidays = new HashSet<>();
        var parser = DateTimeFormatter.ofPattern(dateFormat);
        for (var holidayDateString : holidayDateStrings) {
            if (holidayDateString.isEmpty()) {
                continue; // Empty line
            }
            if (holidayDateString.charAt(0) == '#') {
                continue; // Explicitly marked as a comment
            }
            try {
                publicHolidays.add(parser.parse(holidayDateString, LocalDate::from));
            } catch (DateTimeParseException e) {
                System.out.println("Skipping unrecognized date string: " + holidayDateString);
            }
        }
        return publicHolidays;
    }

    /**
     * Checks if the given date is a public holiday
     *
     * @param zonedDateTime the date to check
     * @return true if the date is a public holiday, false otherwise
     */
    private boolean isPublicHoliday(@NonNull ZonedDateTime zonedDateTime) {
        if (publicHolidays == null) {
//            return false; // Holidays have not been initialized
            throw new RuntimeException("Holiday settings have not been initialized properly");
        }

        LocalDate date = LocalDate.of(zonedDateTime.getYear(), zonedDateTime.getMonthValue(), zonedDateTime.getDayOfMonth());
        return publicHolidays.contains(date);
    }
}
