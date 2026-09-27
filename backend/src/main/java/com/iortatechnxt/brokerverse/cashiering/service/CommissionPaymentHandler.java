package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.bulk.service.TextLayout;
import com.iortatechnxt.brokerverse.cashiering.domain.CommissionLine.CommissionDetail;
import com.iortatechnxt.brokerverse.cashiering.domain.OrAmounts;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@code COMMISSION_PAYMENT} (CSHID.007): commission payment details from Collection (insurer,
 * payee, certificate, payment reference, basic commission, VAT and withholding tax per invoice) are
 * staged; "Issue Commission ORs" then issues one OR per insurer, certificate and payment. Missing
 * certificates are flagged in the file validation report.
 */
@Component
public class CommissionPaymentHandler implements BulkImportHandler {

  private static final String GROSS = "Basic commission";
  private static final String VAT = "VAT";
  private static final String WTAX = "WTAX";

  private final CommissionOrService commissions;
  private final PaymentFileLayouts layouts;

  /**
   * Creates the handler.
   *
   * @param commissions commission ORs
   * @param layouts layouts
   */
  public CommissionPaymentHandler(CommissionOrService commissions, PaymentFileLayouts layouts) {
    this.commissions = commissions;
    this.layouts = layouts;
  }

  @Override
  public String code() {
    return "COMMISSION_PAYMENT";
  }

  @Override
  public String title() {
    return "Commission Payment Details";
  }

  @Override
  public String permission() {
    return PaymentFileHandler.PERMISSION;
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required("Insurer code", "Insurer paying the commission", "INS-MGIC"),
        BulkColumn.required("Payee name", "Payee on the official receipt", "MGIC Insurance"),
        BulkColumn.optional("Certificate ref", "BIR 2307 certificate of the insurer", "2307-0001"),
        BulkColumn.required("Payment ref", "Check or credit reference", "CHK-0001"),
        BulkColumn.optional("Invoice no", "Invoice of the commission", "BI-2026-000001"),
        new BulkColumn(GROSS, "Basic commission", true, Type.NUMBER, "2379.13"),
        new BulkColumn(VAT, "EVAT on the commission", true, Type.NUMBER, "285.50"),
        new BulkColumn(WTAX, "Withholding tax", true, Type.NUMBER, "237.91"),
        new BulkColumn("Payment date", "Date of the payment", true, Type.DATE, "2026-09-24"));
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    BigDecimal gross = row.number(GROSS);
    if (gross != null && gross.signum() <= 0) {
      errors.add("The basic commission must be above zero");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    return commissions.stage(
        context.companyId(),
        context.jobNo(),
        row.rowNo(),
        new CommissionDetail(
            row.text("Insurer code"),
            row.text("Payee name"),
            row.text("Certificate ref"),
            row.text("Payment ref"),
            row.text("Invoice no"),
            new OrAmounts(row.number(GROSS), row.number(VAT), row.number(WTAX)),
            row.date("Payment date")));
  }

  @Override
  public TextLayout textLayout() {
    return layouts.layout(code());
  }

  @Override
  public boolean blocksDuplicateFiles() {
    return true;
  }
}
