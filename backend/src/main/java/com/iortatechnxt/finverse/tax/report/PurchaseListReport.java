package com.iortatechnxt.finverse.tax.report;

import com.iortatechnxt.finverse.tax.service.TaxWorksheetService;
import org.springframework.stereotype.Component;

/** TAX-SLP – Summary List of Purchases (purchases per supplier with input VAT). */
@Component
public class PurchaseListReport extends SummaryListReport {

  /**
   * Creates the report.
   *
   * @param worksheets worksheets
   */
  public PurchaseListReport(TaxWorksheetService worksheets) {
    super(worksheets, false);
  }
}
