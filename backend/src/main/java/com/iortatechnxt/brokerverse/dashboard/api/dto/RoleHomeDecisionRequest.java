package com.iortatechnxt.brokerverse.dashboard.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Rejection of a role home change; the reason is mandatory unless the requester withdraws it.
 *
 * @param reason why
 */
public record RoleHomeDecisionRequest(@Size(max = 400) String reason) {}
