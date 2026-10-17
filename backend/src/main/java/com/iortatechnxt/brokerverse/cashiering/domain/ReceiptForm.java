package com.iortatechnxt.brokerverse.cashiering.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A version of the AR or OR form (FRS.CSH.02.06.01 to 02.06.03): the header (company name,
 * description, address and VAT TIN, blank for the company record), the note line of the AR and the
 * four footer lines with the tokens {COMPANY_NAME}, {CERTIFICATE_NO}, {PRINT_DATE}, {SERIES_RANGE}
 * and {COPY_LABEL}. A version is used from its effective date once another authorised user approved
 * it; earlier versions are kept.
 */
@Entity
@Table(name = "csh_receipt_form")
public class ReceiptForm extends BaseEntity {

  /** Waiting for the approval of another authorised user. */
  public static final String PENDING = "PENDING_APPROVAL";

  /** Approved: used from its effective date. */
  public static final String APPROVED = "APPROVED";

  /** Rejected by the approver. */
  public static final String REJECTED = "REJECTED";

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "form_kind", nullable = false, length = 2, updatable = false)
  private String formKind;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Embedded private FormText text;

  @Column(name = "effective_from", nullable = false)
  private LocalDate effectiveFrom;

  @Column(nullable = false, length = 20)
  private String status = PENDING;

  @Column(name = "decided_by", length = 50)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "decision_remarks", length = 250)
  private String decisionRemarks;

  /** JPA. */
  protected ReceiptForm() {}

  /**
   * A new version waiting for approval.
   *
   * @param companyId company
   * @param formKind AR or OR
   * @param versionNo version number
   * @param text header, note and footer lines
   * @param effectiveFrom first print date
   */
  public ReceiptForm(
      Long companyId, String formKind, int versionNo, FormText text, LocalDate effectiveFrom) {
    this.companyId = companyId;
    this.formKind = formKind;
    this.versionNo = versionNo;
    this.text = text;
    this.effectiveFrom = effectiveFrom;
  }

  /**
   * Approves or rejects the version; the user who made it cannot decide it.
   *
   * @param approve true to approve
   * @param user deciding user
   * @param remarks remarks, required to reject
   * @param at time
   */
  public void decide(boolean approve, String user, String remarks, Instant at) {
    if (!PENDING.equals(status)) {
      throw new BusinessRuleException(
          "FORM_NOT_PENDING", "Version " + versionNo + " is not waiting for approval");
    }
    if (user.equals(getCreatedBy())) {
      throw new BusinessRuleException(
          "MAKER_CHECKER_VIOLATION", "The user who changed the form cannot approve it");
    }
    this.status = approve ? APPROVED : REJECTED;
    this.decidedBy = user;
    this.decidedAt = at;
    this.decisionRemarks = remarks;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getFormKind() {
    return formKind;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public FormText getText() {
    return text == null ? FormText.EMPTY : text;
  }

  public LocalDate getEffectiveFrom() {
    return effectiveFrom;
  }

  public String getStatus() {
    return status;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getDecisionRemarks() {
    return decisionRemarks;
  }
}
