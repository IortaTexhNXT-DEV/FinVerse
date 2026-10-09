package com.iortatechnxt.brokerverse.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/**
 * Tax registration of the company (BDOI inputs TX-Q08, template D0-01): the Revenue District Office
 * (RDO) printed on the BIR exports and forms, the VAT registration, and the permits of the
 * computerised accounting system (CAS) and of the e-invoicing system.
 *
 * @param rdoCode Revenue District Office code, null when not given (the parameter TAX_RDO_CODE then
 *     applies)
 * @param vatRegistered the company is registered for VAT
 * @param casPermitNo permit to use the computerised accounting system
 * @param casPermitDate date of the CAS permit
 * @param einvoicingPermitNo e-invoicing permit or enrolment number
 * @param einvoicingPermitDate date of the e-invoicing permit
 */
@Embeddable
public record TaxRegistration(
    @Column(name = "rdo_code", length = 5) String rdoCode,
    @Column(name = "vat_registered", nullable = false) boolean vatRegistered,
    @Column(name = "cas_permit_no", length = 40) String casPermitNo,
    @Column(name = "cas_permit_date") LocalDate casPermitDate,
    @Column(name = "einvoicing_permit_no", length = 40) String einvoicingPermitNo,
    @Column(name = "einvoicing_permit_date") LocalDate einvoicingPermitDate) {

  /** Registered for VAT, nothing else recorded. */
  public static final TaxRegistration NONE =
      new TaxRegistration(null, true, null, null, null, null);

  /** Blank texts are kept as null. */
  public TaxRegistration {
    rdoCode = blankToNull(rdoCode);
    casPermitNo = blankToNull(casPermitNo);
    einvoicingPermitNo = blankToNull(einvoicingPermitNo);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
