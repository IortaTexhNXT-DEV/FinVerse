package com.iortatechnxt.brokerverse.payables.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.BankAccountStatus;
import com.iortatechnxt.brokerverse.payables.domain.NotificationFormat;

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
 * @param createdBy creator
 * @param maker user who created or last maintained the record (unchanged by authorization)
 * @param authorizedBy checker
 * @param status operating status, active or inactive (DIS 2.24.2)
 * @param requestedStatus status waiting for authorisation, null when none
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
    String maker,
    String authorizedBy,
    BankAccountStatus status,
    BankAccountStatus requestedStatus) {

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
        a.getMaker(),
        a.getAuthorizedBy(),
        a.getStatus(),
        a.getRequestedStatus());
  }
}
