package com.iortatechnxt.brokerverse.collections.worklist.api.dto;

import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange;
import com.iortatechnxt.brokerverse.collections.worklist.domain.Assignment;
import com.iortatechnxt.brokerverse.collections.worklist.service.AccountViewService.Account;
import com.iortatechnxt.brokerverse.collections.worklist.service.AccountViewService.PaymentLine;
import com.iortatechnxt.brokerverse.collections.worklist.service.AccountViewService.Payments;
import com.iortatechnxt.brokerverse.collections.worklist.service.AccountViewService.PolicyView;
import com.iortatechnxt.brokerverse.collections.worklist.service.AccountViewService.TimelineEntry;
import com.iortatechnxt.brokerverse.collections.worklist.service.EditLockService.LockState;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Responses of the collection account page (COLLECTIONS_DESIGN 11; BRCLXN.043, 046, 054-057). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class AccountDtos {

  private AccountDtos() {}

  /**
   * The account header: item, ledger chips, live net PR breakdown and edit lock.
   *
   * @param item item
   * @param ledger ledger facts, null when the invoice left the ledger
   * @param breakdown live balance per PR component (BRCLXN.046)
   * @param lock edit lock
   */
  public record AccountResponse(
      ItemResponse item, LedgerFacts ledger, List<BalanceLine> breakdown, LockResponse lock) {

    /**
     * Maps an account.
     *
     * @param a account
     * @return response
     */
    public static AccountResponse from(Account a) {
      OpsInvoice inv = a.invoice();
      return new AccountResponse(
          ItemResponse.from(a.item()),
          inv == null ? null : LedgerFacts.from(inv),
          inv == null
              ? List.of()
              : inv.getComponents().stream()
                  .filter(c -> BalanceLine.shown(c.getComponent().name()))
                  .map(BalanceLine::from)
                  .toList(),
          LockResponse.from(a.lock()));
    }
  }

  /**
   * Facts of the ledger invoice shown as chips (payment, remittance, hold, DP, CWT, lock).
   *
   * @param kind invoice kind
   * @param rootInvoiceNo original invoice
   * @param accountId account (ARN) id
   * @param paymentStatus payment status
   * @param remittanceStatus remittance status
   * @param hold remittance hold
   * @param pendingNegativeAdjustment pending negative adjustment
   * @param writtenOff written off
   * @param cancelled cancelled
   * @param lockOwner module locking the invoice
   * @param lockReason why
   */
  public record LedgerFacts(
      String kind,
      String rootInvoiceNo,
      Long accountId,
      String paymentStatus,
      String remittanceStatus,
      boolean hold,
      boolean pendingNegativeAdjustment,
      boolean writtenOff,
      boolean cancelled,
      String lockOwner,
      String lockReason) {

    static LedgerFacts from(OpsInvoice i) {
      return new LedgerFacts(
          i.getKind().name(),
          i.getRootInvoiceNo(),
          i.getAccountId(),
          i.getPaymentStatus().name(),
          i.getRemittanceStatus().name(),
          i.isHoldFlag(),
          i.isPendingNegAdj(),
          i.isWrittenOff(),
          i.isCancelled(),
          i.getLockOwner(),
          i.getLockReason());
    }
  }

  /**
   * One component of the net PR breakdown (BRCLXN.046).
   *
   * @param component component
   * @param booked booked
   * @param adjusted endorsements, corrections, cancellations
   * @param applied payments net of reversals
   * @param writtenOff write-offs and reversals
   * @param balance outstanding
   */
  public record BalanceLine(
      String component,
      BigDecimal booked,
      BigDecimal adjusted,
      BigDecimal applied,
      BigDecimal writtenOff,
      BigDecimal balance) {

    static boolean shown(String component) {
      return List.of("BASIC", "DST", "PREMIUM_TAX_VAT", "LGT", "FST", "OTHER", "PR2307")
          .contains(component);
    }

    static BalanceLine from(OpsInvoiceComponent c) {
      return new BalanceLine(
          c.getComponent().name(),
          c.getBooked(),
          c.getAdjusted(),
          c.netApplied(),
          c.getWrittenOff(),
          c.getBalance());
    }
  }

  /**
   * The edit lock (NFR "&lt;user&gt; is editing").
   *
   * @param editingBy holder, null when free
   * @param since taken at
   * @param mine whether the caller holds it
   */
  public record LockResponse(String editingBy, Instant since, boolean mine) {

    /**
     * Maps a lock state.
     *
     * @param s state
     * @return response
     */
    public static LockResponse from(LockState s) {
      return new LockResponse(s.editingBy(), s.since(), s.mine());
    }
  }

  /**
   * Payments of an account (BRCLXN.054).
   *
   * @param booked premium due
   * @param paid applied net of reversals
   * @param outstanding still open
   * @param lines one line per transaction
   */
  public record PaymentsResponse(
      BigDecimal booked, BigDecimal paid, BigDecimal outstanding, List<PaymentLine> lines) {

    /**
     * Maps payments.
     *
     * @param p payments
     * @return response
     */
    public static PaymentsResponse from(Payments p) {
      return new PaymentsResponse(p.booked(), p.paid(), p.outstanding(), p.lines());
    }
  }

  /**
   * Policy, invoice family and co-insurance (BRCLXN.056).
   *
   * @param invoiceNo invoice
   * @param policyNo policy
   * @param policyYear policy year
   * @param arn account
   * @param accountId account id
   * @param productLine product line
   * @param riskCode risk code
   * @param bookingDate booking date
   * @param inceptionDate inception
   * @param expiryDate expiry
   * @param firstReceiptDate first AR applied (receipt date, CQ17)
   * @param shares insurer shares
   * @param family invoices of the family
   */
  public record PolicyResponse(
      String invoiceNo,
      String policyNo,
      int policyYear,
      String arn,
      Long accountId,
      String productLine,
      String riskCode,
      LocalDate bookingDate,
      LocalDate inceptionDate,
      LocalDate expiryDate,
      LocalDate firstReceiptDate,
      List<OpsInvoiceShare> shares,
      List<FamilyLine> family) {

    /**
     * Maps a policy view.
     *
     * @param v view
     * @return response
     */
    public static PolicyResponse from(PolicyView v) {
      OpsInvoice i = v.invoice();
      return new PolicyResponse(
          i.getInvoiceNo(),
          i.getPolicyNo(),
          i.getPolicyYear(),
          i.getArn(),
          i.getAccountId(),
          i.getClassification().productLine(),
          i.getClassification().riskCode(),
          i.getBookingDate(),
          i.getClassification().inceptionDate(),
          i.getClassification().expiryDate(),
          v.firstReceiptDate(),
          i.getShares(),
          v.family().stream().map(FamilyLine::from).toList());
    }
  }

  /**
   * An invoice of the family (booking, endorsements, cancellations).
   *
   * @param invoiceNo invoice
   * @param kind kind
   * @param endorsementNo endorsement
   * @param bookingDate booking date
   * @param grossPremium gross premium
   * @param premiumBalance premium balance
   * @param paymentStatus payment status
   */
  public record FamilyLine(
      String invoiceNo,
      String kind,
      String endorsementNo,
      LocalDate bookingDate,
      BigDecimal grossPremium,
      BigDecimal premiumBalance,
      String paymentStatus) {

    static FamilyLine from(OpsInvoice i) {
      return new FamilyLine(
          i.getInvoiceNo(),
          i.getKind().name(),
          i.getEndorsementNo(),
          i.getBookingDate(),
          i.getGrossPremium(),
          i.premiumBalance(),
          i.getPaymentStatus().name());
    }
  }

  /**
   * An assignment of the account (BRCLXN.052).
   *
   * @param id id
   * @param handler handler
   * @param previousHandler previous handler
   * @param kind kind
   * @param validFrom from
   * @param validTo to (temporary)
   * @param reason reason
   * @param assignedBy by
   * @param endedAt ended at (temporary)
   * @param bulkRef bulk reference
   * @param createdAt when
   */
  public record AssignmentResponse(
      Long id,
      String handler,
      String previousHandler,
      String kind,
      LocalDate validFrom,
      LocalDate validTo,
      String reason,
      String assignedBy,
      Instant endedAt,
      String bulkRef,
      Instant createdAt) {

    /**
     * Maps an assignment.
     *
     * @param a assignment
     * @return response
     */
    public static AssignmentResponse from(Assignment a) {
      return new AssignmentResponse(
          a.getId(),
          a.getHandlerUsername(),
          a.getPreviousHandler(),
          a.getKind().name(),
          a.getValidFrom(),
          a.getValidTo(),
          a.getReason(),
          a.getAssignedBy(),
          a.getRevertedAt(),
          a.getBulkRef(),
          a.getCreatedAt());
    }
  }

  /**
   * A field change (BRCLXN.043).
   *
   * @param id id
   * @param entity record type
   * @param field field
   * @param oldValue from
   * @param newValue to
   * @param username user
   * @param changedAt when
   * @param sourceIp source IP
   * @param bulkRef bulk reference
   */
  public record FieldChangeResponse(
      Long id,
      String entity,
      String field,
      String oldValue,
      String newValue,
      String username,
      Instant changedAt,
      String sourceIp,
      String bulkRef) {

    /**
     * Maps a change.
     *
     * @param c change
     * @return response
     */
    public static FieldChangeResponse from(FieldChange c) {
      return new FieldChangeResponse(
          c.getId(),
          c.getEntity(),
          c.getField(),
          c.getOldValue(),
          c.getNewValue(),
          c.getUsername(),
          c.getChangedAt(),
          c.getSourceIp(),
          c.getBulkRef());
    }
  }

  /**
   * A timeline entry (BRCLXN.057).
   *
   * @param at when
   * @param kind kind
   * @param title title
   * @param detail detail
   * @param by user
   * @param amount amount on the premium
   */
  public record TimelineResponse(
      Instant at, String kind, String title, String detail, String by, BigDecimal amount) {

    /**
     * Maps an entry.
     *
     * @param e entry
     * @return response
     */
    public static TimelineResponse from(TimelineEntry e) {
      return new TimelineResponse(e.at(), e.kind(), e.title(), e.detail(), e.by(), e.amount());
    }
  }
}
