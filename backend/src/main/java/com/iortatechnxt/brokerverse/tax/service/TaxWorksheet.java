package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.tax.domain.ReturnFigures;
import com.iortatechnxt.brokerverse.tax.domain.ReturnLineValues;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import java.util.List;

/**
 * A tax computation for a period: summary lines (the figures of the return), headline figures, the
 * source documents behind them and the reconciliation with the ledger.
 *
 * @param kind worksheet kind
 * @param period period
 * @param lines summary lines in print order
 * @param figures headline figures (base, tax due, credits, payable, excess credit)
 * @param documents drill-down rows
 * @param controls ledger reconciliation
 * @param notes remarks (assumptions applied, unmapped payees, rate differences)
 */
public record TaxWorksheet(
    WorksheetKind kind,
    TaxPeriod period,
    List<ReturnLineValues> lines,
    ReturnFigures figures,
    List<TaxDocumentLine> documents,
    List<LedgerControl> controls,
    List<String> notes) {

  /** Canonical constructor copying the lists. */
  public TaxWorksheet {
    lines = List.copyOf(lines);
    documents = List.copyOf(documents);
    controls = List.copyOf(controls);
    notes = List.copyOf(notes);
  }

  /**
   * Documents of one section.
   *
   * @param section section name
   * @return documents
   */
  public List<TaxDocumentLine> section(String section) {
    return documents.stream().filter(d -> d.section().equals(section)).toList();
  }
}
