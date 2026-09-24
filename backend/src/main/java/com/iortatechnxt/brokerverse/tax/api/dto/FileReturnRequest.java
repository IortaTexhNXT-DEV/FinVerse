package com.iortatechnxt.brokerverse.tax.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request to record the filing of a return.
 *
 * @param filedOn filing date
 * @param reference eFPS / eBIRForms confirmation number
 */
public record FileReturnRequest(
    @NotNull LocalDate filedOn, @NotBlank @Size(max = 60) String reference) {}
