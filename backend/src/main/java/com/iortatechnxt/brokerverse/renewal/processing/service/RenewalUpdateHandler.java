package com.iortatechnxt.brokerverse.renewal.processing.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_RENEWAL_UPDATE} (FRRN.015.03, FRRN.015.04): the Renewal Update file of the
 * Renewal landing page, matched on the reference number; each record updates the disposition,
 * reason, new invoice number and remarks of its renewal account and moves it on, as the
 * dispositioned file does, immediately on upload.
 */
@Component
public class RenewalUpdateHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_RENEWAL_UPDATE";

  private static final Map<String, String> HEADERS =
      Map.of(
          "Reference Number", DispositionUploadHandler.REFERENCE,
          "Disposition", "Disposition",
          "Reason for Non-Renewal", "Reason",
          "New Invoice Number", "New Invoice No",
          "Remarks", "Remarks");

  private final DispositionUploadHandler dispositions;

  /**
   * Creates the handler.
   *
   * @param dispositions the dispositioned file it applies
   */
  public RenewalUpdateHandler(DispositionUploadHandler dispositions) {
    this.dispositions = dispositions;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - Renewal Update";
  }

  @Override
  public String permission() {
    return Permission.RNW_UPLOAD.name();
  }

  @Override
  public String filledBy() {
    return "Marketing, from the Renewal Update template of the Renewal landing page";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Home, button Renewal Update Upload";
  }

  @Override
  public List<BulkColumn> columns() {
    Map<String, String> back = new HashMap<>();
    HEADERS.forEach((ours, theirs) -> back.put(theirs, ours));
    return dispositions.columns().stream()
        .map(
            c ->
                new BulkColumn(
                    back.getOrDefault(c.header(), c.header()),
                    c.description(),
                    c.required(),
                    c.type(),
                    c.example(),
                    c.condition(),
                    c.choices(),
                    c.lov(),
                    c.allowed(),
                    c.format()))
        .toList();
  }

  @Override
  public Set<String> optionalHeaders() {
    return Set.of("Reason for Non-Renewal", "New Invoice Number", "Remarks");
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of("MATCHED");
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text("Reference Number");
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return dispositions.validate(mapped(row), context);
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    return new BulkOutcome(dispositions.commit(mapped(row), context), "MATCHED");
  }

  private static BulkRow mapped(BulkRow row) {
    Map<String, String> values = new LinkedHashMap<>();
    row.values().forEach((k, v) -> values.put(HEADERS.getOrDefault(k, k), v));
    return new BulkRow(row.rowNo(), values);
  }
}
