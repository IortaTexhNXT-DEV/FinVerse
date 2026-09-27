package com.iortatechnxt.brokerverse.submitted.fee.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNoTouchLine;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Upload of the No Touch list returned by an insurer (Report List #164; parameter {@code batchNo}):
 * per masterlist number the basic premium, gross service fee, VAT and withholding tax validated by
 * the insurer. Once committed the batch totals and the billing statement are produced.
 */
@Component
public class NoTouchReturnHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "SBM_NO_TOUCH_RETURN";

  /** Parameter: batch number. */
  public static final String BATCH = "batchNo";

  private static final String SBM_NO = "Masterlist No";

  private final NoTouchService noTouch;

  /**
   * Creates the handler.
   *
   * @param noTouch No Touch billing
   */
  public NoTouchReturnHandler(NoTouchService noTouch) {
    this.noTouch = noTouch;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "No Touch list returned by the insurer";
  }

  @Override
  public String permission() {
    return "SBM_HANDLING_FEE";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(SBM_NO, "Masterlist number of the exported line", "SBM-2026-000012"),
        new BulkColumn(
            "Basic Premium",
            "Basic premium validated by the insurer",
            true,
            Type.NUMBER,
            "18500.00"),
        new BulkColumn("Gross Service Fee", "Gross service fee", true, Type.NUMBER, "925.00"),
        new BulkColumn("VAT", "VAT on the service fee", true, Type.NUMBER, "111.00"),
        new BulkColumn(
            "Withholding Tax", "Withholding tax on the service fee", true, Type.NUMBER, "138.75"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(SBM_NO);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    String batchNo = context.parameter(BATCH);
    if (batchNo == null || batchNo.isBlank()) {
      errors.add("Choose the No Touch batch of the return");
      return errors;
    }
    try {
      boolean known =
          noTouch.lines(noTouch.batch(batchNo).getId()).stream()
              .anyMatch(l -> l.getSbmNo().equals(row.text(SBM_NO)));
      if (!known) {
        errors.add(row.text(SBM_NO) + " is not in batch " + batchNo);
      }
    } catch (ResourceNotFoundException e) {
      errors.add("Batch " + batchNo + " is unknown");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    return noTouch
        .returnLine(
            context.parameter(BATCH),
            row.text(SBM_NO),
            new SbmNoTouchLine.Values(
                row.number("Basic Premium"),
                row.number("Gross Service Fee"),
                row.number("VAT"),
                row.number("Withholding Tax")))
        .getSbmNo();
  }

  @Override
  public void afterCommit(BulkContext context, int committed, int failed) {
    if (committed > 0) {
      noTouch.closeReturn(context.parameter(BATCH));
    }
  }
}
