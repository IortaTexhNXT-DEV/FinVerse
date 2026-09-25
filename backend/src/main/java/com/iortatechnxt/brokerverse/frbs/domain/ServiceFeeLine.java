package com.iortatechnxt.brokerverse.frbs.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.LineStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * A line of a service-fee run (FRBS 2.10.0-2.10.2): the fee of one service-fee segment, sales unit
 * and currency, its accrual journal, its payout request to Disbursement and the RELEASED and
 * LIQUIDATED tags with their dates and the unit's liquidation report.
 */
@Entity
@Table(name = "frbs_service_fee_line")
public class ServiceFeeLine extends BaseEntity {

  private static final int MAX_TEXT = 250;

  @Column(name = "run_id", nullable = false, updatable = false)
  private Long runId;

  @Column(name = "line_no", nullable = false, updatable = false)
  private int lineNo;

  @Column(nullable = false, length = 20, updatable = false)
  private String segment;

  @Column(name = "sales_unit", nullable = false, length = 20, updatable = false)
  private String salesUnit;

  @Column(name = "payee_code", nullable = false, length = 30, updatable = false)
  private String payeeCode;

  @Column(name = "payee_name", nullable = false, length = 250, updatable = false)
  private String payeeName;

  @Column(name = "cost_center", length = 20, updatable = false)
  private String costCenter;

  @Column(name = "branch_id", nullable = false, updatable = false)
  private Long branchId;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(nullable = false, precision = 9, scale = 4, updatable = false)
  private BigDecimal rate;

  @Column(name = "invoice_count", nullable = false, updatable = false)
  private int invoiceCount;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal commission;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal wtax;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal base;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal fee;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private LineStatus status = LineStatus.COMPUTED;

  @Column(name = "accrual_batch_no", length = 40)
  private String accrualBatchNo;

  @Column(name = "send_count", nullable = false)
  private int sendCount;

  @Column(name = "request_no", length = 30)
  private String requestNo;

  @Column(name = "gateway_status", length = 20)
  private String gatewayStatus;

  @Column(name = "dv_no", length = 40)
  private String dvNo;

  @Column(name = "gateway_message", length = MAX_TEXT)
  private String gatewayMessage;

  @Column(name = "released_on")
  private LocalDate releasedOn;

  @Column(name = "released_by", length = 50)
  private String releasedBy;

  @Column(name = "liquidated_on")
  private LocalDate liquidatedOn;

  @Column(name = "liquidated_by", length = 50)
  private String liquidatedBy;

  @Column(name = "liquidation_ref", length = 40)
  private String liquidationRef;

  @Column(length = MAX_TEXT)
  private String remarks;

  protected ServiceFeeLine() {}

  /**
   * A computed line.
   *
   * @param runId run
   * @param lineNo line number
   * @param draft computed content
   */
  public ServiceFeeLine(Long runId, int lineNo, LineDraft draft) {
    this.runId = runId;
    this.lineNo = lineNo;
    this.segment = draft.segment();
    this.salesUnit = draft.salesUnit();
    this.payeeCode = draft.payeeCode();
    this.payeeName = draft.payeeName();
    this.costCenter = draft.costCenter();
    this.branchId = draft.branchId();
    this.currency = draft.currency();
    this.rate = draft.rate();
    this.invoiceCount = draft.invoiceCount();
    this.commission = draft.commission();
    this.wtax = draft.wtax();
    this.base = draft.base();
    this.fee = draft.fee();
  }

  /**
   * The payout reference of the current sending: {@code SFR-...:<line>}, then {@code :2}, {@code
   * :3} after a return.
   *
   * @param runNo run number
   * @return source reference
   */
  public String sourceRef(String runNo) {
    String ref = runNo + ":" + lineNo;
    return sendCount > 1 ? ref + ":" + sendCount : ref;
  }

  /**
   * Records the accrual journal (FRBS_SERVICE_FEE_ACCRUE).
   *
   * @param batchNo journal batch
   */
  public void accrued(String batchNo) {
    accrualBatchNo = batchNo;
  }

  /** Counts a new sending to Disbursement (the first, or again after a return). */
  public void nextSending() {
    require(Set.of(LineStatus.COMPUTED, LineStatus.RETURNED), "sent to Disbursement");
    sendCount++;
  }

  /**
   * Records the gateway ticket of a sending.
   *
   * @param ticket request number, gateway status, DV number and message
   */
  public void sent(Ticket ticket) {
    status = LineStatus.SENT;
    track(ticket);
  }

  /**
   * Keeps what Disbursement reports.
   *
   * @param ticket request number, gateway status, DV number and message
   */
  public void track(Ticket ticket) {
    requestNo = ticket.requestNo() == null ? requestNo : ticket.requestNo();
    gatewayStatus = ticket.status();
    dvNo = ticket.dvNo() == null ? dvNo : ticket.dvNo();
    gatewayMessage = cut(ticket.message());
  }

