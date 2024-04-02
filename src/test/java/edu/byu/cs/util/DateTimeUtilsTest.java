package edu.byu.cs.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilsTest {

    @Test
    void getDateString__withIncludeTime() {
        assertEquals("2024-02-01 01:02:03", DateTimeUtils.getDateString(1706774523, true));
    }

    @Test
    void getDateString__withoutIncludeTime() {
        assertEquals("2024-02-01", DateTimeUtils.getDateString(1706774523, false));
    }
}