package edu.byu.cs.model.request;

import java.util.List;

/**
 * Represents a request for updating the list of holidays
 * (days the AutoGrader should not count towards the late penalty)
 *
 * @param holidays the new list of holidays to apply
 */
public record ConfigHolidayUpdateRequest(
        List<String> holidays
) {}
