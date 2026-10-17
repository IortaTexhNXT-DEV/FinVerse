package com.iortatechnxt.brokerverse.bulk;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.bulk.service.TextLayout;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Test handler of a fixed-width payment file: refuses duplicate files and sorts rows into APPLIED
 * (amount above 100) and UNAPPLIED; reference "RETRY" fails the first time it is committed.
 */
@Component
public class TestPaymentFileHandler implements BulkImportHandler {

  static final String CODE = "TEST_PAYMENT_FILE";

  private final Set<String> attempted = ConcurrentHashMap.newKeySet();

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Test payment file";
  }

  @Override
  public String permission() {
    return "BULK_PROCESS";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required("Reference", "Payment reference", "P1"),
        new BulkColumn("Amount", "Amount paid", true, BulkColumn.Type.NUMBER, "100.00"));
  }

  @Override
  public TextLayout textLayout() {
    return TextLayout.fixedWidth(
        List.of(
            new TextLayout.FixedField("Reference", 1, 10),
            new TextLayout.FixedField("Amount", 11, 10)));
  }

  @Override
  public boolean blocksDuplicateFiles() {
    return true;
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of("APPLIED", "UNAPPLIED");
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return List.of();
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    String reference = row.text("Reference");
    if (reference.startsWith("RETRY") && attempted.add(context.jobNo() + reference)) {
      throw new BusinessRuleException("TEST_RETRY", "Temporarily refused");
    }
    return new BulkOutcome(
        "PAY-" + reference,
        row.number("Amount").compareTo(BigDecimal.valueOf(100)) > 0 ? "APPLIED" : "UNAPPLIED");
  }
}