  /**
   * Disbursement returned or cancelled the payout before it was released.
   *
   * @param ticket status and reason
   */
  public void returned(Ticket ticket) {
    track(ticket);
    if (status == LineStatus.SENT) {
      status = LineStatus.RETURNED;
    }
  }

  /**
   * Tags the line released (FRBS 2.10.2): credited to the recipient.
   *
   * @param on release date
   * @param by user or SYSTEM
   */
  public void release(LocalDate on, String by) {
    require(Set.of(LineStatus.SENT), "tagged released");
    status = LineStatus.RELEASED;
    releasedOn = on;
    releasedBy = by;
  }

  /**
   * Tags the line liquidated with the unit's liquidation report (FRBS 2.10.1-2.10.2).
   *
   * @param on liquidation date
   * @param by user
   * @param reportRef attachment id of the liquidation report
   * @param note remarks
   */
  public void liquidate(LocalDate on, String by, String reportRef, String note) {
    require(Set.of(LineStatus.RELEASED), "tagged liquidated");
    if (releasedOn != null && on.isBefore(releasedOn)) {
      throw new BusinessRuleException(
          "SERVICE_FEE_DATES", "The liquidation date is before the release date " + releasedOn);
    }
    status = LineStatus.LIQUIDATED;
    liquidatedOn = on;
    liquidatedBy = by;
    liquidationRef = reportRef;
    remarks = cut(note);
  }

  /** The run was cancelled. */
  public void cancel() {
    status = LineStatus.CANCELLED;
  }

  private void require(Set<LineStatus> allowed, String action) {
    if (!allowed.contains(status)) {
      throw new BusinessRuleException(
          "SERVICE_FEE_LINE_STATUS",
          "Line " + lineNo + " is " + status + " and cannot be " + action);
    }
  }

  private static String cut(String text) {
    return text == null || text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT);
  }

  public Long getRunId() {
    return runId;
  }

  public int getLineNo() {
    return lineNo;
  }

  public String getSegment() {
    return segment;
  }

  public String getSalesUnit() {
    return salesUnit;
  }

  public String getPayeeCode() {
    return payeeCode;
  }

  public String getPayeeName() {
    return payeeName;
  }

  public String getCostCenter() {
    return costCenter;
  }

  public Long getBranchId() {
    return branchId;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public int getInvoiceCount() {
    return invoiceCount;
  }

  public BigDecimal getCommission() {
    return commission;
  }

  public BigDecimal getWtax() {
    return wtax;
  }

  public BigDecimal getBase() {
    return base;
  }

  public BigDecimal getFee() {
    return fee;
  }

  public LineStatus getStatus() {
    return status;
  }

  public String getAccrualBatchNo() {
    return accrualBatchNo;
  }

  public int getSendCount() {
    return sendCount;
  }

  public String getRequestNo() {
    return requestNo;
  }

  public String getGatewayStatus() {
    return gatewayStatus;
  }

  public String getDvNo() {
    return dvNo;
  }

  public String getGatewayMessage() {
    return gatewayMessage;
  }

  public LocalDate getReleasedOn() {
    return releasedOn;
  }

  public String getReleasedBy() {
    return releasedBy;
  }

  public LocalDate getLiquidatedOn() {
    return liquidatedOn;
  }

  public String getLiquidatedBy() {
    return liquidatedBy;
  }

  public String getLiquidationRef() {
    return liquidationRef;
  }

  public String getRemarks() {
    return remarks;
  }

  /**
   * The computed content of a line.
   *
   * @param segment service-fee segment
   * @param salesUnit sales unit
   * @param payeeCode recipient party code
   * @param payeeName recipient name
   * @param costCenter cost centre charged, null for the derivation rules
   * @param branchId branch of the accrual
   * @param currency currency
   * @param rate rate in percent
   * @param invoiceCount invoices
   * @param commission commission of the invoices
   * @param wtax insurer's withholding tax deducted from the base
   * @param base commission base
   * @param fee service fee
   */
  public record LineDraft(
      String segment,
      String salesUnit,
      String payeeCode,
      String payeeName,
      String costCenter,
      Long branchId,
      String currency,
      BigDecimal rate,
      int invoiceCount,
      BigDecimal commission,
      BigDecimal wtax,
      BigDecimal base,
      BigDecimal fee) {}

  /**
   * What Disbursement said about a payout.
   *
   * @param requestNo gateway request number
   * @param status gateway status
   * @param dvNo DV number
   * @param message return reason or other information
   */
  public record Ticket(String requestNo, String status, String dvNo, String message) {}
}
