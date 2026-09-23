package com.iortatechnxt.finverse.payables.domain;

import com.iortatechnxt.finverse.common.domain.BaseEntity;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;

/**
 * Transaction document approved under maker-checker control (petty cash vouchers, supplier
 * invoices, payment vouchers). Holds the approval audit columns and enforces that the checker is
 * not the maker.
 */
@MappedSuperclass
public abstract class ApprovableDocument extends BaseEntity {

  @Column(name = "approved_by", length = 50)
  private String approvedBy;

  @Column(name = "approved_at")
  private Instant approvedAt;

  @Column(name = "status_reason", length = 200)
  private String statusReason;

  /**
   * Records the approval after checking segregation of duties.
   *
   * @param checker approving user
   * @param when timestamp
   */
  protected void recordApproval(String checker, Instant when) {
    requireChecker(checker);
    this.approvedBy = checker;
    this.approvedAt = when;
    this.statusReason = null;
  }

  /**
   * Fails when the checker also maintained the document.
   *
   * @param checker user acting as checker
   */
  public void requireChecker(String checker) {
    if (Objects.equals(checker, getCreatedBy()) || Objects.equals(checker, maker())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "A document cannot be approved by the user who prepared it");
    }
  }

  /**
   * The user accountable for the content (submitter, else creator).
   *
   * @return maker user name
   */
  public String maker() {
    return getCreatedBy();
  }

  /**
   * Stores the reason of a rejection, cancellation or void.
   *
   * @param reason reason text
   */
  protected void recordReason(String reason) {
    this.statusReason = reason;
  }

  public String getApprovedBy() {
    return approvedBy;
  }

  public Instant getApprovedAt() {
    return approvedAt;
  }

  public String getStatusReason() {
    return statusReason;
  }
}
