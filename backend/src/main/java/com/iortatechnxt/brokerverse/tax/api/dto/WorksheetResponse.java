package com.iortatechnxt.brokerverse.tax.api.dto;

import com.iortatechnxt.brokerverse.tax.domain.ReturnFigures;
import com.iortatechnxt.brokerverse.tax.domain.ReturnLineValues;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.TaxDocumentLine;
import com.iortatechnxt.brokerverse.tax.service.TaxWorksheet;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A worksheet with its drill-down documents and ledger reconciliation.
 *
 * @param kind worksheet kind
 * @param from period start
 * @param to period end
 * @param periodLabel period label (2026-Q1, 2026-03)
 * @param lines summary lines
 * @param figures headline figures
 * @param documents source documents
 * @param controls ledger reconciliation
 * @param notes remarks
 */
public record WorksheetResponse(
    WorksheetKind kind,
    LocalDate from,
    LocalDate to,
    String periodLabel,
    List<ReturnLineValues> lines,
    ReturnFigures figures,
    List<TaxDocumentLine> documents,
    List<Control> controls,
    List<String> notes) {

  /**
   * Maps a worksheet.
   *
   * @param w worksheet
   * @return response
   */
  public static WorksheetResponse from(TaxWorksheet w) {
    return new WorksheetResponse(
        w.kind(),
        w.period().from(),
        w.period().to(),
        w.period().label(),
        w.lines(),
        w.figures(),
        w.documents(),
        w.controls().stream()
            .map(
                c ->
                    new Control(
                        c.accountCode(),
                        c.description(),
                        c.perDocuments(),
                        c.perLedger(),
                        c.difference()))
            .toList(),
        w.notes());
  }

  /**
   * Ledger reconciliation line.
   *
   * @param accountCode GL account(s)
   * @param description what is reconciled
   * @param perDocuments documents total
   * @param perLedger ledger movement
   * @param difference ledger − documents
   */
  public record Control(
      String accountCode,
      String description,
      BigDecimal perDocuments,
      BigDecimal perLedger,
      BigDecimal difference) {}
}
