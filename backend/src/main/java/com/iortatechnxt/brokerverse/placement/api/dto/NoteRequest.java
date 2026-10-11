package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.Size;

/**
 * An optional comment or reference with an action.
 *
 * @param comment comment, insurer reference or remarks
 */
public record NoteRequest(@Size(max = 1000) String comment) {}
