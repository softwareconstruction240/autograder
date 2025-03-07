package edu.byu.cs.model.request;

import java.util.List;

/**
 * Represents a request for updating the list of holidays
 * (days the AutoGrader should not count towards the late penalty).
 * The format of the holiday dates should be YYYY-MM-DD.
 *
 * @param holidays the new list of holidays to apply
 */
public record ConfigHolidayUpdateRequest(
        List<String> holidays
) {}
