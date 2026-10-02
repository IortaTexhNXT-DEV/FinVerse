package com.iortatechnxt.brokerverse.underwriting.api.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Options for converting an approved quotation into a draft policy.
 *
 * @param issueDate policy issue date (default: today)
 * @param coinsurerCode coinsurer party, required when the quoted share is below 100 %
 * @param coinsuranceLeader company leads the coinsurance
 * @param riskDescription description of the risk created from the quotation
 */
public record ConvertQuotationRequest(
    LocalDate issueDate,
    @Size(max = 30) String coinsurerCode,
    boolean coinsuranceLeader,
    @Size(max = 300) String riskDescription) {}
