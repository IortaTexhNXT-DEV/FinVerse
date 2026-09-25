package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@code DISB_CHECKS_NEGOTIATED} (DIS 2.22.0, 3.26.1): the bank's deposited-checks file (.txt,
 * .csv, .xlsx); each row tags its check NEGOTIATED and clears the checks outstanding. The bank
 * layout is parked (AQ09): the file carries the check number, amount and deposit date.
 */
@Component
public class ChecksNegotiatedHandler implements BulkImportHandler {

  private static final String CHECK = "Check no";
  private static final String AMOUNT = "Amount";
  private static final String DATE = "Date deposited";

  private final InstrumentUploads uploads;

  /**
   * Creates the handler.
   *
   * @param uploads instrument uploads
   */
  public ChecksNegotiatedHandler(InstrumentUploads uploads) {
    this.uploads = uploads;
  }

  @Override
  public String code() {
    return "DISB_CHECKS_NEGOTIATED";
  }

  @Override
  public String title() {
    return "Deposited Checks (Negotiated)";
  }

  @Override
  public String permission() {
    return "DISB_UPLOAD";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(CHECK, "Check number as printed", "100001"),
        new BulkColumn(AMOUNT, "Amount deposited", true, Type.NUMBER, "12500.00"),
        new BulkColumn(DATE, "Date the check was deposited", false, Type.DATE, "2026-09-25"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CHECK);
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
    LocalDate deposited = row.date(DATE);
    return uploads
        .negotiated(
            row.text(CHECK),
            row.number(AMOUNT),
            deposited == null ? "Deposited" : "Deposited " + deposited,
            context.jobNo())
        .label();
  }
}
