package com.iortatechnxt.brokerverse.workflow.service;

/**
 * Published synchronously, inside the transaction, after every stage change (user, system or
 * generic action). Business modules listen with {@code @EventListener}, filter on {@code
 * entityType} and mirror the stage on their record (e.g. an account's status).
 *
 * @param caseId case
 * @param workflowCode workflow
 * @param entityType record type
 * @param entityId record id
 * @param fromStage previous stage
 * @param toStage new stage
 * @param action action code
 * @param reasonCode reason
 * @param comment comment
 */
public record WorkCaseTransitioned(
    Long caseId,
    String workflowCode,
    String entityType,
    String entityId,
    String fromStage,
    String toStage,
    String action,
    String reasonCode,
    String comment) {}
