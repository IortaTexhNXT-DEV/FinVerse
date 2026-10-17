package com.iortatechnxt.brokerverse.account.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Free First Year tag (BRNB.113).
 *
 * @param start FFY start (the end is one year later)
 */
public record FfyRequest(@NotNull LocalDate start) {}
