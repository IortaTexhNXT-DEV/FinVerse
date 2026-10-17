package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * A hold cover request (BRNB.072).
 *
 * @param startDate first day; today when empty
 * @param to recipients; the insurer branch mailbox when empty
 */
public record HoldCoverRequestBody(LocalDate startDate, @Size(max = 20) List<String> to) {}
