package com.iortatechnxt.brokerverse.tax.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;

/**
 * Withholding status and tax exemption of a party (BDOI inputs TX-Q06, TX-Q09; template TX-01): a
 * withholding agent withholds tax on its payments to the company and issues 2307 certificates; a
 * top withholding agent or a government payor is always a withholding agent, and a government payor
 * also withholds final VAT and final tax. The tax exemption certificate supports a zero-rated or
 * exempt VAT treatment for its validity.
 *
 * @param withholdingAgent the party withholds tax on its payments to the company
 * @param topWithholdingAgent the party is a top withholding agent
 * @param governmentPayor the party is a government office or corporation that withholds final VAT
 *     and final tax
 * @param exemptionCertificateNo number of the tax exemption certificate, null when none
 * @param exemptionValidFrom first day of the certificate
 * @param exemptionValidTo last day of the certificate
 */
@Embeddable
public record PartyTaxStatus(
    @Column(name = "withholding_agent", nullable = false) boolean withholdingAgent,
    @Column(name = "top_withholding_agent", nullable = false) boolean topWithholdingAgent,
    @Column(name = "government_payor", nullable = false) boolean governmentPayor,
    @Column(name = "exemption_certificate_no", length = 40) String exemptionCertificateNo,
    @Column(name = "exemption_valid_from") LocalDate exemptionValidFrom,
    @Column(name = "exemption_valid_to") LocalDate exemptionValidTo) {

  /** No withholding and no exemption certificate. */
  public static final PartyTaxStatus NONE =
      new PartyTaxStatus(false, false, false, null, null, null);

  /**
   * Normalises and checks the status: a top withholding agent or a government payor is a
   * withholding agent; a certificate needs its validity, which ends on or after it starts.
   *
   * @return the status to keep
   */
  public PartyTaxStatus checked() {
    String certificate =
        exemptionCertificateNo == null || exemptionCertificateNo.isBlank()
            ? null
            : exemptionCertificateNo.strip();
    boolean agent = withholdingAgent || topWithholdingAgent || governmentPayor;
    if (certificate == null) {
      return new PartyTaxStatus(agent, topWithholdingAgent, governmentPayor, null, null, null);
    }
    requireValidity();
    return new PartyTaxStatus(
        agent,
        topWithholdingAgent,
        governmentPayor,
        certificate,
        exemptionValidFrom,
        exemptionValidTo);
  }

  private void requireValidity() {
    if (exemptionValidFrom == null || exemptionValidTo == null) {
      throw new BusinessRuleException(
          "EXEMPTION_VALIDITY_REQUIRED",
          "Enter the first and last day of the tax exemption certificate");
    }
    if (exemptionValidTo.isBefore(exemptionValidFrom)) {
      throw new BusinessRuleException(
          "EXEMPTION_VALIDITY_INVALID", "The tax exemption certificate ends before it starts");
    }
  }

  /**
   * Whether an exemption certificate is recorded.
   *
   * @return true when a certificate number is kept
   */
  public boolean hasExemptionCertificate() {
    return exemptionCertificateNo != null;
  }

  /**
   * Whether the exemption certificate covers a date.
   *
   * @param date date of the transaction
   * @return true when a certificate is recorded and valid on the date
   */
  public boolean exemptionValidOn(LocalDate date) {
    return hasExemptionCertificate()
        && !date.isBefore(exemptionValidFrom)
        && !date.isAfter(exemptionValidTo);
  }
}
