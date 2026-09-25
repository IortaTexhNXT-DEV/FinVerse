package com.iortatechnxt.brokerverse.accounting.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Objects;

/**
 * Standard rule for capturing the cost / responsibility centre (FRBS 3.1.1, DIS 3.30.0; AQ26): the
 * accounting engine gives a journal line that needs a cost centre, and has none, the cost centre of
 * the first active rule (lowest priority number) whose criteria all match. A blank criterion
 * matches everything.
 */
@Entity
@Table(name = "acc_cost_center_rule")
public class CostCenterRule extends BaseEntity {

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false)
  private int priority;

  @Column(name = "source_module", length = 30)
  private String sourceModule;

  @Column(name = "event_type", length = 40)
  private String eventType;

  @Column(name = "branch_id")
  private Long branchId;

  @Column(name = "party_code", length = 30)
  private String partyCode;

  @Column(name = "account_code", length = 30)
  private String accountCode;

  @Column(name = "cost_center", nullable = false, length = 20)
  private String costCenter;

  @Column(length = 200)
  private String description;

  @Column(nullable = false)
  private boolean active = true;

  protected CostCenterRule() {}

  /**
   * Creates a rule.
   *
   * @param companyId company
   * @param values rule values
   */
  public CostCenterRule(Long companyId, CostCenterRuleValues values) {
    this.companyId = companyId;
    change(values);
  }

  /**
   * Changes the rule.
   *
   * @param values rule values
   */
  public final void change(CostCenterRuleValues values) {
    this.priority = values.priority();
    this.sourceModule = values.sourceModule();
    this.eventType = values.eventType();
    this.branchId = values.branchId();
    this.partyCode = values.partyCode();
    this.accountCode = values.accountCode();
    this.costCenter = values.costCenter();
    this.description = values.description();
    this.active = values.active();
  }

  /**
   * Whether the rule applies to a posting line.
   *
   * @param line facts of the line
   * @return true when active and every criterion matches
   */
  public boolean matches(LineFacts line) {
    return active && matchesEvent(line) && matchesParty(line);
  }

  private boolean matchesEvent(LineFacts line) {
    return matches(sourceModule, line.sourceModule()) && matches(eventType, line.eventType());
  }

  private boolean matchesParty(LineFacts line) {
    return (branchId == null || branchId.equals(line.branchId()))
        && matches(partyCode, line.partyCode())
        && matches(accountCode, line.accountCode());
  }

  private static boolean matches(String criterion, String value) {
    return criterion == null
        || String.CASE_INSENSITIVE_ORDER.compare(criterion, Objects.toString(value, "")) == 0;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public int getPriority() {
    return priority;
  }

  public String getSourceModule() {
    return sourceModule;
  }

  public String getEventType() {
    return eventType;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getPartyCode() {
    return partyCode;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public String getDescription() {
    return description;
  }

  public boolean isActive() {
    return active;
  }

  /**
   * Facts of a posting line that rules are matched against.
   *
   * @param sourceModule publishing module
   * @param eventType event type
   * @param branchId branch
   * @param partyCode sub-ledger party
   * @param accountCode GL account
   */
  public record LineFacts(
      String sourceModule, String eventType, Long branchId, String partyCode, String accountCode) {}
}
