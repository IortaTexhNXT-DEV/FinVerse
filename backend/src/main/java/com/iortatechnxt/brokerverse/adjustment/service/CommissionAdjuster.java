package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.ShareChange;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceRegister;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.CreditRequest;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.IssueRequest;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.Remaining;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A commission change without premium change (increase / decrease in commission, ADJID.014
 * addendum): event {@code OPS_ADJ_COMMISSION} per insurer share, the {@code ADJUSTED} commission
 * movement on the invoice, and a service invoice per insurer for an increase or a credit of the
 * insurer's service invoice for a decrease, through booking's {@code ServiceInvoiceService}.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class CommissionAdjuster {

  /** Service invoice type of endorsements (V870). */
  public static final String SI_TYPE = "INSURER_COMMISSION_ENDT";

  private final AdjustmentEvents events;
  private final LedgerEffects effects;
  private final ServiceInvoiceService serviceInvoices;
  private final ServiceInvoiceRegister register;
  private final Clock clock;

  /**
   * Creates the adjuster.
   *
   * @param events accounting events
   * @param effects ledger movement
   * @param serviceInvoices service invoice issue and credit
   * @param register service invoices of an invoice
   * @param clock clock
   */
  public CommissionAdjuster(
      AdjustmentEvents events,
      LedgerEffects effects,
      ServiceInvoiceService serviceInvoices,
      ServiceInvoiceRegister register,
      Clock clock) {
    this.events = events;
    this.effects = effects;
    this.serviceInvoices = serviceInvoices;
    this.register = register;
    this.clock = clock;
  }

  /**
   * Posts a commission change.
   *
   * @param request request
   * @param invoice invoice
   * @param recompute recompute (commission change per insurer)
   * @param journals journal batches written, completed here
   * @return service invoices issued or credited
   */
  public List<String> adjust(
      EndorsementRequest request, OpsInvoice invoice, Recompute recompute, List<String> journals) {
    LocalDate today = LocalDate.now(clock);
    String ref = Adjustments.sourceRef(request.getRequestNo()) + ":COM:";
    List<String> documents = new ArrayList<>();
    for (ShareChange share : recompute.shares()) {
      if (share.commissionDelta().signum() == 0 && share.vatDelta().signum() == 0) {
        continue;
      }
      JournalBatch journal =
          events.post(
              invoice,
              today,
              new AdjustmentEvents.Spec(
                  AdjustmentEvents.COMMISSION,
                  ref + share.insurerCode(),
                  request.getRequestNo(),
                  share.insurerCode(),
                  "Commission change of "
                      + invoice.getInvoiceNo()
                      + " ("
                      + request.getRequestNo()
                      + ")",
                  Map.of(
                      "COMMISSION_RECEIVABLE", share.commissionDelta().add(share.vatDelta()),
                      "UNREALIZED_COMMISSION", share.commissionDelta(),
                      "DEFERRED_OUTPUT_VAT", share.vatDelta()),
                  Map.of()));
      journals.add(journal.getBatchNo());
      serviceInvoice(request, invoice, share).ifPresent(documents::add);
    }
    BigDecimal commission = recompute.commission().commission();
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    amounts.put(LedgerComponent.COMMISSION, commission);
    amounts.put(LedgerComponent.COMMISSION_VAT, recompute.commission().vatOnCommission());
    amounts.put(LedgerComponent.WTAX, LedgerEffects.wtax(commission, invoice.getWtaxRate()));
    effects.adjust(request, amounts, journals.isEmpty() ? null : journals.get(0));
    return documents;
  }

  private Optional<String> serviceInvoice(
      EndorsementRequest request, OpsInvoice invoice, ShareChange share) {
    if (share.commissionDelta().signum() > 0) {
      ServiceInvoice issued =
          serviceInvoices.issue(
              new IssueRequest(
                  invoice.getCompanyId(),
                  SI_TYPE,
                  invoice.getInvoiceNo(),
                  invoice.getArn(),
                  share.insurerCode(),
                  null,
                  LocalDate.now(clock),
                  invoice.getCurrency(),
                  share.commissionDelta(),
                  share.vatDelta(),
                  LedgerEffects.wtax(share.commissionDelta(), invoice.getWtaxRate()),
                  "Commission increase " + request.getRequestNo()));
      return Optional.of(issued.getSiNo());
    }
    return register.forInvoice(invoice.getInvoiceNo()).stream()
        .filter(s -> s.getKind() == SiKind.INVOICE)
        .filter(s -> s.getRecipientCode().equals(share.insurerCode()))
        .findFirst()
        .flatMap(original -> credit(request, original, share));
  }

  private Optional<String> credit(
      EndorsementRequest request, ServiceInvoice original, ShareChange share) {
    Remaining left = serviceInvoices.remaining(original);
    BigDecimal commission = share.commissionDelta().negate().min(left.commission());
    BigDecimal vat = share.vatDelta().negate().min(left.vat());
    if (!left.allows(commission.max(BigDecimal.ZERO), vat.max(BigDecimal.ZERO))) {
      return Optional.empty();
    }
    return Optional.of(
        serviceInvoices
            .credit(
                original.getSiNo(),
                new CreditRequest(
                    commission.max(BigDecimal.ZERO),
                    vat.max(BigDecimal.ZERO),
                    null,
                    "Commission decrease " + request.getRequestNo()))
            .getSiNo());
  }
}
