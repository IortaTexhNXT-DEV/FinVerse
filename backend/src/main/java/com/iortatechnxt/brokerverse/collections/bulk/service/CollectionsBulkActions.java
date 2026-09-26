package com.iortatechnxt.brokerverse.collections.bulk.service;

import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService.ManualEscalation;
import com.iortatechnxt.brokerverse.collections.installment.service.LedgerBalances;
import com.iortatechnxt.brokerverse.collections.promise.domain.PaymentPromise;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService.PromiseInput;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bulk actions on selected collection accounts (BRCLXN.050/051): escalate several invoices at once
 * (one escalation per account, the invoices of the account together) and record the same promise to
 * pay on several invoices. Each account is validated and processed in its own transaction and gets
 * its own outcome, so one refusal never undoes the others; the records carry a bulk reference for
 * the audit.
 */
@Service
public class CollectionsBulkActions {

  private final LedgerBalances ledger;
  private final EscalationService escalations;
  private final PromiseService promises;
  private final DocumentNumberService numbers;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param ledger ledger reads
   * @param escalations escalations
   * @param promises promises
   * @param numbers bulk references
   * @param txManager transactions
   * @param clock clock
   */
  public CollectionsBulkActions(
      LedgerBalances ledger,
      EscalationService escalations,
      PromiseService promises,
      DocumentNumberService numbers,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.ledger = ledger;
    this.escalations = escalations;
    this.promises = promises;
    this.numbers = numbers;
    this.tx = new TransactionTemplate(txManager);
    this.clock = clock;
  }

  /**
   * Escalates invoices manually (BRCLXN.050): one escalation per account with its selected
   * invoices, regardless of the automatic status.
   *
   * @param request invoices, target, reason and remarks
   * @return one outcome per invoice
   */
  public List<ItemResult> escalate(ManualEscalation request) {
    List<ItemResult> results = new ArrayList<>();
    Map<String, List<Candidate>> byAccount = new LinkedHashMap<>();
    for (String invoiceNo : distinct(request.invoiceNos())) {
      try {
        Candidate account = inTx(() -> openAccount(request.companyId(), invoiceNo));
        byAccount.computeIfAbsent(account.arn(), k -> new ArrayList<>()).add(account);
      } catch (BusinessRuleException | ResourceNotFoundException ex) {
        results.add(ItemResult.refused(invoiceNo, ex.getMessage()));
      }
    }
    String bulkRef = request.invoiceNos().size() > 1 ? inTx(this::bulkRef) : null;
    byAccount.forEach(
        (arn, invoices) -> {
          try {
            Escalation e = inTx(() -> escalations.raiseManual(request, invoices, bulkRef));
            invoices.forEach(
                c ->
                    results.add(
                        ItemResult.done(
                            c.invoiceNo(),
                            "Escalated in " + e.getEscalationNo() + " (" + e.getStatus() + ")")));
          } catch (BusinessRuleException | ResourceNotFoundException ex) {
            invoices.forEach(c -> results.add(ItemResult.refused(c.invoiceNo(), ex.getMessage())));
          }
        });
    return results;
  }

  /**
   * Records the same promise to pay on several invoices (BRCLXN.051/055). Without an amount each
   * invoice's whole outstanding is promised.
   *
   * @param companyId company
   * @param invoiceNos invoices
   * @param input dates, amount and remarks (no installment)
   * @return one outcome per invoice
   */
  public List<ItemResult> promise(Long companyId, List<String> invoiceNos, PromiseInput input) {
    String bulkRef = invoiceNos.size() > 1 ? inTx(this::bulkRef) : null;
    List<ItemResult> results = new ArrayList<>();
    for (String invoiceNo : distinct(invoiceNos)) {
      try {
        PaymentPromise p = inTx(() -> promises.record(companyId, invoiceNo, input, bulkRef));
        results.add(
            ItemResult.done(
                invoiceNo,
                "Promise of "
                    + p.getCurrency()
                    + " "
                    + p.getPromisedAmount()
                    + " by "
                    + p.getPromisedDate()));
      } catch (BusinessRuleException | ResourceNotFoundException ex) {
        results.add(ItemResult.refused(invoiceNo, ex.getMessage()));
      }
    }
    return results;
  }

  private Candidate openAccount(Long companyId, String invoiceNo) {
    OpsInvoice invoice = ledger.requireReceivable(companyId, invoiceNo);
    if (ledger.collected(invoice.premiumBalance())) {
      throw new BusinessRuleException(
          "CLX_NOTHING_TO_COLLECT", "Invoice " + invoiceNo + " has nothing to collect");
    }
    return Candidate.of(invoice);
  }

  private String bulkRef() {
    return numbers.next("CLXB-" + LocalDate.now(clock).getYear());
  }

  private <T> T inTx(Supplier<T> work) {
    return tx.execute(s -> work.get());
  }

  private static List<String> distinct(List<String> invoiceNos) {
    return invoiceNos.stream()
        .filter(n -> n != null && !n.isBlank())
        .map(String::strip)
        .distinct()
        .toList();
  }
}
