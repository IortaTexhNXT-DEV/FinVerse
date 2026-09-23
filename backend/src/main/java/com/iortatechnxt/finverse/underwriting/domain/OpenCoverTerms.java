package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.party.domain.Party;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Terms of a marine open cover (master policy under which shipment certificates are declared).
 *
 * @param branchId issuing branch
 * @param product marine product allowing open covers
 * @param customer client
 * @param insuredName insured name
 * @param periodFrom cover start
 * @param periodTo cover end
 * @param currency currency of declarations
 * @param limitPerShipment maximum sum insured of one shipment
 * @param annualLimit maximum total sum insured declared in the period
 * @param rate premium rate % applied to each shipment
 * @param cargoDescription goods covered
 */
public record OpenCoverTerms(
    Long branchId,
    Product product,
    Party customer,
    String insuredName,
    LocalDate periodFrom,
    LocalDate periodTo,
    String currency,
    BigDecimal limitPerShipment,
    BigDecimal annualLimit,
    BigDecimal rate,
    String cargoDescription) {}
