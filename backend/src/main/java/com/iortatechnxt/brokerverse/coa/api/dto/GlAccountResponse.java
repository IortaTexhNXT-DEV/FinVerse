package com.iortatechnxt.brokerverse.coa.api.dto;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.AccountLevel;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.domain.NegativeBalancePolicy;
import com.iortatechnxt.brokerverse.coa.domain.SubLedgerType;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.time.LocalDate;
import java.util.Set;

/**
 * GL account view.
 *
 * @param id id
 * @param companyId company
 * @param code code
 * @param name name
 * @param shortName short name
 * @param accountClass class
 * @param level tier
 * @param parentCode parent code
 * @param categoryCode category code
 * @param postable postable flag
 * @param controlAccount control flag
 * @param subLedgerType sub-ledger
 * @param allowManualPosting manual posting flag
 * @param costCenterRequired cost centre required
 * @param businessLineRequired line of business required
 * @param revaluationRequired revaluation flag
 * @param reconcilable reconcilable flag
 * @param interBranch inter-branch flag
 * @param contraAccountCode contra account
 * @param reportGroup statement line
 * @param frozen frozen flag
 * @param freezeReason freeze reason
 * @param openedOn opening date
 * @param closedOn closure date
 * @param allowedCurrencies allowed currencies
 * @param allowedBranchIds allowed branches
 * @param allowedRoleCodes access codes
 * @param recordStatus maker-checker status
 * @param createdBy creator
 * @param maker user who created or last maintained the record (unchanged by authorization)
 * @param authorizedBy checker
 * @param negativeBalancePolicy negative balance control (FRBS 2.5.4)
 */
public record GlAccountResponse(
    Long id,
    Long companyId,
    String code,
    String name,
    String shortName,
    AccountClass accountClass,
    AccountLevel level,
    String parentCode,
    String categoryCode,
    boolean postable,
    boolean controlAccount,
    SubLedgerType subLedgerType,
    boolean allowManualPosting,
    boolean costCenterRequired,
    boolean businessLineRequired,
    boolean revaluationRequired,
    boolean reconcilable,
    boolean interBranch,
    String contraAccountCode,
    String reportGroup,
    boolean frozen,
    String freezeReason,
    LocalDate openedOn,
    LocalDate closedOn,
    Set<String> allowedCurrencies,
    Set<Long> allowedBranchIds,
    Set<String> allowedRoleCodes,
    RecordStatus recordStatus,
    String createdBy,
    String maker,
    String authorizedBy,
    NegativeBalancePolicy negativeBalancePolicy) {

  /**
   * Maps an entity.
   *
   * @param a account
   * @return response
   */
  public static GlAccountResponse from(GlAccount a) {
    return new GlAccountResponse(
        a.getId(),
        a.getCompanyId(),
        a.getCode(),
        a.getName(),
        a.getShortName(),
        a.getAccountClass(),
        a.getLevel(),
        a.getParent() == null ? null : a.getParent().getCode(),
        a.getCategory() == null ? null : a.getCategory().getCode(),
        a.isPostable(),
        a.isControlAccount(),
        a.getSubLedgerType(),
        a.isAllowManualPosting(),
        a.isCostCenterRequired(),
        a.isBusinessLineRequired(),
        a.isRevaluationRequired(),
        a.isReconcilable(),
        a.isInterBranch(),
        a.getContraAccountCode(),
        a.getReportGroup(),
        a.isFrozen(),
        a.getFreezeReason(),
        a.getOpenedOn(),
        a.getClosedOn(),
        a.getAllowedCurrencies(),
        a.getAllowedBranchIds(),
        a.getAllowedRoleCodes(),
        a.getRecordStatus(),
        a.getCreatedBy(),
        a.getMaker(),
        a.getAuthorizedBy(),
        a.getNegativeBalancePolicy());
  }
}
