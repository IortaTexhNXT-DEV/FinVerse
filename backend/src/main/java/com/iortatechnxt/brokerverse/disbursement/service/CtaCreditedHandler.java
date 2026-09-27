package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@code DISB_CTA_CREDITED} (DIS 2.22.0, 3.26.4): the credited-accounts file returned by TPD after
 * ACA processing of the DCTF; each row tags its credit to account CREDITED. The TPD layout is
 * parked (AQ09): the file carries the DCTF reference (DV number) and the amount.
 */
@Component
public class CtaCreditedHandler implements BulkImportHandler {

  private static final String REFERENCE = "Reference";
  private static final String AMOUNT = "Amount";

  private final InstrumentUploads uploads;

  /**
   * Creates the handler.
   *
   * @param uploads instrument uploads
   */
  public CtaCreditedHandler(InstrumentUploads uploads) {
    this.uploads = uploads;
  }

  @Override
  public String code() {
    return "DISB_CTA_CREDITED";
  }

  @Override
  public String title() {
    return "Credited Accounts (Credit to Account)";
  }

  @Override
  public String permission() {
    return "DISB_UPLOAD";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(REFERENCE, "DCTF reference (DV number)", "DV-2026-000001"),
        new BulkColumn(AMOUNT, "Amount credited", true, Type.NUMBER, "12500.00"),
        BulkColumn.optional("Account no", "Payee account credited", "001234567890"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(REFERENCE);
  }

  @Override
  public boolean blocksDuplicateFiles() {
    return true;
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return List.of();
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    uploads.credited(row.text(REFERENCE), row.number(AMOUNT), context.jobNo());
    return row.text(REFERENCE);
  }
}
