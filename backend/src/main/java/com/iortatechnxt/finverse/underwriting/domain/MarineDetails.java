package com.iortatechnxt.finverse.underwriting.domain;

import java.time.LocalDate;

/**
 * Shipment details of a marine cargo risk (certificate under an open cover).
 *
 * @param vesselName carrying vessel
 * @param voyageFrom port of loading
 * @param voyageTo port of discharge
 * @param sailDate sailing date
 * @param blNo bill of lading number
 * @param blDate bill of lading date
 * @param lcNo letter of credit number
 * @param bankName LC issuing bank
 * @param valuationBasis basis of valuation, e.g. "CIF + 10%"
 */
public record MarineDetails(
    String vesselName,
    String voyageFrom,
    String voyageTo,
    LocalDate sailDate,
    String blNo,
    LocalDate blDate,
    String lcNo,
    String bankName,
    String valuationBasis) {}
