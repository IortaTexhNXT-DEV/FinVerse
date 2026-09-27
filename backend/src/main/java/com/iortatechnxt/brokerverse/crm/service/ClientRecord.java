package com.iortatechnxt.brokerverse.crm.service;

import java.time.LocalDate;

/**
 * A record linked to a client (client 360 view).
 *
 * @param kind record kind, e.g. "Account", "Quotation"
 * @param reference business reference (ARN, quotation number...)
 * @param description one-line description (product, insurer, sum insured...)
 * @param status status or stage code
 * @param date relevant date (created, inception...)
 * @param link frontend route of the record
 */
public record ClientRecord(
    String kind,
    String reference,
    String description,
    String status,
    LocalDate date,
    String link) {}
