package com.iortatechnxt.finverse.payables.api.dto;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.payables.domain.BankAccount;
import com.iortatechnxt.finverse.payables.domain.NotificationFormat;

/**
 * Bank account view.
 *
 * @param id id
 * @param companyId company
 * @param code code
 * @param name name
 * @param bankPartyCode bank party code
 * @param bankName bank name
 * @param accountNo account number
 * @param currency currency
 * @param glAccountCode bank GL account
 * @param pdcClearingAccountCode PDC clearing account
 * @param branchId branch
 * @param notificationFormat notification layout
 * @param recordStatus maker-checker status
 * @param createdBy maker
 * @param authorizedBy checker
 */
public record BankAccountResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    String bankPartyCode,
    String bankName,
    String accountNo,
    String currency,
    String glAccountCode,
    String pdcClearingAccountCode,
    Long branchId,
    NotificationFormat notificationFormat,
    RecordStatus recordStatus,
    String createdBy,
    String authorizedBy) {

  /**
   * Maps an entity.
   *
   * @param a account
   * @return response
   */
  public static BankAccountResponse from(BankAccount a) {
    return new BankAccountResponse(
        a.getId(),
        a.getCompanyId(),
        a.getCode(),
        a.getName(),
        a.getBankPartyCode(),
        a.getBankName(),
        a.getAccountNo(),
        a.getCurrency(),
        a.getGlAccountCode(),
        a.getPdcClearingAccountCode(),
        a.getBranchId(),
        a.getNotificationFormat(),
        a.getRecordStatus(),
        a.getCreatedBy(),
        a.getAuthorizedBy());
  }
}
