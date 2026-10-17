package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceMovement;
import com.iortatechnxt.brokerverse.opsledger.service.port.PolicyTransactionSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The year-end adjustments of the opening position of legacy invoices (DATA_MIGRATION_DESIGN 17.7)
 * in the policy transaction history: one row per true-up and invoice, with the change of each
 * component it adjusted, the amount it wrote off and the journals it posted.
 */
@Component
@Transactional(readOnly = true)
public class LegacyTrueUpTransactions implements PolicyTransactionSource {

  private static final String PREFIX = "MIG:TU:";

  private final InvoiceLedgerQueryService ledger;
  private final MigratedInvoiceHistory history;

  /**
   * Creates the source.
   *
   * @param ledger invoice ledger
   * @param history migration journals of an invoice
   */
  public LegacyTrueUpTransactions(
      InvoiceLedgerQueryService ledger, MigratedInvoiceHistory history) {
    this.ledger = ledger;
    this.history = history;
  }

  @Override
  public List<SourcedTransaction> transactionsFor(Collection<String> invoiceNos) {
    List<SourcedTransaction> out = new ArrayList<>();
    for (String invoiceNo : invoiceNos) {
      OpsInvoice invoice = ledger.find(invoiceNo).orElse(null);
      if (invoice == null || !invoice.getLegacy().isLegacy()) {
        continue;
      }
      Map<String, List<OpsInvoiceMovement>> groups = new LinkedHashMap<>();
      for (OpsInvoiceMovement m : ledger.movements(invoiceNo)) {
        if (isTrueUp(m)) {
          groups.computeIfAbsent(trueUpNo(m), k -> new ArrayList<>()).add(m);
        }
      }
      groups.forEach((no, group) -> out.add(transaction(invoice, no, group)));
    }
    return out;
  }

  private static boolean isTrueUp(OpsInvoiceMovement m) {
    return m.getSourceRef() != null
        && m.getSourceRef().startsWith(PREFIX)
        && (m.getMovementType() == MovementType.LEGACY_ADJUSTED
            || m.getMovementType() == MovementType.LEGACY_WRITTEN_OFF);
  }

  private SourcedTransaction transaction(
      OpsInvoice invoice, String trueUpNo, List<OpsInvoiceMovement> group) {
    Map<LedgerComponent, BigDecimal> changes = new EnumMap<>(LedgerComponent.class);
    BigDecimal writtenOff = BigDecimal.ZERO;
    for (OpsInvoiceMovement m : group) {
      if (m.getMovementType() == MovementType.LEGACY_WRITTEN_OFF) {
        writtenOff = writtenOff.add(m.getAmount().abs());
      } else {
        changes.merge(m.getComponent(), m.getAmount(), BigDecimal::add);
      }
    }
    OpsInvoiceMovement head = group.get(0);
    String prefix = head.getSourceRef().substring(0, head.getSourceRef().lastIndexOf(':') + 1);
    List<String> batches = history.journals(invoice, prefix);
    String detail = MigratedInvoiceHistory.detail(invoice);
    if (writtenOff.signum() != 0) {
      detail = detail + ", written off " + writtenOff.toPlainString();
    }
    return new SourcedTransaction(
        Kind.ADJUSTMENT,
        trueUpNo,
        null,
        invoice.getInvoiceNo(),
        null,
        head.getValueDate(),
        null,
        changes.isEmpty()
            ? "Year-end Write-off of Legacy Position"
            : "Year-end Adjustment of Legacy Position",
        detail,
        changes,
        "POSTED",
        "Posted",
        true,
        batches);
  }

  /** The true-up number of a movement keyed by true-up, item and component. */
  private static String trueUpNo(OpsInvoiceMovement m) {
    String rest = m.getSourceRef().substring(PREFIX.length());
    int end = rest.indexOf(':');
    return end < 0 ? rest : rest.substring(0, end);
  }
}
