package com.iortatechnxt.brokerverse.workflow.domain;

/**
 * Facts about the business record a case tracks.
 *
 * @param entityType entity type, e.g. "Account"
 * @param entityId entity id
 * @param reference business reference shown in queues (ARN, PRF number...)
 * @param title one-line description, e.g. insured name and product
 * @param link frontend route of the record
 * @param originatingUnit originating unit (segment, branch or team) shown to Processing
 */
public record CaseRecord(
    String entityType,
    String entityId,
    String reference,
    String title,
    String link,
    String originatingUnit) {}
