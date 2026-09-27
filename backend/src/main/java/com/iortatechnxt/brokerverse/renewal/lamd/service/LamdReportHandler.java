package com.iortatechnxt.brokerverse.renewal.lamd.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.LamdLine;
import com.iortatechnxt.brokerverse.renewal.domain.LamdStatus;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_LAMD_REPORT} (FR-RN-025; BRRN.029): the paid-off or RMU loan report of
 * LAMD for a month (parameters {@code reportType} and {@code period}). Each line is matched to the
 * open renewals by PN, the primary key, and the matched renewals are routed by their checks: a
 * paid-off loan tags the renewal Not for Renewal (Loan fully paid), an RMU loan tags it RMU or
 * transfers it to the unit of {@code RNW_RMU_UNIT}.
 */
@Component
public class LamdReportHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_LAMD_REPORT";

  /** Parameter: PAID_OFF or RMU. */
  public static final String PARAM_TYPE = "reportType";

  /** Parameter: month yyyy-MM. */
  public static final String PARAM_PERIOD = "period";

  private static final String PN = "PN Number";
  private static final String STATUS_DATE = "Status Date";
  private static final String BORROWER = "Borrower";
  private static final Pattern PERIOD = Pattern.compile("\\d{4}-(0[1-9]|1[0-2])");

  private final LamdService lamd;

  /**
   * Creates the handler.
   *
   * @param lamd LAMD matching
   */
  public LamdReportHandler(LamdService lamd) {
    this.lamd = lamd;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - LAMD loan report";
  }

  @Override
  public String permission() {
    return Permission.RNW_LAMD_UPLOAD.name();
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(PN, "PN number of the loan", "PN-778812"),
        new BulkColumn(
            STATUS_DATE, "Date of the loan status", false, BulkColumn.Type.DATE, "2027-05-31"),
        BulkColumn.optional(BORROWER, "Borrower name", "Juan Dela Cruz"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(PN);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (type(context) == null) {
      errors.add("Choose the report type (Paid-off or RMU)");
    }
    String period = context.parameter(PARAM_PERIOD);
    if (period == null || !PERIOD.matcher(period).matches()) {
      errors.add("Choose the period of the report");
    }
    if (row.text(PN) == null) {
      errors.add("The row has no PN number");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    LamdLine line =
        lamd.line(
            context.companyId(),
            context.jobNo(),
            new LamdService.Header(type(context), context.parameter(PARAM_PERIOD)),
            row.rowNo(),
            new LamdLine.Loan(
                row.text(PN), type(context), row.date(STATUS_DATE), row.text(BORROWER)));
    return line.getMatchOutcome() + (line.getRouting() == null ? "" : ": " + line.getRouting());
  }

  private static LamdStatus type(BulkContext context) {
    String value = context.parameter(PARAM_TYPE);
    if (value == null) {
      return null;
    }
    for (LamdStatus s : LamdStatus.values()) {
      if (s.name().equals(value.strip())) {
        return s;
      }
    }
    return null;
  }
}
