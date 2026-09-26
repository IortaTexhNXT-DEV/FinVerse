package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.payables.domain.PettyCashFund;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Petty cash fund view.
 *
 * @param id id
 * @param companyId company
 * @param branchId branch
 * @param code code
 * @param name name
 * @param custodian custodian
 * @param glAccountCode GL account
 * @param replenishBankAccountId replenishment bank account
 * @param currency currency
 * @param imprestAmount imprest (box limit)
 * @param cashBalance cash in the box
 * @param pendingReimbursement vouchers paid and not yet reimbursed (imprest - cash)
 * @param establishedOn establishment date
 * @param recordStatus maker-checker status
 * @param createdBy creator
 * @param maker user who created or last maintained the record (unchanged by authorization)
 * @param authorizedBy checker
 */
public record FundResponse(
    Long id,
    Long companyId,
    Long branchId,
    String code,
    String name,
    String custodian,
    String glAccountCode,
    Long replenishBankAccountId,
    String currency,
    BigDecimal imprestAmount,
    BigDecimal cashBalance,
    BigDecimal pendingReimbursement,
    LocalDate establishedOn,
    RecordStatus recordStatus,
    String createdBy,
    String maker,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param f fund
   * @return response
   */
  public static FundResponse from(PettyCashFund f) {
    return new FundResponse(
        f.getId(),
        f.getCompanyId(),
        f.getBranchId(),
        f.getCode(),
        f.getName(),
        f.getCustodian(),
        f.getGlAccountCode(),
        f.getReplenishBankAccountId(),
        f.getCurrency(),
        f.getImprestAmount(),
        f.getCashBalance(),
        f.getEstablishedOn() == null
            ? BigDecimal.ZERO
            : f.getImprestAmount().subtract(f.getCashBalance()),
        f.getEstablishedOn(),
        f.getRecordStatus(),
        f.getCreatedBy(),
        f.getMaker(),
        f.getAuthorizedBy());
  }
}
