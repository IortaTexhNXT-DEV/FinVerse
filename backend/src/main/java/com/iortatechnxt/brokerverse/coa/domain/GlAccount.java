package com.iortatechnxt.brokerverse.coa.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * GL Head of Account (Main / Sub / Micro GL) in the multi-tier chart of accounts.
 *
 * <p>Holds every posting control defined by the GL maintenance programs: allowed currencies,
 * allowed posting branches, role based access codes, freeze, closure, manual posting allowance,
 * mandatory dimensions and sub-ledger control.
 */
@Entity
@Table(name = "coa_account")
public class GlAccount extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false, length = 30)
  private String code;

  @Column(nullable = false, length = 150)
  private String name;

  @Column(name = "short_name", length = 40)
  private String shortName;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_class", nullable = false, length = 20)
  private AccountClass accountClass;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private AccountLevel level;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private GlAccount parent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private GlCategory category;

  @Column(nullable = false)
  private boolean postable;

  @Column(name = "control_account", nullable = false)
  private boolean controlAccount;

  @Enumerated(EnumType.STRING)
  @Column(name = "sub_ledger_type", nullable = false, length = 20)
  private SubLedgerType subLedgerType = SubLedgerType.NONE;

  @Column(name = "allow_manual_posting", nullable = false)
  private boolean allowManualPosting = true;

  @Column(name = "cost_center_required", nullable = false)
  private boolean costCenterRequired;

  @Column(name = "business_line_required", nullable = false)
  private boolean businessLineRequired;

  @Column(name = "revaluation_required", nullable = false)
  private boolean revaluationRequired;

  @Column(name = "reconcilable", nullable = false)
  private boolean reconcilable;

  @Column(name = "inter_branch", nullable = false)
  private boolean interBranch;

  @Column(name = "contra_account_code", length = 30)
  private String contraAccountCode;

  @Column(name = "report_group", length = 60)
  private String reportGroup;

  @Column(nullable = false)
  private boolean frozen;

  @Column(name = "freeze_reason", length = 200)
  private String freezeReason;

  @Column(name = "opened_on", nullable = false)
  private LocalDate openedOn;

  @Column(name = "closed_on")
  private LocalDate closedOn;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "coa_account_currency", joinColumns = @JoinColumn(name = "account_id"))
  @Column(name = "currency_code", nullable = false, length = 3)
  private final Set<String> allowedCurrencies = new HashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "coa_account_branch", joinColumns = @JoinColumn(name = "account_id"))
  @Column(name = "branch_id", nullable = false)
  private final Set<Long> allowedBranchIds = new HashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "coa_account_role", joinColumns = @JoinColumn(name = "account_id"))
  @Column(name = "role_code", nullable = false, length = 40)
  private final Set<String> allowedRoleCodes = new HashSet<>();

  protected GlAccount() {}

  /**
   * Creates an account.
   *
   * @param companyId company
   * @param code account code
   * @param name account name
   * @param accountClass classification
   * @param level tier
   * @param openedOn date of opening
   */
  public GlAccount(
      Long companyId,
      String code,
      String name,
      AccountClass accountClass,
      AccountLevel level,
      LocalDate openedOn) {
    this.companyId = companyId;
    this.code = code;
    this.name = name;
    this.accountClass = accountClass;
    this.level = level;
    this.openedOn = openedOn;
    this.postable = level != AccountLevel.GROUP;
  }

  /**
   * Freezes the account for posting.
   *
   * @param reason reason recorded for audit
   */
  public void freeze(String reason) {
    this.frozen = true;
    this.freezeReason = reason;
  }

  /** Removes the posting freeze. */
  public void unfreeze() {
    this.frozen = false;
    this.freezeReason = null;
  }

  /**
   * Closes the account (GL Closure) from a date.
   *
   * @param date closure date
   */
  public void close(LocalDate date) {
    this.closedOn = date;
  }

  /**
   * Checks whether the account accepts a currency.
   *
   * @param currency ISO code
   * @return true when no restriction exists or the currency is allowed
   */
  public boolean acceptsCurrency(String currency) {
    return allowedCurrencies.isEmpty() || allowedCurrencies.contains(currency);
  }

  /**
   * Checks whether the account accepts postings from a branch.
   *
   * @param branchId branch id
   * @return true when no restriction exists or the branch is allowed
   */
  public boolean acceptsBranch(Long branchId) {
    return allowedBranchIds.isEmpty() || allowedBranchIds.contains(branchId);
  }

  /**
   * Checks whether any of the given roles may post to the account.
   *
   * @param roleCodes the user's role codes
   * @return true when no access code restriction exists or a role matches
   */
  public boolean acceptsAnyRole(Set<String> roleCodes) {
    return allowedRoleCodes.isEmpty() || roleCodes.stream().anyMatch(allowedRoleCodes::contains);
  }

  /**
   * Checks whether the account is closed on a date.
   *
   * @param date date
   * @return true when closed on or before date
   */
  public boolean isClosedOn(LocalDate date) {
    return closedOn != null && !date.isBefore(closedOn);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  public AccountClass getAccountClass() {
    return accountClass;
  }

  public AccountLevel getLevel() {
    return level;
  }

  public GlAccount getParent() {
    return parent;
  }

  public void setParent(GlAccount parent) {
    this.parent = parent;
  }

  public GlCategory getCategory() {
    return category;
  }

  public void setCategory(GlCategory category) {
    this.category = category;
  }

  public boolean isPostable() {
    return postable;
  }

  public void setPostable(boolean postable) {
    this.postable = postable;
  }

  public boolean isControlAccount() {
    return controlAccount;
  }

  public void setControlAccount(boolean controlAccount) {
    this.controlAccount = controlAccount;
  }

  public SubLedgerType getSubLedgerType() {
    return subLedgerType;
  }

  public void setSubLedgerType(SubLedgerType subLedgerType) {
    this.subLedgerType = subLedgerType;
  }

  public boolean isAllowManualPosting() {
    return allowManualPosting;
  }

  public void setAllowManualPosting(boolean allowManualPosting) {
    this.allowManualPosting = allowManualPosting;
  }

  public boolean isCostCenterRequired() {
    return costCenterRequired;
  }

  public void setCostCenterRequired(boolean costCenterRequired) {
    this.costCenterRequired = costCenterRequired;
  }

  public boolean isBusinessLineRequired() {
    return businessLineRequired;
  }

  public void setBusinessLineRequired(boolean businessLineRequired) {
    this.businessLineRequired = businessLineRequired;
  }

  public boolean isRevaluationRequired() {
    return revaluationRequired;
  }

  public void setRevaluationRequired(boolean revaluationRequired) {
    this.revaluationRequired = revaluationRequired;
  }

  public boolean isReconcilable() {
    return reconcilable;
  }

  public void setReconcilable(boolean reconcilable) {
    this.reconcilable = reconcilable;
  }

  public boolean isInterBranch() {
    return interBranch;
  }

  public void setInterBranch(boolean interBranch) {
    this.interBranch = interBranch;
  }

  public String getContraAccountCode() {
    return contraAccountCode;
  }

  public void setContraAccountCode(String contraAccountCode) {
    this.contraAccountCode = contraAccountCode;
  }

  public String getReportGroup() {
    return reportGroup;
  }

  public void setReportGroup(String reportGroup) {
    this.reportGroup = reportGroup;
  }

  public boolean isFrozen() {
    return frozen;
  }

  public String getFreezeReason() {
    return freezeReason;
  }

  public LocalDate getOpenedOn() {
    return openedOn;
  }

  public LocalDate getClosedOn() {
    return closedOn;
  }

  public Set<String> getAllowedCurrencies() {
    return Set.copyOf(allowedCurrencies);
  }

  /**
   * Replaces the allowed currencies (empty means all).
   *
   * @param currencies ISO codes
   */
  public void replaceAllowedCurrencies(Set<String> currencies) {
    allowedCurrencies.clear();
    allowedCurrencies.addAll(currencies);
  }

  public Set<Long> getAllowedBranchIds() {
    return Set.copyOf(allowedBranchIds);
  }

  /**
   * Replaces the allowed posting branches (empty means all).
   *
   * @param branchIds branch ids
   */
  public void replaceAllowedBranches(Set<Long> branchIds) {
    allowedBranchIds.clear();
    allowedBranchIds.addAll(branchIds);
  }

  public Set<String> getAllowedRoleCodes() {
    return Set.copyOf(allowedRoleCodes);
  }

  /**
   * Replaces the access codes (empty means all roles).
   *
   * @param roleCodes role codes
   */
  public void replaceAllowedRoles(Set<String> roleCodes) {
    allowedRoleCodes.clear();
    allowedRoleCodes.addAll(roleCodes);
  }
}
