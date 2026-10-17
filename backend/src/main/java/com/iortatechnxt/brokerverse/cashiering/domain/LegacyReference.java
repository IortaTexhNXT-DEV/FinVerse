package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A legacy number of an account migrated at go-live (QPS, ISYS, Ebix, PN or Loan Application
 * Number) and the account it stands for, so that a payment quoting it is matched (FRS.CSH.05.01.08;
 * Appendix P).
 */
@Entity
@Table(name = "csh_legacy_reference")
public class LegacyReference extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "ref_type", nullable = false, length = 20, updatable = false)
  private String refType;

  @Column(name = "ref_no", nullable = false, length = 60, updatable = false)
  private String refNo;

  @Column(name = "account_ref", nullable = false, length = 60)
  private String accountRef;

  protected LegacyReference() {}

  /**
   * Creates a legacy number.
   *
   * @param companyId company
   * @param refType QPS, ISYS, EBIX, PN or LOAN_APPLICATION
   * @param refNo legacy number
   * @param accountRef account, invoice or ARN it stands for
   */
  public LegacyReference(Long companyId, String refType, String refNo, String accountRef) {
    this.companyId = companyId;
    this.refType = refType;
    this.refNo = refNo;
    this.accountRef = accountRef;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getRefType() {
    return refType;
  }

  public String getRefNo() {
    return refNo;
  }

  public String getAccountRef() {
    return accountRef;
  }
}
