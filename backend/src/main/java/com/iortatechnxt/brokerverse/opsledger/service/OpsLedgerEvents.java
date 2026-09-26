package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.FeedSource;
import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Spring events of the Operations ledger (OPERATIONS_DESIGN 2.1), so the Operations modules react
 * to each other without depending on each other. All are published inside the transaction of the
 * change; listen with {@code @TransactionalEventListener(phase = AFTER_COMMIT)} to act on committed
 * data only, or with {@code @EventListener} to take part in the same transaction.
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class OpsLedgerEvents {

  private OpsLedgerEvents() {}

  /**
   * A booked invoice entered the ledger (cashiering re-matches pre-booked payments, CSHID.020;
   * prodrecon auto-matches, PRCID.023).
   *
   * @param companyId company
   * @param invoiceNo invoice number
   * @param arn Account Reference Number
   * @param kind booking, endorsement plus / minus or cancellation
   * @param clientCode client
   * @param insurerCode lead insurer
   * @param policyNo policy number
   * @param directPayment direct payment (BRNB.114)
   * @param source event or replay
   */
  public record OpsInvoiceBooked(
      Long companyId,
      String invoiceNo,
      String arn,
      InvoiceKind kind,
      String clientCode,
      String insurerCode,
      String policyNo,
      boolean directPayment,
      FeedSource source) {}

  /**
   * Movements were posted on an invoice (RMTID.038).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param type movement type
   * @param sourceModule source module
   * @param sourceRef source reference
   * @param amounts signed amount per component
   */
  public record InvoiceMovementPosted(
      Long companyId,
      String invoiceNo,
      MovementType type,
      String sourceModule,
      String sourceRef,
      Map<LedgerComponent, BigDecimal> amounts) {

    /** Defensive copy. */
    public InvoiceMovementPosted {
      amounts = Map.copyOf(amounts);
    }
  }

  /**
   * A flag of an invoice changed (hold, pending negative adjustment, written off, cancelled,
   * estimated).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param flag flag
   * @param value new value
   * @param module module that changed it
   * @param reason reason
   */
  public record InvoiceFlagChanged(
      Long companyId,
      String invoiceNo,
      InvoiceFlag flag,
      boolean value,
      String module,
      String reason) {}

  /**
   * The remittance status of an invoice changed (RMTID.019/032).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param from old status
   * @param to new status
   * @param module module that changed it (OPSLEDGER when derived from the payment status)
   */
  public record RemittanceStatusChanged(
      Long companyId,
      String invoiceNo,
      RemittanceStatus from,
      RemittanceStatus to,
      String module) {}

  /**
   * A module locked an invoice (RMTID.040).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param owner module holding the lock
   * @param reason reason
   */
  public record InvoiceLocked(Long companyId, String invoiceNo, String owner, String reason) {}

  /**
   * A module released its lock (RMTID.040).
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param owner module that held the lock
   */
  public record InvoiceUnlocked(Long companyId, String invoiceNo, String owner) {}

  /**
   * An adjustment request that reduces an invoice was raised or ended (RMTID.020/035): published by
   * the adjustment module next to the {@code PENDING_NEG_ADJ} flag, consumed by remittance to
   * exclude the invoice and notify its processor.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param requestNo endorsement request number
   * @param pending true when raised, false when posted or cancelled
   * @param requestedBy user who raised it
   */
  public record NegativeAdjustmentPending(
      Long companyId, String invoiceNo, String requestNo, boolean pending, String requestedBy) {}

  /**
   * A payment request changed status (acknowledged, DV assigned, paid, returned, cancelled;
   * RMTID.019, DIS 2.20.0), or Disbursement reported a new DV stage or instrument status (DIS 2.8,
   * 3.26). Published by the adapter of {@code DisbursementGateway} (the in-app queue, then the
   * {@code disbursement} module).
   *
   * @param companyId company
   * @param requestNo request number
   * @param type request type
   * @param sourceModule module that sent it
   * @param sourceRef its reference
   * @param status new gateway status
   * @param dvNo disbursement voucher number, when assigned
   * @param reason return or cancellation reason
   * @param dvStatus DV stage in Disbursement (IN_PROCESS ... APPROVED, CANCELLED), may be null
   * @param instrumentStatus instrument status (PRINTED, RELEASED, CREDITED, NEGOTIATED, STALE ...),
   *     may be null
   */
  public record DisbursementStatusChanged(
      Long companyId,
      String requestNo,
      DisbursementRequest.Type type,
      String sourceModule,
      String sourceRef,
      DisbursementRequest.Status status,
      String dvNo,
      String reason,
      String dvStatus,
      String instrumentStatus) {

    /**
     * A status change without DV stage or instrument status (in-app queue, BRD-2 contract).
     *
     * @param companyId company
     * @param requestNo request number
     * @param type request type
     * @param sourceModule module that sent it
     * @param sourceRef its reference
     * @param status new status
     * @param dvNo disbursement voucher number, when assigned
     * @param reason return reason, when returned
     */
    public DisbursementStatusChanged(
        Long companyId,
        String requestNo,
        DisbursementRequest.Type type,
        String sourceModule,
        String sourceRef,
        DisbursementRequest.Status status,
        String dvNo,
        String reason) {
      this(companyId, requestNo, type, sourceModule, sourceRef, status, dvNo, reason, null, null);
    }
  }

  /**
   * Collections has items waiting in the outbox of an inbound {@code COLLECTION_*} feed
   * (COLLECTIONS_DESIGN 2.2 and 9): consumers (cashiering, commission) may pull at once instead of
   * waiting for their schedule. Published by the {@code collections} module after commit.
   *
   * @param companyId company
   * @param feedCode feed with pending items (e.g. {@code COLLECTION_CWT2307})
   */
  public record CollectionFeedReady(Long companyId, String feedCode) {}

  /**
   * Cashiering decided a disposition requested through {@code UnappliedDispositionRequests}
   * (BRCLXN.030-033, 040): accepted, rejected or executed. Published by the implementing module
   * (cashiering); consumed by Collections to update its application request.
   *
   * @param companyId company
   * @param unappliedRef unapplied item reference
   * @param source requesting module (COLLECTIONS)
   * @param sourceRef its reference (idempotency key of the request)
   * @param status ACCEPTED, REJECTED or APPLIED
   * @param cashieringRef disposition reference in cashiering
   * @param message reason or remarks, may be null
   */
  public record UnappliedDispositionChanged(
      Long companyId,
      String unappliedRef,
      String source,
      String sourceRef,
      String status,
      String cashieringRef,
      String message) {}

  /**
   * A validator answered a refund validation opened through {@code RefundValidationSource} (MKT
   * 1.11.0, ACSL 2.5.5). Published by the validating module (acsl, cashiering); consumed by
   * payrequest.
   *
   * @param companyId company
   * @param validator ACSL or CASHIERING
   * @param sourceModule module that asked (PAYREQUEST)
   * @param sourceRef its reference
   * @param confirmed true when confirmed, false when rejected
   * @param newArNo new acknowledgement receipt number (cashiering reinstatement), may be null
   * @param remarks result remarks, may be null
   */
  public record RefundValidationCompleted(
      Long companyId,
      String validator,
      String sourceModule,
      String sourceRef,
      boolean confirmed,
      String newArNo,
      String remarks) {}

  /**
   * A payment reversal requested through {@code PaymentReversalRequester} was decided (ACSL
   * 2.6.0-2.6.1). Published by cashiering; consumed by acsl.
   *
   * @param companyId company
   * @param sourceModule module that asked (ACSL)
   * @param sourceRef its reference
   * @param approved true when approved and posted, false when rejected
   * @param reference reversal reference in cashiering
   * @param remarks reason or remarks, may be null
   */
  public record PaymentReversalCompleted(
      Long companyId,
      String sourceModule,
      String sourceRef,
      boolean approved,
      String reference,
      String remarks) {}
}
