package com.iortatechnxt.brokerverse.submitted.domain;

/**
 * The tracking fields a handler maintains on a masterlist record (BRIDSP-29).
 *
 * @param handlerUsername handler
 * @param aoUsername account officer
 * @param conversionStatus conversion status (LOV SBM_CONVERSION_STATUS)
 * @param opportunityTag opportunity tag
 * @param remarks remarks
 */
public record SbmTracking(
    String handlerUsername,
    String aoUsername,
    String conversionStatus,
    String opportunityTag,
    String remarks) {}
