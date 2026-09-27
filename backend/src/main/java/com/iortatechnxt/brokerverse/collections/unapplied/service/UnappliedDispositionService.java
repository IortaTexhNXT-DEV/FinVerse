package com.iortatechnxt.brokerverse.collections.unapplied.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.common.domain.FieldChange.Target;
import com.iortatechnxt.brokerverse.collections.common.service.ChangeRecorder;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.UnappliedDisposition;
import com.iortatechnxt.brokerverse.collections.unapplied.domain.UnappliedDispositionRepository;
import com.iortatechnxt.brokerverse.collections.unapplied.service.UnappliedRules.Rule;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedEvent;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedDirectory.UnappliedView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Collector dispositions of unapplied payments (BRCLXN.030-033, 037-040, 047/048): a Collection
 * Handler or an Unapplied Payment Handler ({@code CLX_UNAPPLIED_WORK}) documents the disposition of
 * an item read from Cashiering through {@link UnappliedDirectory}; a value with a Cashiering action
 * also sends a request to Cashiering (e.g. "For application to invoice" with a valid invoice number
 * becomes an application request). Dispositions are append-only; the history merges them with
 * Cashiering's own history of the item.
 */
@Service
@Transactional
public class UnappliedDispositionService {

  static final String ENTITY = "CollectionsUnapplied";

  private final UnappliedDispositionRepository dispositions;
  private final UnappliedDirectory directory;
  private final UnappliedRules rules;
  private final ApplicationRequestService requests;
  private final ChangeRecorder changes;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param dispositions collector dispositions
   * @param directory Cashiering's unapplied items
   * @param rules disposition rules
   * @param requests requests to Cashiering
   * @param changes field change log
   * @param audit audit trail
   */
  public UnappliedDispositionService(
      UnappliedDispositionRepository dispositions,
      UnappliedDirectory directory,
      UnappliedRules rules,
      ApplicationRequestService requests,
      ChangeRecorder changes,
      AuditTrailService audit) {
    this.dispositions = dispositions;
    this.directory = directory;
    this.rules = rules;
    this.requests = requests;
    this.changes = changes;
    this.audit = audit;
  }

  /**
   * An unapplied item of a company.
   *
   * @param companyId company
   * @param unappliedRef item reference
   * @return item
   */
  @Transactional(readOnly = true)
  public UnappliedView item(Long companyId, String unappliedRef) {
    return directory
        .find(unappliedRef)
        .filter(v -> v.companyId().equals(companyId))
        .orElseThrow(() -> new ResourceNotFoundException("Unapplied payment", unappliedRef));
  }

  /**
   * Records a collector disposition and, when its value has a Cashiering action, sends the request.
   *
   * @param companyId company
   * @param unappliedRef item reference
   * @param input disposition, invoice, amount and remarks
   * @return the disposition
   */
  public UnappliedDisposition dispose(Long companyId, String unappliedRef, DispositionInput input) {
    UnappliedView item = item(companyId, unappliedRef);
    requireAmount(item, input.amount());
    Rule rule = rules.require(input.dispositionCode());
    OpsInvoice invoice = invoice(companyId, rule, blankToNull(input.invoiceNo()));
    String invoiceNo = invoice == null ? null : invoice.getInvoiceNo();
    BigDecimal amount = input.amount();
    String before = latest(companyId, unappliedRef);
    UnappliedDisposition d =
        dispositions.save(
            new UnappliedDisposition(
                companyId,
                unappliedRef,
                new UnappliedDisposition.Content(
                    rule.code(),
                    rule.cashieringAction(),
                    invoiceNo,
                    amount,
                    blankToNull(input.remarks()))));
    if (rule.sendsRequest()) {
      d.linkRequest(
          requests.send(item, d, invoice == null ? null : invoice.getAssuredName()).getId());
    }
    changes.record(
        new Target(companyId, ENTITY, unappliedRef, null),
        "disposition",
        before,
        rule.code(),
        null);
    audit.record(
        ENTITY,
        unappliedRef,
        AuditAction.UPDATE,
        "Collector disposition " + rule.code() + (invoiceNo == null ? "" : " to " + invoiceNo));
    return d;
  }

  private static void requireAmount(UnappliedView item, BigDecimal amount) {
    if (item.balance() == null || item.balance().signum() <= 0) {
      throw new BusinessRuleException(
          "CLX_UNAPPLIED_NO_BALANCE", item.unappliedRef() + " has no unapplied balance left");
    }
    if (amount != null && (amount.signum() <= 0 || amount.compareTo(item.balance()) > 0)) {
      throw new BusinessRuleException(
          "CLX_UNAPPLIED_AMOUNT",
          "The amount must be above zero and at most the balance " + item.balance());
    }
  }

  private OpsInvoice invoice(Long companyId, Rule rule, String invoiceNo) {
    return rule.requiresInvoice() || invoiceNo != null
        ? rules.requireInvoice(companyId, invoiceNo)
        : null;
  }

  private String latest(Long companyId, String unappliedRef) {
    List<UnappliedDisposition> earlier =
        dispositions.findByCompanyIdAndUnappliedRefOrderByIdDesc(companyId, unappliedRef);
    return earlier.isEmpty() ? null : earlier.get(0).getDispositionCode();
  }

  /**
   * Collector dispositions of an item.
   *
   * @param companyId company
   * @param unappliedRef item reference
   * @return dispositions, newest first
   */
  @Transactional(readOnly = true)
  public List<UnappliedDisposition> of(Long companyId, String unappliedRef) {
    return dispositions.findByCompanyIdAndUnappliedRefOrderByIdDesc(companyId, unappliedRef);
  }

  /**
   * The history of an item (BRCLXN.040): Cashiering's events and the collector dispositions, oldest
   * first.
   *
   * @param companyId company
   * @param unappliedRef item reference
   * @return events
   */
  @Transactional(readOnly = true)
  public List<UnappliedEvent> history(Long companyId, String unappliedRef) {
    item(companyId, unappliedRef);
    List<UnappliedEvent> events = new ArrayList<>(directory.history(unappliedRef));
    for (UnappliedDisposition d : of(companyId, unappliedRef)) {
      events.add(
          new UnappliedEvent(
              d.getCreatedAt(),
              "COLLECTOR_DISPOSITION",
              "Collector disposition "
                  + d.getDispositionCode()
                  + (d.getInvoiceNo() == null ? "" : " to invoice " + d.getInvoiceNo())
                  + (d.getRemarks() == null ? "" : ": " + d.getRemarks()),
              d.getAmount(),
              d.getCreatedBy(),
              d.getRequestId() == null ? null : ApplicationRequestService.KEY_PREFIX + d.getId()));
    }
    events.sort(
        Comparator.comparing(UnappliedEvent::at, Comparator.nullsLast(Comparator.naturalOrder())));
    return events;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * A collector disposition to record.
   *
   * @param dispositionCode value of CLX_UPP_DISPOSITION
   * @param invoiceNo target invoice (mandatory when the value requires one)
   * @param amount amount, null for the whole balance
   * @param remarks remarks
   */
  public record DispositionInput(
      String dispositionCode, String invoiceNo, BigDecimal amount, String remarks) {}
}
