package com.iortatechnxt.brokerverse.journal.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Journals selected for a bulk action (FRBS 2.5.6).
 *
 * @param ids journals
 */
public record JournalIdsRequest(@NotEmpty @Size(max = 200) List<@NotNull Long> ids) {}
