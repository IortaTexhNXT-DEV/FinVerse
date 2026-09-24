package com.iortatechnxt.brokerverse.underwriting.api.dto;

import java.time.LocalDate;

/**
 * Approval options.
 *
 * @param accountingDate accounting (approval) date of the premium posting; default today
 */
public record ApprovalRequest(LocalDate accountingDate) {}
