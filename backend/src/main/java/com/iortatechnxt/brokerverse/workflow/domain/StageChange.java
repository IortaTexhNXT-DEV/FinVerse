package com.iortatechnxt.brokerverse.workflow.domain;

/**
 * A stage change.
 *
 * @param fromStage previous stage (null when the case opens)
 * @param toStage new stage
 * @param action action code
 * @param reasonCode reason (list of values), may be null
 * @param comment free text, may be null
 */
public record StageChange(
    String fromStage, String toStage, String action, String reasonCode, String comment) {}
