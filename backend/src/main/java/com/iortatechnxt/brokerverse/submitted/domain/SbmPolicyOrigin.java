package com.iortatechnxt.brokerverse.submitted.domain;

import java.time.LocalDate;

/**
 * Where a new masterlist record comes from (BRIDSP-01, 33).
 *
 * @param sourceCode source register code
 * @param intakeRunId intake run, may be null
 * @param dateReceived date received
 * @param status first status
 */
public record SbmPolicyOrigin(
    String sourceCode, Long intakeRunId, LocalDate dateReceived, SbmPolicyStatus status) {}
