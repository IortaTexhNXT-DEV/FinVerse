package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.tax.domain.PayeeClass;
import com.iortatechnxt.brokerverse.tax.domain.VatTreatment;

/**
 * Values of a party tax profile.
 *
 * @param companyId company
 * @param partyCode party (immutable after creation)
 * @param tin TIN as entered (digits are extracted; 9 digits TIN, optional branch digits)
 * @param branchCode TIN branch code, null = from the TIN or 000
 * @param payeeClass individual or corporate
 * @param registeredName BIR registered name
 * @param lastName last name (individuals)
 * @param firstName first name (individuals)
 * @param middleName middle name (individuals)
 * @param registeredAddress registered address
 * @param zipCode ZIP code
 * @param vatTreatment VAT treatment
 * @param defaultAtcCode default withholding tax code, null when none
 */
public record PartyTaxProfileCommand(
    Long companyId,
    String partyCode,
    String tin,
    String branchCode,
    PayeeClass payeeClass,
    String registeredName,
    String lastName,
    String firstName,
    String middleName,
    String registeredAddress,
    String zipCode,
    VatTreatment vatTreatment,
    String defaultAtcCode) {}
