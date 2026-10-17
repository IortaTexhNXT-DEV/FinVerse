package com.iortatechnxt.brokerverse.bulk;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Test handler: plates and amounts; plate "FAIL" fails at commit, amount above 1M is invalid. */
@Component
public class TestBulkHandler implements BulkImportHandler {

  static final String CODE = "TEST_VEHICLES";
  private final List<String> committed = new CopyOnWriteArrayList<>();

  List<String> committed() {
    return committed;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Test vehicles";
  }

  @Override
  public String permission() {
    return "BULK_PROCESS";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required("Plate No", "Plate number", "ABC 1234"),
        new BulkColumn("Amount", "Sum insured", true, BulkColumn.Type.NUMBER, "850000.00"),
        new BulkColumn("Inception", "Inception date", false, BulkColumn.Type.DATE, "2026-10-01"),
        new BulkColumn("Fleet", "Part of a fleet", false, BulkColumn.Type.YES_NO, "N"),
        BulkColumn.optional("Remarks", "Free text", ""));
  }

  @Override
  public String sanitize(String header, String value) {
    String clean = BulkImportHandler.super.sanitize(header, value);
    return "Plate No".equals(header) ? BulkImportHandler.identifier(clean) : clean;
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text("Plate No");
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (row.number("Amount").compareTo(new java.math.BigDecimal("1000000")) > 0) {
      errors.add("Amount above the test limit");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    if ("FAIL".equals(row.text("Plate No"))) {
      throw new BusinessRuleException("TEST_FAIL", "Insurer rejected the vehicle");
    }
    committed.add(
        row.text("Plate No") + "@" + context.parameter("product") + (row.yes("Fleet") ? "/F" : ""));
    return "REF-" + row.text("Plate No");
  }
}
