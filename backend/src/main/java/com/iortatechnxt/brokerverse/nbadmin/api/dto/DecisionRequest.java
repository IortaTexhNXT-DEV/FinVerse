package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Approver's comment on a decision (mandatory to reject).
 *
 * @param comment comment
 */
public record DecisionRequest(@Size(max = 1000) String comment) {}
