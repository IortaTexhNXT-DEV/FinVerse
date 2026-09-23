package com.iortatechnxt.finverse.consolidation.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Inter-company relationship of a company pair: the due-from (receivable) and due-to (payable)
 * accounts each company uses for the other. Only active relationships may transact.
 */
@Entity
@Table(name = "ic_relationship")
public class IntercompanyRelationship extends BaseEntity {

  @Column(name = "company_a_id", nullable = false)
  private Long companyAId;

  @Column(name = "company_b_id", nullable = false)
  private Long companyBId;

  @Column(name = "a_due_from_account", nullable = false, length = 30)
  private String aDueFromAccount;

  @Column(name = "a_due_to_account", nullable = false, length = 30)
  private String aDueToAccount;

  @Column(name = "b_due_from_account", nullable = false, length = 30)
  private String bDueFromAccount;

  @Column(name = "b_due_to_account", nullable = false, length = 30)
  private String bDueToAccount;

  @Column(nullable = false)
  private boolean active = true;

  protected IntercompanyRelationship() {}

  /**
   * Creates a relationship.
   *
   * @param a first company side
   * @param b second company side
   */
  public IntercompanyRelationship(Side a, Side b) {
    if (a.companyId().equals(b.companyId())) {
      throw new BusinessRuleException(
          "INVALID_IC_RELATIONSHIP", "An inter-company relationship needs two different companies");
    }
    this.companyAId = a.companyId();
    this.aDueFromAccount = a.dueFromAccount();
    this.aDueToAccount = a.dueToAccount();
    this.companyBId = b.companyId();
    this.bDueFromAccount = b.dueFromAccount();
    this.bDueToAccount = b.dueToAccount();
  }

  /**
   * Whether a company is one of the pair.
   *
   * @param companyId company
   * @return true when A or B
   */
  public boolean involves(Long companyId) {
    return companyAId.equals(companyId) || companyBId.equals(companyId);
  }

  /**
   * The accounts one company of the pair uses for the other.
   *
   * @param companyId A or B
   * @return side
   */
  public Side sideOf(Long companyId) {
    if (companyAId.equals(companyId)) {
      return new Side(companyAId, aDueFromAccount, aDueToAccount);
    }
    if (companyBId.equals(companyId)) {
      return new Side(companyBId, bDueFromAccount, bDueToAccount);
    }
    throw new BusinessRuleException(
        "INVALID_IC_RELATIONSHIP", "Company is not part of this inter-company relationship");
  }

  public Long getCompanyAId() {
    return companyAId;
  }

  public Long getCompanyBId() {
    return companyBId;
  }

  public String getADueFromAccount() {
    return aDueFromAccount;
  }

  public String getADueToAccount() {
    return aDueToAccount;
  }

  public String getBDueFromAccount() {
    return bDueFromAccount;
  }

  public String getBDueToAccount() {
    return bDueToAccount;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  /**
   * One company's accounts in the relationship.
   *
   * @param companyId company
   * @param dueFromAccount receivable from the counterparty
   * @param dueToAccount payable to the counterparty
   */
  public record Side(Long companyId, String dueFromAccount, String dueToAccount) {}
}
