package com.iortatechnxt.brokerverse.renewal.lamd.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_BDOFC_SOLD} (FRRN.012.06): a BDOFC or BDOSOLD report (parameter {@code
 * reportType}), each line matched on the PN.
 */
@Component
public class BdofcSoldHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_BDOFC_SOLD";

  /** Parameter: BDOFC or BDOSOLD. */
  public static final String PARAM_TYPE = "reportType";

  private static final String LOCATION = "Location";
  private static final String PN = "PN";
  private static final String ASSURED = "Assured's Name";
  private static final String INCEPTION = "Inception Date";
  private static final String EXPIRY = "Expiry Date";
  private static final String REMARKS = "Remarks";
  private static final Set<String> TYPES = Set.of("BDOFC", "BDOSOLD");

  private final BdofcSoldService service;

  /**
   * Creates the handler.
   *
   * @param service matching
   */
  public BdofcSoldHandler(BdofcSoldService service) {
    this.service = service;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - BDOFC and BDOSOLD report";
  }

  @Override
  public String permission() {
    return Permission.RNW_LAMD_UPLOAD.name();
  }

  @Override
  public String filledBy() {
    return "The renewal team, from the BDOFC or BDOSOLD report of the bank";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Home, button BDOFC/SOLD Report Upload";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional(LOCATION, "Location of the account", "MAKATI"),
        BulkColumn.required(PN, "Promissory Note number", "7001234567"),
        BulkColumn.required(ASSURED, "Name of the assured", "Juan Dela Cruz"),
        new BulkColumn(INCEPTION, "Inception date", false, BulkColumn.Type.DATE, "2026-11-01"),
        new BulkColumn(EXPIRY, "Expiry date", false, BulkColumn.Type.DATE, "2027-11-01"),
        BulkColumn.required(REMARKS, "Remarks of the report", "Unit sold"));
  }

  @Override
  public Set<String> optionalHeaders() {
    return Set.of(LOCATION, INCEPTION, EXPIRY);
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of("MATCHED", "UNMATCHED");
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    String type = context.parameter(PARAM_TYPE);
    if (type == null || !TYPES.contains(type.strip())) {
      errors.add("Choose the report type (BDOFC or BDOSOLD)");
    }
    for (String h : List.of(PN, ASSURED, REMARKS)) {
      if (row.text(h) == null) {
        errors.add("The " + h + " is missing");
      }
    }
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    return service.record(
        context.companyId(),
        context.jobNo(),
        row.rowNo(),
        context.parameter(PARAM_TYPE).strip(),
        new BdofcSoldService.Line(
            row.text(LOCATION),
            row.text(PN),
            row.text(ASSURED),
            row.date(INCEPTION),
            row.date(EXPIRY),
            row.text(REMARKS)));
  }
}
