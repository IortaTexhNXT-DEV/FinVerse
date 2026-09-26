package com.iortatechnxt.brokerverse.underwriting.service;

import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.underwriting.domain.Endorsement;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.brokerverse.underwriting.domain.PremiumBreakdown;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One premium transaction (policy issue or endorsement) with its premium figures, for registers and
 * for other modules (reinsurance cessions, commission, receivables).
 *
 * @param ref transaction key
 * @param endorsementId endorsement id, null for the original issue
 * @param documentNo policy number or endorsement number (policy/Enn)
 * @param kind NEW for the original issue, else the endorsement type
 * @param status status of the document
 * @param policy policy header
 * @param issueDate document issue date
 * @param effectiveDate cover start (policy) or effective date (endorsement)
 * @param uwYear underwriting year of the transaction: the policy's for the original issue, the
 *     endorsement's otherwise (a renewal belongs to the year its new period starts)
 * @param approvalDate approval (accounting) date, null while pending
 * @param createdBy maker
 * @param approvedBy checker
 * @param debitNoteNo client debit / credit note
 * @param creditNoteNo intermediary credit / debit note
 * @param exchangeRate rate to base currency used at approval (null while pending)
 * @param premium premium figures (negative for return premium)
 */
public record PremiumTransaction(
    TransactionRef ref,
    Long endorsementId,
    String documentNo,
    String kind,
    PolicyStatus status,
    PolicySnapshot policy,
    LocalDate issueDate,
    LocalDate effectiveDate,
    int uwYear,
    LocalDate approvalDate,
    String createdBy,
    String approvedBy,
    String debitNoteNo,
    String creditNoteNo,
    BigDecimal exchangeRate,
    PremiumBreakdown premium) {

  /** Kind of an original policy issue. */
  public static final String NEW = "NEW";

  /**
   * Transaction of an original issue.
   *
   * @param p policy
   * @return transaction
   */
  public static PremiumTransaction of(Policy p) {
    return new PremiumTransaction(
        TransactionRef.original(p.getId()),
        null,
        p.getPolicyNo(),
        NEW,
        p.getStatus(),
        PolicySnapshot.of(p),
        p.getIssueDate(),
        p.getPeriodFrom(),
        p.getUwYear(),
        p.getWorkflow().getApprovalDate(),
        p.getCreatedBy(),
        p.getWorkflow().getApprovedBy(),
        p.getRefs().getDebitNoteNo(),
        p.getRefs().getCreditNoteNo(),
        p.getRefs().getExchangeRate(),
        p.getPremium());
  }

  /**
   * Transaction of an endorsement.
   *
   * @param e endorsement (policy loaded)
   * @return transaction
   */
  public static PremiumTransaction of(Endorsement e) {
    return new PremiumTransaction(
        new TransactionRef(e.getPolicy().getId(), e.getEndorsementNo()),
        e.getId(),
        e.documentNo(),
        e.getEndorsementType().name(),
        e.getStatus(),
        PolicySnapshot.of(e.getPolicy()),
        e.getIssueDate(),
        e.getEffectiveDate(),
        e.getUwYear(),
        e.getWorkflow().getApprovalDate(),
        e.getCreatedBy(),
        e.getWorkflow().getApprovedBy(),
        e.getRefs().getDebitNoteNo(),
        e.getRefs().getCreditNoteNo(),
        e.getRefs().getExchangeRate(),
        e.getPremium());
  }

  /**
   * Endorsement number (0 for the original issue).
   *
   * @return number
   */
  public int endorsementNo() {
    return ref.endorsementNo();
  }

  /**
   * Converts an amount of this transaction to base currency at its exchange rate.
   *
   * @param amount amount in the policy currency
   * @return base currency amount (unconverted while no rate is recorded)
   */
  public BigDecimal toBase(BigDecimal amount) {
    return exchangeRate == null ? amount : Money.convert(amount, exchangeRate);
  }
}
