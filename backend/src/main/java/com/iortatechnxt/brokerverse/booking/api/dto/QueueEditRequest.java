package com.iortatechnxt.brokerverse.booking.api.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Booking date and cost center of a queued account.
 *
 * @param bookingDate booking date, null for the batch date
 * @param costCenter cost center, null for the account's
 */
public record QueueEditRequest(LocalDate bookingDate, @Size(max = 20) String costCenter) {}
