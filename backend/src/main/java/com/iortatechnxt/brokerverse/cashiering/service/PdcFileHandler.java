package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.bulk.service.TextLayout;
import com.iortatechnxt.brokerverse.cashiering.domain.PdcItem.PdcCheck;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@code PAY_PDC} (CSHID.008 item 4): the daily list of post-dated checks (PDC from PMS; layout not
 * given in the BRD, OQ03) goes into the PDC warehouse with a {@code PDCW-} number per check. The
 * same file cannot be uploaded twice and a check already warehoused is refused.
 */
@Component
public class PdcFileHandler implements BulkImportHandler {

  private static final String CHECK_NO = "Check number";
  private static final String BANK = "Bank code";
  private static final String MATURITY = "Maturity date";
  private static final String AMOUNT = "Amount";

  private final PdcWarehouseService warehouse;
  private final PaymentFileLayouts layouts;
  private final CashieringSettings settings;

  /**
   * Creates the handler.
   *
   * @param warehouse PDC warehouse
   * @param layouts layouts
   * @param settings settings
   */
  public PdcFileHandler(
      PdcWarehouseService warehouse, PaymentFileLayouts layouts, CashieringSettings settings) {
    this.warehouse = warehouse;
    this.layouts = layouts;
    this.settings = settings;
  }

  @Override
  public String code() {
    return "PAY_PDC";
  }

  @Override
  public String title() {
    return "Post-dated Checks";
  }

  @Override
  public String permission() {
    return PaymentFileHandler.PERMISSION;
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional("Client code", "Client", "CL-2026-000001"),
        BulkColumn.required("Payor", "Payor", "Maria Clara Santos"),
        BulkColumn.required("Reference", "Invoice, ARN, policy or PN number", "ARN-2026-940001"),
        BulkColumn.required(CHECK_NO, CHECK_NO, "0012345"),
        BulkColumn.required(BANK, "Drawee bank", "BDO"),
        BulkColumn.optional("Check branch", "Bank branch", "Makati"),
        new BulkColumn(MATURITY, "Maturity (check) date", true, Type.DATE, "2026-10-15"),
        new BulkColumn(AMOUNT, "Check amount", true, Type.NUMBER, "5000.00"),
        BulkColumn.optional("Market segment", "Market segment", "CBG"));
  }

  @Override
  public String instructions() {
    return "Each row is a post-dated check stored in the warehouse until its maturity date.";
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(BANK) + "|" + row.text(CHECK_NO);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (row.number(AMOUNT) != null && row.number(AMOUNT).signum() <= 0) {
      errors.add("The amount must be above zero");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    return warehouse
        .warehouse(
            context.companyId(),
            settings.headOffice(context.companyId()).getId(),
            new PdcCheck(
                context.jobNo(),
                row.text("Client code"),
                row.text("Payor"),
                row.text("Reference"),
                row.text(CHECK_NO),
                row.text(BANK),
                row.text("Check branch"),
                row.date(MATURITY),
                row.number(AMOUNT),
                "PHP",
                row.text("Market segment")))
        .getWarehouseNo();
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
