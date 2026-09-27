package com.iortatechnxt.brokerverse.placement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A placement slip PL-yyyy-nnnnnn for one insurer branch (BRNB.069), covering one or more accounts.
 * A slip regenerated after an insurer return keeps its number with the next version; the previous
 * version stays as SUPERSEDED. Sending records the placement of its accounts (BRNB.071).
 */
@Entity
@Table(name = "plc_slip")
public class PlacementSlip extends BaseEntity {

  @Column(name = "company_id", nullable = false, updatable = false)
  private Long companyId;

  @Column(name = "slip_no", nullable = false, length = 30, updatable = false)
  private String slipNo;

  @Column(name = "version_no", nullable = false, updatable = false)
  private int versionNo;

  @Column(name = "insurer_code", nullable = false, length = 30, updatable = false)
  private String insurerCode;

  @Column(name = "branch_code", nullable = false, length = 20, updatable = false)
  private String branchCode;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private SlipStatus status = SlipStatus.GENERATED;

  @Column(name = "template_version", nullable = false, length = 60, updatable = false)
  private String templateVersion;

  @Column(length = 1000)
  private String recipients;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "send_count", nullable = false)
  private int sendCount;

  @ElementCollection
  @CollectionTable(name = "plc_slip_account", joinColumns = @JoinColumn(name = "slip_id"))
  @OrderColumn(name = "line_index")
  private final List<SlipAccount> accounts = new ArrayList<>();

  protected PlacementSlip() {}

  /**
   * Creates a generated slip.
   *
   * @param companyId company
   * @param number slip number and version
   * @param insurer insurer and branch
   * @param templateVersion placement slip template version used (BRNB.004)
   * @param slipAccounts accounts placed on the slip
   */
  public PlacementSlip(
      Long companyId,
      SlipNumber number,
      SlipInsurer insurer,
      String templateVersion,
      List<SlipAccount> slipAccounts) {
    this.companyId = companyId;
    this.slipNo = number.slipNo();
    this.versionNo = number.versionNo();
    this.insurerCode = insurer.insurerCode();
    this.branchCode = insurer.branchCode();
    this.templateVersion = templateVersion;
    this.accounts.addAll(slipAccounts);
  }

  /**
   * Records a send (first send or resend).
   *
   * @param to recipients
   * @param when time
   * @return true on the first send (the accounts are placed), false on a resend
   */
  public boolean recordSend(String to, Instant when) {
    if (status == SlipStatus.SUPERSEDED) {
      throw new BusinessRuleException(
          "SLIP_SUPERSEDED",
          "Slip " + slipNo + " v" + versionNo + " was replaced by a new version");
    }
    boolean first = status == SlipStatus.GENERATED;
    this.status = SlipStatus.SENT;
    this.recipients = to;
    this.sentAt = when;
    this.sendCount++;
    return first;
  }

  /** Marks the slip as replaced by a new version. */
  public void supersede() {
    this.status = SlipStatus.SUPERSEDED;
  }

  /**
   * Display reference, e.g. {@code PL-2026-000001 v2}.
   *
   * @return reference
   */
  public String displayNo() {
    return versionNo == 1 ? slipNo : slipNo + " v" + versionNo;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getSlipNo() {
    return slipNo;
  }

  public int getVersionNo() {
    return versionNo;
  }

  public String getInsurerCode() {
    return insurerCode;
  }

  public String getBranchCode() {
    return branchCode;
  }

  public SlipStatus getStatus() {
    return status;
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  public String getRecipients() {
    return recipients;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public int getSendCount() {
    return sendCount;
  }

  public List<SlipAccount> getAccounts() {
    return List.copyOf(accounts);
  }

  /**
   * Number and version of a slip.
   *
   * @param slipNo slip number
   * @param versionNo version, 1 for a new slip
   */
  public record SlipNumber(String slipNo, int versionNo) {}

  /**
   * Insurer and branch the slip is addressed to.
   *
   * @param insurerCode insurer party code
   * @param branchCode insurer branch
   */
  public record SlipInsurer(String insurerCode, String branchCode) {}
}
