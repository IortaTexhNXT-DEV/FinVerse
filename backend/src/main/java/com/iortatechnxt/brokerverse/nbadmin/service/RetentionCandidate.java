package com.iortatechnxt.brokerverse.nbadmin.service;

import java.time.LocalDate;

/**
 * A record eligible under a retention rule.
 *
 * @param reference business reference (client code, ARN...)
 * @param description one-line description
 * @param status record status
 * @param lastActivity date of the last change
 * @param link frontend route of the record
 */
public record RetentionCandidate(
    String reference, String description, String status, LocalDate lastActivity, String link) {}
