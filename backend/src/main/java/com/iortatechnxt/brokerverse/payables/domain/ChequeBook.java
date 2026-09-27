package com.iortatechnxt.brokerverse.payables.domain;

import com.iortatechnxt.brokerverse.common.domain.BaseEntity;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** Range of pre-printed cheque leaves of a bank account; hands out leaves in order. */
@Entity
@Table(name = "pay_cheque_book")
public class ChequeBook extends BaseEntity {

  @Column(name = "bank_account_id", nullable = false)
  private Long bankAccountId;

  @Column(name = "first_no", nullable = false)
  private long firstNo;

  @Column(name = "last_no", nullable = false)
  private long lastNo;

  @Column(name = "next_no", nullable = false)
  private long nextNo;

  @Column(name = "received_on", nullable = false)
  private LocalDate receivedOn;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ChequeBookStatus status = ChequeBookStatus.ACTIVE;

  @Column(name = "edited_by", length = 50)
  private String editedBy;

  @Column(name = "edited_at")
  private Instant editedAt;

  @Column(name = "previous_range", length = 60)
  private String previousRange;

  protected ChequeBook() {}

  /**
   * Registers a cheque book.
   *
   * @param bankAccountId bank account
   * @param firstNo first leaf number
   * @param lastNo last leaf number
   * @param receivedOn date received from the bank
   */
  public ChequeBook(Long bankAccountId, long firstNo, long lastNo, LocalDate receivedOn) {
    if (firstNo <= 0 || lastNo < firstNo) {
      throw new BusinessRuleException(
          "INVALID_CHEQUE_RANGE", "Cheque range " + firstNo + "-" + lastNo + " is invalid");
    }
    this.bankAccountId = bankAccountId;
    this.firstNo = firstNo;
    this.lastNo = lastNo;
    this.nextNo = firstNo;
    this.receivedOn = receivedOn;
  }

  /**
   * Takes the next leaf.
   *
   * @return cheque number, zero padded to the width of the last leaf number
   */
  public String allocate() {
    if (status != ChequeBookStatus.ACTIVE) {
      throw new BusinessRuleException("CHEQUE_BOOK_NOT_ACTIVE", "Cheque book is " + status);
    }
    long number = nextNo;
    nextNo++;
    if (nextNo > lastNo) {
      status = ChequeBookStatus.EXHAUSTED;
    }
    return format(number);
  }

  /**
   * Corrects the beginning check series while no leaf has been used (DIS 2.23.2); the previous
   * range is kept on the book with the user and time of the edit.
   *
   * @param first new first leaf
   * @param last new last leaf
   * @param user user editing
   * @param at time
   */
  public void editRange(long first, long last, String user, Instant at) {
    if (status != ChequeBookStatus.ACTIVE || nextNo != firstNo) {
      throw new BusinessRuleException(
          "CHEQUE_BOOK_IN_USE", "Only an active cheque book with no leaf used can be edited");
    }
    if (first <= 0 || last < first) {
      throw new BusinessRuleException(
          "INVALID_CHEQUE_RANGE", "Cheque range " + first + "-" + last + " is invalid");
    }
    previousRange = firstNo + "-" + lastNo;
    firstNo = first;
    lastNo = last;
    nextNo = first;
    editedBy = user;
    editedAt = at;
  }

  /** Withdraws the book (lost or damaged); unused leaves are never issued. */
  public void cancel() {
    status = ChequeBookStatus.CANCELLED;
  }

  /**
   * Whether two ranges share at least one leaf.
   *
   * @param from first number
   * @param to last number
   * @return true when overlapping
   */
  public boolean overlaps(long from, long to) {
    return from <= lastNo && to >= firstNo;
  }

  /**
   * Leaves not yet used.
   *
   * @return remaining count
   */
  public long remaining() {
    return status == ChequeBookStatus.ACTIVE ? lastNo - nextNo + 1 : 0;
  }

  private String format(long number) {
    int width = String.valueOf(lastNo).length();
    StringBuilder text = new StringBuilder(String.valueOf(number));
    while (text.length() < width) {
      text.insert(0, '0');
    }
    return text.toString();
  }

  public Long getBankAccountId() {
    return bankAccountId;
  }

  public long getFirstNo() {
    return firstNo;
  }

  public long getLastNo() {
    return lastNo;
  }

  public long getNextNo() {
    return nextNo;
  }

  public LocalDate getReceivedOn() {
    return receivedOn;
  }

  public ChequeBookStatus getStatus() {
    return status;
  }

  public String getEditedBy() {
    return editedBy;
  }

  public Instant getEditedAt() {
    return editedAt;
  }

  public String getPreviousRange() {
    return previousRange;
  }
}
