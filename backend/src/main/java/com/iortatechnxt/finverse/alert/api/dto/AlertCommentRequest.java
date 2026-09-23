package com.iortatechnxt.finverse.alert.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Optional comment when acknowledging or resolving an alert.
 *
 * @param comment comment
 */
public record AlertCommentRequest(@Size(max = 500) String comment) {}
