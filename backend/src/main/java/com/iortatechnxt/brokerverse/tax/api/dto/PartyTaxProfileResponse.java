package com.iortatechnxt.brokerverse.tax.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.tax.domain.PartyTaxProfile;
import com.iortatechnxt.brokerverse.tax.domain.PayeeClass;
import com.iortatechnxt.brokerverse.tax.domain.VatTreatment;
import java.time.LocalDate;

/**
 * Party tax profile.
 *
 * @param id id
 * @param companyId company
 * @param partyCode party
 * @param tin 9-digit TIN
 * @param branchCode TIN branch code
 * @param payeeClass individual or corporate
 * @param registeredName registered name
 * @param lastName last name
 * @param firstName first name
 * @param middleName middle name
 * @param registeredAddress registered address
 * @param zipCode ZIP code
 * @param vatTreatment VAT treatment
 * @param defaultAtcCode default withholding tax code
 * @param withholdingAgent the party withholds tax on its payments to the company
 * @param topWithholdingAgent top withholding agent
 * @param governmentPayor government payor (final VAT and final tax withheld)
 * @param exemptionCertificateNo tax exemption certificate number
 * @param exemptionValidFrom first day of the certificate
 * @param exemptionValidTo last day of the certificate
 * @param recordStatus maker-checker status
 * @param maker last maintainer
 */
public record PartyTaxProfileResponse(
    Long id,
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
    String defaultAtcCode,
    boolean withholdingAgent,
    boolean topWithholdingAgent,
    boolean governmentPayor,
    String exemptionCertificateNo,
    LocalDate exemptionValidFrom,
    LocalDate exemptionValidTo,
    RecordStatus recordStatus,
    String maker) {

  /**
   * Maps an entity.
   *
   * @param p profile
   * @return response
   */
  public static PartyTaxProfileResponse from(PartyTaxProfile p) {
    return new PartyTaxProfileResponse(
        p.getId(),
        p.getCompanyId(),
        p.getPartyCode(),
        p.getTin(),
        p.getBranchCode(),
        p.getPayeeClass(),
        p.getRegisteredName(),
        p.getLastName(),
        p.getFirstName(),
        p.getMiddleName(),
        p.getRegisteredAddress(),
        p.getZipCode(),
        p.getVatTreatment(),
        p.getDefaultAtcCode(),
        p.getTaxStatus().withholdingAgent(),
        p.getTaxStatus().topWithholdingAgent(),
        p.getTaxStatus().governmentPayor(),
        p.getTaxStatus().exemptionCertificateNo(),
        p.getTaxStatus().exemptionValidFrom(),
        p.getTaxStatus().exemptionValidTo(),
        p.getRecordStatus(),
        p.getMaker());
  }
}
