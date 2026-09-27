package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * A level of the IAAF or TSU (TOR) approval matrix (BRIDSP-07, 18): per document, segment and sum
 * insured band, the permission or named approver of the level and the signatory title printed on
 * the PDF. Maintained with maker and checker.
 */
@Entity
@Table(name = "sbm_approval_matrix")
public class SbmApprovalMatrix extends AuthorizableEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(nullable = false, length = 10)
  private String document;

  @Column(length = 30)
  private String segment;

  @Column(name = "tsi_from", nullable = false, precision = 19, scale = 2)
  private BigDecimal tsiFrom;

  @Column(name = "tsi_to", precision = 19, scale = 2)
  private BigDecimal tsiTo;

  @Column(nullable = false)
  private int level;

  @Column(nullable = false, length = 50)
  private String permission;

  @Column(name = "approver_username", length = 50)
  private String approverUsername;

  @Column(name = "signatory_title", nullable = false, length = 120)
  private String signatoryTitle;

  protected SbmApprovalMatrix() {}

  /**
   * A level.
   *
   * @param companyId company
   * @param row document, segment, band, level, approver and title
   */
  public SbmApprovalMatrix(Long companyId, Row row) {
    this.companyId = companyId;
    change(row);
  }

  /**
   * Changes the level (pending approval again).
   *
   * @param row document, segment, band, level, approver and title
   */
  public void change(Row row) {
    this.document = row.document();
    this.segment = row.segment();
    this.tsiFrom = row.tsiFrom() == null ? BigDecimal.ZERO : row.tsiFrom();
    this.tsiTo = row.tsiTo();
    this.level = row.level();
    this.permission = row.permission();
    this.approverUsername = row.approverUsername();
    this.signatoryTitle = row.signatoryTitle();
    markModified();
  }

  /**
   * Whether the level applies to a document of a segment and sum insured.
   *
   * @param doc IAAF or TOR
   * @param seg segment
   * @param tsi sum insured (zero when unknown)
   * @return true when it applies
   */
  public boolean applies(String doc, String seg, BigDecimal tsi) {
    return document.equals(doc)
        && (segment == null || segment.equals(seg))
        && tsi.compareTo(tsiFrom) >= 0
        && (tsiTo == null || tsi.compareTo(tsiTo) <= 0);
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getDocument() {
    return document;
  }

  public String getSegment() {
    return segment;
  }

  public BigDecimal getTsiFrom() {
    return tsiFrom;
  }

  public BigDecimal getTsiTo() {
    return tsiTo;
  }

  public int getLevel() {
    return level;
  }

  public String getPermission() {
    return permission;
  }

  public String getApproverUsername() {
    return approverUsername;
  }

  public String getSignatoryTitle() {
    return signatoryTitle;
  }

  /**
   * The content of a level.
   *
   * @param document IAAF or TOR
   * @param segment segment, null for all
   * @param tsiFrom band start
   * @param tsiTo band end, null for no end
   * @param level level
   * @param permission permission of the approvers
   * @param approverUsername named approver, may be null
   * @param signatoryTitle title printed with the signature
   */
  public record Row(
      String document,
      String segment,
      BigDecimal tsiFrom,
      BigDecimal tsiTo,
      int level,
      String permission,
      String approverUsername,
      String signatoryTitle) {}
}
