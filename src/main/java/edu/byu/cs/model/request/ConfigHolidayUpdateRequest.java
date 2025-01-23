package edu.byu.cs.model.request;

import java.time.LocalDate;
import java.util.List;

public record ConfigHolidayUpdateRequest(
        List<LocalDate> holidays
) {}
