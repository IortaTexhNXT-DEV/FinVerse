package com.iortatechnxt.brokerverse.disbursement.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * The payment instrument of an approved voucher (DIS 2.7.0, 2.8.x, 3.26.x): check, authority to
 * debit, credit to account, manager's check / demand draft, credit ticket, telegraphic transfer or
 * online banking, with its number, status and the time of each step. Status changes follow {@link
 * InstrumentLifecycle}; a status edit approved by the team leader (DIS 2.8.5) sets it directly.
 */
@Entity
@Table(name = "dsb_instrument")
public class Instrument extends BaseEntity {

  private static final int MAX_TEXT = 250;

  @Column(name = "voucher_id", nullable = false, updatable = false)
  private Long voucherId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20, updatable = false)
  private DisbursementMode mode;

  @Column(name = "instrument_no", length = 40)
  private String instrumentNo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private InstrumentStatus status;

  @Column(nullable = false, precision = 19, scale = 2, updatable = false)
  private BigDecimal amount;

  @Column(nullable = false, length = 3, updatable = false)
  private String currency;

  @Column(name = "bank_account_id", updatable = false)
  private Long bankAccountId;

  @Column(length = 80)
  private String reference;

  @Column(name = "printed_on")
  private LocalDate printedOn;

  @Column(name = "printed_at")
  private Instant printedAt;

  @Column(name = "released_at")
  private Instant releasedAt;

  @Column(name = "released_to", length = MAX_TEXT)
  private String releasedTo;

  @Column(name = "emailed_at")
  private Instant emailedAt;

  @Column(name = "received_at")
  private Instant receivedAt;

  @Column(name = "credited_at")
  private Instant creditedAt;

  @Column(name = "debited_at")
  private Instant debitedAt;

  @Column(name = "negotiated_at")
  private Instant negotiatedAt;

  @Column(name = "stale_at")
  private Instant staleAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "eod_run_id")
  private Long eodRunId;

  protected Instrument() {}

  /**
   * The instrument of an approved voucher, in the first status of its mode.
   *
   * @param voucherId voucher
   * @param mode mode of payment
   * @param amount net amount
   * @param currency currency
   * @param bankAccountId paying bank account
   */
  public Instrument(
      Long voucherId,
      DisbursementMode mode,
      BigDecimal amount,
      String currency,
      Long bankAccountId) {
    this.voucherId = voucherId;
    this.mode = mode;
    this.amount = amount;
    this.currency = currency;
    this.bankAccountId = bankAccountId;
    this.status = InstrumentLifecycle.initial(mode);
  }

  /**
   * Moves along the life cycle of the mode.
   *
   * @param to target status
   * @param at time
   * @return the previous status
   */
  public InstrumentStatus move(InstrumentStatus to, Instant at) {
    if (!InstrumentLifecycle.allows(mode, status, to)) {
      throw new BusinessRuleException(
          "INSTRUMENT_STATUS", mode + " " + label() + " is " + status + " and cannot become " + to);
    }
    return set(to, at);
  }

  /**
   * Sets a status approved as a status edit (DIS 2.8.5), outside the normal life cycle.
   *
   * @param to status
   * @param at time
   * @return the previous status
   */
  public InstrumentStatus correct(InstrumentStatus to, Instant at) {
    return set(to, at);
  }

  @SuppressWarnings("PMD.CyclomaticComplexity") // one time stamp per status, a flat switch
  private InstrumentStatus set(InstrumentStatus to, Instant at) {
    InstrumentStatus from = status;
    status = to;
    switch (to) {
      case PRINTED -> printedAt = at;
      case RELEASED -> releasedAt = at;
      case EMAILED -> emailedAt = at;
      case RECEIVED -> receivedAt = at;
      case CREDITED -> creditedAt = at;
      case DEBITED -> debitedAt = at;
      case NEGOTIATED -> negotiatedAt = at;
      case STALE -> staleAt = at;
      case CANCELLED -> cancelledAt = at;
      default -> {
        // PENDING, EXTRACTED and APPROVED carry no time of their own
      }
    }
    return from;
  }

  /**
   * Records the number of the instrument (check number, ATD / form number) and its print date.
   *
   * @param number number
   * @param printDate print date
   */
  public void numbered(String number, LocalDate printDate) {
    instrumentNo = number;
    printedOn = printDate;
  }

  /**
   * Records the bank reference (BOB voucher reference, credit ticket reference).
   *
   * @param ref reference
   */
  public void referenced(String ref) {
    reference = ref;
  }

  /**
   * Records to whom a check or MC / DD was released.
   *
   * @param who recipient
   */
  public void releasedTo(String who) {
    releasedTo = who == null || who.length() <= MAX_TEXT ? who : who.substring(0, MAX_TEXT);
  }

  /**
   * Frozen in an end-of-day run.
   *
   * @param runId run
   */
  public void inEod(Long runId) {
    eodRunId = runId;
  }

  /**
   * A short label: the number, or the mode when not yet numbered.
   *
   * @return label
   */
  public String label() {
    return instrumentNo == null ? "instrument" : instrumentNo;
  }

  public Long getVoucherId() {
    return voucherId;
  }

  public DisbursementMode getMode() {
    return mode;
  }

  public String getInstrumentNo() {
    return instrumentNo;
  }

  public InstrumentStatus getStatus() {
    return status;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public Long getBankAccountId() {
    return bankAccountId;
  }

  public String getReference() {
    return reference;
  }

  public LocalDate getPrintedOn() {
    return printedOn;
  }

  public Instant getPrintedAt() {
    return printedAt;
  }

  public Instant getReleasedAt() {
    return releasedAt;
  }

  public String getReleasedTo() {
    return releasedTo;
  }

  public Instant getEmailedAt() {
    return emailedAt;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }

  public Instant getCreditedAt() {
    return creditedAt;
  }

  public Instant getDebitedAt() {
    return debitedAt;
  }

  public Instant getNegotiatedAt() {
    return negotiatedAt;
  }

  public Instant getStaleAt() {
    return staleAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public Long getEodRunId() {
    return eodRunId;
  }
}
