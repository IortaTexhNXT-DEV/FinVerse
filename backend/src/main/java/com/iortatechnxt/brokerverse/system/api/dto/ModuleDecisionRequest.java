package com.iortatechnxt.brokerverse.system.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Rejection of a module change; the reason is mandatory unless the requester withdraws it.
 *
 * @param reason why
 */
public record ModuleDecisionRequest(@Size(max = 400) String reason) {}
