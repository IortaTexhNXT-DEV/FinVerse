package com.iortatechnxt.brokerverse.workflow.service;

/**
 * Open items of one stage the current user works.
 *
 * @param workflowCode workflow
 * @param stageCode stage
 * @param stageName stage name
 * @param open open items
 * @param overdue items past due
 * @param mine items assigned to me
 */
public record QueueCount(
    String workflowCode, String stageCode, String stageName, long open, long overdue, long mine) {}
