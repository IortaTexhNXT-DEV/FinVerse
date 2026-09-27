package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest.RequestFacts;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * {@code DISB_REQUESTS} (DIS 2.5.0-2.5.1, 3.28.4): an .xlsx / .ods / .csv list of payment requests;
 * each valid row becomes a request of the processing list (its payee matched to the master), the
 * failed rows form the fall-out with their reasons. The RFP number identifies a row: the same RFP
 * is refused twice. Template columns to confirm with BDOI (AQ12).
 */
@Component
public class RequestUploadHandler implements BulkImportHandler {

  /** Source module of uploaded requests. */
  public static final String SOURCE = "DISB_UPLOAD";

  private static final String RFP = "RFP no";
  private static final String TYPE = "Disbursement type";
  private static final String AMOUNT = "Amount";
  private static final String CURRENCY = "Currency";

  private final RequestIntakeService intake;
  private final LovService lovs;

  /**
   * Creates the handler.
   *
   * @param intake request intake
   * @param lovs lists of values
   */
  public RequestUploadHandler(RequestIntakeService intake, LovService lovs) {
    this.intake = intake;
    this.lovs = lovs;
  }

  @Override
  public String code() {
    return "DISB_REQUESTS";
  }

  @Override
  public String title() {
    return "Disbursement Requests";
  }

  @Override
  public String permission() {
    return "DISB_UPLOAD";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(
            RFP, "Request-for-payment number of the requesting unit", "RFP-2026-0101"),
        BulkColumn.required(
            TYPE, "LOV DISBURSEMENT_TYPE (SUPPLIER, EMPLOYEE, OTHER...)", "SUPPLIER"),
        BulkColumn.required("Payee code", "Party code of the payee", "SUP-0001"),
        BulkColumn.optional("Payee name", "Payee name (second match key)", "Acme Office Supply"),
        BulkColumn.required(CURRENCY, "ISO currency", "PHP"),
        new BulkColumn(AMOUNT, "Gross amount", true, Type.NUMBER, "12500.00"),
        BulkColumn.required("Purpose", "What is paid", "Office supplies September"),
        BulkColumn.optional("Root invoice no", "Invoice family, when any", ""),
        BulkColumn.optional("Expense account", "Expense account of an OTHER payment", ""),
        BulkColumn.optional("Cost centre", "Cost centre of the expense", ""));
  }

  @Override
  public String instructions() {
    return "One row per payment request. Rows whose payee is not in the payee master wait as"
        + " 'No payee' until it is maintained.";
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(RFP);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    String type = row.text(TYPE);
    if (type == null
        || lovs.activeValues("DISBURSEMENT_TYPE", context.businessDate()).stream()
            .noneMatch(v -> v.getCode().equals(type.strip()))) {
      errors.add("Unknown disbursement type " + type);
    }
    BigDecimal amount = row.number(AMOUNT);
    if (amount == null || amount.signum() <= 0) {
      errors.add("The amount must be positive");
    }
    String currency = row.text(CURRENCY);
    if (currency == null || !currency.matches("[A-Za-z]{3}")) {
      errors.add("The currency must be a 3-letter code");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    return intake
        .register(
            new RequestFacts(
                context.companyId(),
                RequestSource.UPLOAD,
                SOURCE,
                row.text(RFP),
                row.text(RFP),
                row.text(TYPE).toUpperCase(Locale.ROOT),
                null,
                row.text("Payee code"),
                row.text("Payee name"),
                row.text(CURRENCY).toUpperCase(Locale.ROOT),
                row.number(AMOUNT),
                row.text("Purpose"),
                row.text("Root invoice no"),
                List.of(),
                List.of(),
                false,
                null,
                context.jobNo(),
                row.text("Expense account"),
                row.text("Cost centre")))
        .getRequestNo();
  }
}
