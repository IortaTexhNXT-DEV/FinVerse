package com.iortatechnxt.brokerverse.docgen.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * A new template version.
 *
 * @param title title
 * @param body text with {{placeholders}}
 * @param effectiveFrom first date of use
 */
public record DocTemplateRequest(
    @NotBlank @Size(max = 200) String title,
    @NotBlank @Size(max = 20000) String body,
    @NotNull LocalDate effectiveFrom) {}
