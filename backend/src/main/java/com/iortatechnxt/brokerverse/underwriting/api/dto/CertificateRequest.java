package com.iortatechnxt.brokerverse.underwriting.api.dto;

import com.iortatechnxt.brokerverse.underwriting.domain.SourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Shipment declaration under an open cover; creates a draft certificate. The shipment is insured
 * from its sailing date for {@code transitDays}; the premium defaults to sum insured × cover rate.
 *
 * @param issueDate certificate issue date
 * @param transitDays days of cover from the sailing date (default 60)
 * @param sourceType channel (default DIRECT)
 * @param intermediaryCode agent or broker
 * @param shipment shipment risk (vessel, voyage, B/L, LC, valuation, sum insured)
 */
public record CertificateRequest(
    @NotNull LocalDate issueDate,
    Integer transitDays,
    SourceType sourceType,
    @Size(max = 30) String intermediaryCode,
    @NotNull @Valid RiskRequest shipment) {}
