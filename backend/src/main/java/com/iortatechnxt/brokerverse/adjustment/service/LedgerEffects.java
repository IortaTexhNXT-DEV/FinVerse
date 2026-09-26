package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponent;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.service.InsurerShareAllocator;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier.ReapplyRequest;
import com.iortatechnxt.brokerverse.opsledger.service.port.PaymentReapplier.ReapplyResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a posted request does to its invoice in the Operations ledger, in the posting transaction:
 *
 * <ul>
 *   <li>the {@code ADJUSTED} movement of a decrease or cancellation on the original invoice (the
 *       return invoice booked by booking is informational and is offset by {@code
 *       ReturnInvoiceOffset});
 *   <li>the AR Insurer of a decrease already remitted (row 18): event {@code OPS_AR_INSURER_SETUP}
 *       per insurer share and the negative DTIP reclassified in the ledger;
 *   <li>the re-application of the invoice's payments through the cashiering port (row 17,
 *       ADJID.009/012/013); while cashiering is not installed the default port refuses ({@code
 *       PAYMENT_REAPPLIER_UNAVAILABLE}) and the request waits for re-application.
 * </ul>
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class LedgerEffects {

  /** Error code of the default payment re-applier (no cashiering module). */
  public static final String REAPPLIER_UNAVAILABLE = "PAYMENT_REAPPLIER_UNAVAILABLE";

  private static final int MONEY = 2;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final InvoiceLedgerService ledger;
  private final InvoiceLedgerQueryService queries;
  private final AdjustmentEvents events;
  private final PaymentReapplier reapplier;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param ledger ledger writes
   * @param queries ledger reads
   * @param events accounting events
   * @param reapplier cashiering port
   * @param clock clock
   */
  public LedgerEffects(
      InvoiceLedgerService ledger,
      InvoiceLedgerQueryService queries,
      AdjustmentEvents events,
      PaymentReapplier reapplier,
      Clock clock) {
    this.ledger = ledger;
    this.queries = queries;
    this.events = events;
    this.reapplier = reapplier;
    this.clock = clock;
  }

  /**
   * Posts a return invoice's amounts on the original invoice as an adjustment.
   *
   * @param request request
   * @param returned return invoice as booked (negative amounts)
   * @param journalBatchNo booking journal of the return invoice, null when none
   */
  public void adjustOriginal(
      EndorsementRequest request, InvoiceBooked returned, String journalBatchNo) {
    adjust(request, amountsOf(returned), journalBatchNo);
  }

  /**
   * Posts signed amounts on the request's invoice as an adjustment.
   *
   * @param request request
   * @param amounts signed amounts per component
   * @param journalBatchNo journal batch carrying the change, null when none
   */
  public void adjust(
      EndorsementRequest request, Map<LedgerComponent, BigDecimal> amounts, String journalBatchNo) {
    ledger.post(
        new MovementRequest(
            request.getSubject().invoiceNo(),
            MovementType.ADJUSTED,
            Adjustments.MODULE,
            Adjustments.sourceRef(request.getRequestNo()),
            LocalDate.now(clock),
            amounts,
            new DocumentRefs(null, null, request.outcome().batchNo(), journalBatchNo),
            request.getTerms().endorsementType() + " " + request.getRequestNo()));
  }

  /**
   * Sets up the AR Insurer of a decrease already remitted: the DTIP the ledger now shows as
   * negative (the insurer was paid more than it is due) is reclassified to AR Insurer.
   *
   * @param request request (posted amounts already on the invoice)
   * @param journals journal batches written, completed with the AR Insurer journals
   * @return the AR Insurer amount, zero when none
   */
  public BigDecimal setUpArInsurer(EndorsementRequest request, List<String> journals) {
    OpsInvoice invoice = queries.require(request.getSubject().invoiceNo());
    OpsInvoiceComponent dtip = invoice.component(LedgerComponent.DTIP);
    BigDecimal amount = dtip.getBalance().negate().min(dtip.getRemitted());
    if (amount.signum() <= 0) {
      return BigDecimal.ZERO.setScale(MONEY);
    }
    String ref = Adjustments.sourceRef(request.getRequestNo()) + ":ARI";
    LocalDate today = LocalDate.now(clock);
    InsurerShareAllocator.allocate(amount, invoice.getShares())
        .forEach(
            (insurer, part) -> {
              if (part.signum() != 0) {
                JournalBatch journal =
                    events.post(
                        invoice,
                        today,
                        new AdjustmentEvents.Spec(
                            AdjustmentEvents.AR_INSURER_SETUP,
                            ref + ":" + insurer,
                            request.getRequestNo(),
                            insurer,
                            "AR Insurer - return premium of "
                                + invoice.getInvoiceNo()
                                + " already remitted ("
                                + request.getRequestNo()
                                + ")",
                            Map.of("AR_INSURER", part),
                            Map.of()));
                journals.add(journal.getBatchNo());
              }
            });
    ledger.post(
        new MovementRequest(
            invoice.getInvoiceNo(),
            MovementType.ADJUSTED,
            Adjustments.MODULE,
            ref,
            today,
            Map.of(LedgerComponent.DTIP, amount),
            new DocumentRefs(null, null, request.outcome().batchNo(), null),
            "Negative DTIP reclassified to AR Insurer (" + request.getRequestNo() + ")"));
    return amount;
  }

  /**
   * Re-applies the payments of the request's invoice through cashiering.
   *
   * @param request request
   * @return the result, empty while cashiering is not installed
   */
  public Optional<ReapplyResult> reapply(EndorsementRequest request) {
    try {
      return Optional.of(
          reapplier.reapply(
              new ReapplyRequest(
                  request.getSubject().invoiceNo(),
                  Adjustments.MODULE,
                  Adjustments.sourceRef(request.getRequestNo()),
                  LocalDate.now(clock),
                  request.getTerms().endorsementType() + " " + request.getRequestNo())));
    } catch (BusinessRuleException e) {
      if (REAPPLIER_UNAVAILABLE.equals(e.getCode())) {
        return Optional.empty();
      }
      throw e;
    }
  }

  /**
   * The ledger amounts of a booked invoice: premium by component, DTIP, commission, VAT and
   * withholding tax.
   *
   * @param booked booked invoice
   * @return signed amounts
   */
  static Map<LedgerComponent, BigDecimal> amountsOf(InvoiceBooked booked) {
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    for (Map.Entry<PremiumComponent, BigDecimal> c : booked.components().entrySet()) {
      amounts.put(LedgerComponent.of(c.getKey()), c.getValue());
    }
    amounts.put(LedgerComponent.DTIP, booked.grossPremium());
    amounts.put(LedgerComponent.COMMISSION, booked.commission());
    amounts.put(LedgerComponent.COMMISSION_VAT, booked.vatOnCommission());
    amounts.put(LedgerComponent.WTAX, wtax(booked.commission(), booked.wtaxRate()));
    return amounts;
  }

  /**
   * Withholding tax on a commission.
   *
   * @param commission commission (signed)
   * @param rate rate in percent
   * @return tax
   */
  static BigDecimal wtax(BigDecimal commission, BigDecimal rate) {
    if (rate == null) {
      return BigDecimal.ZERO.setScale(MONEY);
    }
    return commission.multiply(rate).divide(HUNDRED, MONEY, RoundingMode.HALF_UP);
  }
}
