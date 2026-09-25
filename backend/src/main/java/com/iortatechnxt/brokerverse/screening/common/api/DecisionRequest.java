package com.iortatechnxt.brokerverse.screening.common.api;

import jakarta.validation.constraints.Size;

/**
 * Remarks of a checker decision (rejection reason of a configuration version or list change). A
 * blank value is refused by the service with the FRS message ("Enter the reason for the
 * rejection").
 *
 * @param remarks the remarks
 */
public record DecisionRequest(@Size(max = 1000) String remarks) {}
