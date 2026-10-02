package com.iortatechnxt.brokerverse.adjustment.api.dto;

import jakarta.validation.constraints.Size;

/**
 * A comment given with an action (submit, validate, approve, post).
 *
 * @param comment comment, may be null
 */
public record CommentInput(@Size(max = 1000) String comment) {}
