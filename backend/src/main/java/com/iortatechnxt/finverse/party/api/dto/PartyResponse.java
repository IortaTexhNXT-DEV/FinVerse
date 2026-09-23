package com.iortatechnxt.finverse.party.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.domain.PartyType;
import java.math.BigDecimal;

/**
 * Party view.
 *
 * @param id id
 * @param companyId company
 * @param code code
 * @param name name
 * @param partyType type
 * @param taxId tax id
 * @param address address
 * @param email email
 * @param phone phone
 * @param defaultCurrency currency
 * @param creditDays credit days
 * @param commissionRate commission %
 * @param withholdingTaxRate withholding tax %
 * @param licenceNo licence no.
 * @param bankName bank
 * @param bankAccountNo bank account
 * @param branchId servicing branch
 * @param recordStatus maker-checker status
 * @param createdBy creator
 * @param maker user who created or last maintained the record (unchanged by authorization)
 * @param authorizedBy checker
 */
public record PartyResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    PartyType partyType,
    String taxId,
    String address,
    String email,
    String phone,
    String defaultCurrency,
    int creditDays,
    BigDecimal commissionRate,
    BigDecimal withholdingTaxRate,
    String licenceNo,
    String bankName,
    String bankAccountNo,
    Long branchId,
    RecordStatus recordStatus,
    String createdBy,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param p party
   * @return response
   */
  public static PartyResponse from(Party p) {
    return new PartyResponse(
        p.getId(),
        p.getCompanyId(),
        p.getCode(),
        p.getName(),
        p.getPartyType(),
        p.getTaxId(),
        p.getAddress(),
        p.getEmail(),
        p.getPhone(),
        p.getDefaultCurrency(),
        p.getCreditDays(),
        p.getCommissionRate(),
        p.getWithholdingTaxRate(),
        p.getLicenceNo(),
        p.getBankName(),
        p.getBankAccountNo(),
        p.getBranchId(),
        p.getRecordStatus(),
        p.getCreatedBy(),
        p.getMaker(),
        p.getAuthorizedBy());
  }
}
