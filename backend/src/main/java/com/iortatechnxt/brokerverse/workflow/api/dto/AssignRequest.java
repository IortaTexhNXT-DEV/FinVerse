package com.iortatechnxt.brokerverse.workflow.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Assignment.
 *
 * @param assignee user; null or blank releases the item to the team queue
 */
public record AssignRequest(@Size(max = 50) String assignee) {}
