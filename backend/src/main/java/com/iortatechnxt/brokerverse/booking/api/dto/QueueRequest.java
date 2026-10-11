package com.iortatechnxt.brokerverse.booking.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * Accounts to queue for the next batch, or to book now.
 *
 * @param companyId company
 * @param arns Account Reference Numbers
 * @param bookingDate booking date for "Book now", null for today
 */
public record QueueRequest(
    @NotNull Long companyId, @NotEmpty List<String> arns, LocalDate bookingDate) {}
