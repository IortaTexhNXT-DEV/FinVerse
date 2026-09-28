package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.bulk.service.TextLayout;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.CwtPath;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag.CwtDetails;
import com.iortatechnxt.brokerverse.common.excel.GuideColumn.Choice;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * {@code CWT_TAGS} (MKTID.013): bulk BIR 2307 tagging by Marketing Collection, one {@code CWT-}
 * reference per invoice (until the Collection or Marketing system sends the tags, OQ45).
 */
@Component
public class CwtTagHandler implements BulkImportHandler {

  private static final String PATH = "Path";
  private static final String CERTIFICATE = "Certificate no";
  private static final Set<String> PATHS = Set.of("CASH", "CERTIFICATE");

  private final CwtService cwt;
  private final PaymentFileLayouts layouts;

  /**
   * Creates the handler.
   *
   * @param cwt BIR 2307
   * @param layouts layouts
   */
  public CwtTagHandler(CwtService cwt, PaymentFileLayouts layouts) {
    this.cwt = cwt;
    this.layouts = layouts;
  }

  @Override
  public String code() {
    return "CWT_TAGS";
  }

  @Override
  public String title() {
    return "BIR 2307 Tagging";
  }

  @Override
  public String permission() {
    return "CWT_TAG";
  }

  @Override
  public String filledBy() {
    return "Cashiering officers tagging the BIR 2307 certificates received from clients";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
                "Invoice no", "Invoice the 2% creditable withholding tax is on", "BI-2026-000001")
            .master("invoice"),
        new BulkColumn(
            "Amount", "Amount of the 2% tax; blank for the expected 2%", false, Type.NUMBER, ""),
        BulkColumn.required(PATH, "How the tax is settled", "CERTIFICATE")
            .choices(
                List.of(
                    new Choice("CASH", "Paid in cash by the client"),
                    new Choice("CERTIFICATE", "Covered by a BIR 2307 certificate"))),
        BulkColumn.optional(CERTIFICATE, "Number of the BIR 2307 certificate", "2307-2026-0001")
            .when("the path is CERTIFICATE"),
        new BulkColumn(
                "Period from", "Start of the certificate period", false, Type.DATE, "2026-07-01")
            .when("the path is CERTIFICATE"),
        new BulkColumn("Period to", "End of the certificate period", false, Type.DATE, "2026-09-30")
            .when("the path is CERTIFICATE"),
        BulkColumn.optional("Remarks", "Remarks", ""));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text("Invoice no");
  }

  @Override
  public String sanitize(String header, String value) {
    String clean = BulkImportHandler.super.sanitize(header, value);
    return PATH.equals(header) ? pathOf(clean) : clean;
  }

  private static String pathOf(String text) {
    String s = text.strip();
    for (CwtPath path : CwtPath.values()) {
      String name = path.name();
      if (name.length() == s.length() && name.regionMatches(true, 0, s, 0, s.length())) {
        return name;
      }
    }
    return text;
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (!PATHS.contains(row.text(PATH))) {
      errors.add("Path must be CASH or CERTIFICATE");
    } else if ("CERTIFICATE".equals(row.text(PATH)) && row.text(CERTIFICATE) == null) {
      errors.add("A certificate tag needs the certificate number");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    return cwt.tag(
            context.companyId(),
            row.text("Invoice no"),
            new CwtDetails(
                row.number("Amount"),
                CwtPath.valueOf(row.text(PATH)),
                row.text(CERTIFICATE),
                row.date("Period from"),
                row.date("Period to"),
                row.text("Remarks")))
        .getReference();
  }

  @Override
  public TextLayout textLayout() {
    return layouts.layout(code());
  }
}
