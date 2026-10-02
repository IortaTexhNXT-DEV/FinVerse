package com.iortatechnxt.brokerverse.quotation.service;

import java.time.Instant;

/**
 * A quotation request as received from Marketing, a bulk upload or a source system (BRNB.041/023).
 *
 * @param channel source channel (list SOURCE_CHANNEL: EMAIL, HLS, UPLOAD...)
 * @param externalRef reference in the source system (duplicates are ignored), may be null
 * @param receivedAt time received; null for now
 * @param clientCode client or prospect code of an existing client, may be null
 * @param prospectName prospect name when there is no client code ("Last, First" for a person)
 * @param prospectEmail prospect e-mail
 * @param prospectMobile prospect mobile
 * @param productCode requested product, may be null
 * @param marketSegment market segment
 * @param requestedCover requested cover as described by the requester
 */
public record IncomingQuotationRequest(
    String channel,
    String externalRef,
    Instant receivedAt,
    String clientCode,
    String prospectName,
    String prospectEmail,
    String prospectMobile,
    String productCode,
    String marketSegment,
    String requestedCover) {}
