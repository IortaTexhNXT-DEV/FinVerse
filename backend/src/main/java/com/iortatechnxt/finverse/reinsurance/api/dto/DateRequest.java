package com.iortatechnxt.finverse.reinsurance.api.dto;

import java.time.LocalDate;

/**
 * Optional business date of an action (placement or closing date); blank = today.
 *
 * @param date date
 */
public record DateRequest(LocalDate date) {}
