package com.iortatechnxt.brokerverse.configpromo.api.dto;

import jakarta.validation.constraints.Size;

/**
 * The approver's remarks.
 *
 * @param note remarks, optional on approval
 */
public record DecisionRequest(@Size(max = 1000) String note) {}
