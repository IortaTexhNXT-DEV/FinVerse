package com.iortatechnxt.brokerverse.remittance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * How a batch is settled with the insurer beyond its line amounts (ACCOUNTING_DISBURSEMENT_DESIGN
 * 12.2): the remittance deductions applied in the current send cycle (ACSL 2.9.2), the send cycle
 * of the payment request (DIS 2.20.0: +1 after each cancelled DV, so the next request and postings
 * get new references), the early-incentive service invoice with its withholding tax (DIS 3.29.1)
 * and the last cancelled DV.
 */
@Embeddable
public class BatchSettlement {

  private static final String SEPARATOR = "/R";

  @Column(name = "deduction_amount", nullable = false, precision = 19, scale = 2)
  private BigDecimal deductionAmount = BigDecimal.ZERO.setScale(2);

  @Column(name = "send_cycle", nullable = false)
  private int sendCycle = 1;

  @Column(name = "early_si_no", length = 40)
  private String earlySiNo;

  @Column(name = "early_si_wtax", precision = 19, scale = 2)
  private BigDecimal earlySiWtax;

  @Column(name = "cancelled_dv_no", length = 40)
  private String cancelledDvNo;

  @Column(name = "cancel_reason", length = 500)
  private String cancelReason;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  /**
   * The reference of the batch's payment request and postings in the current cycle: the batch
   * number in the first cycle, then {@code <batch>/R<cycle>}.
   *
   * @param batchNo batch number
   * @return reference
   */
  public String reference(String batchNo) {
    return sendCycle == 1 ? batchNo : batchNo + SEPARATOR + sendCycle;
  }

  /**
   * The batch number of a cycle reference.
   *
   * @param reference reference of a payment request
   * @return batch number
   */
  public static String batchNoOf(String reference) {
    int at = reference.indexOf(SEPARATOR);
    return at < 0 ? reference : reference.substring(0, at);
  }

  void applied(BigDecimal amount) {
    this.deductionAmount = amount;
  }

  void serviceInvoice(String siNo, BigDecimal wtax) {
    this.earlySiNo = siNo;
    this.earlySiWtax = wtax;
  }

  void cancelled(String dvNo, String reason, Instant at, int maxReason) {
    this.cancelledDvNo = dvNo;
    this.cancelReason =
        reason == null || reason.length() <= maxReason ? reason : reason.substring(0, maxReason);
    this.cancelledAt = at;
    this.deductionAmount = BigDecimal.ZERO.setScale(2);
    this.sendCycle++;
  }

  public BigDecimal getDeductionAmount() {
    return deductionAmount;
  }

  public int getSendCycle() {
    return sendCycle;
  }

  public String getEarlySiNo() {
    return earlySiNo;
  }

  public BigDecimal getEarlySiWtax() {
    return earlySiWtax;
  }

  public String getCancelledDvNo() {
    return cancelledDvNo;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }
}
