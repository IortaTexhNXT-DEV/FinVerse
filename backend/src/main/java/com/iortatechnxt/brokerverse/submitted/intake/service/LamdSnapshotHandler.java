package com.iortatechnxt.brokerverse.submitted.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan.LoanFacts;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoanRepository;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Upload of a LAMD loan snapshot (BRIDSP-13; FRS FR-SP-032): per PN the loan status, the amortised
 * flag, the maturity date and the collateral numbers, for the date of the snapshot (parameter
 * {@code snapshotDate}, today when blank). The matching step of the next processing run reads the
 * latest snapshot of each PN. Loading a PN again for the same date replaces its row.
 */
@Component
public class LamdSnapshotHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "SBM_LAMD";

  /** Parameter: snapshot date (yyyy-MM-dd). */
  public static final String SNAPSHOT_DATE = "snapshotDate";

  private static final String PN = "PN No";
  private static final String STATUS = "Loan Status";

  private final SbmLamdLoanRepository loans;
  private final LovService lovs;

  /**
   * Creates the handler.
   *
   * @param loans LAMD loans
   * @param lovs lists of values (loan status)
   */
  public LamdSnapshotHandler(SbmLamdLoanRepository loans, LovService lovs) {
    this.loans = loans;
    this.lovs = lovs;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "LAMD loan snapshot";
  }

  @Override
  public String permission() {
    return "SBM_INTAKE";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(PN, "Promissory note number of the loan", "PN-2026-000123"),
        BulkColumn.optional("Borrower", "Borrower name", "Juan Dela Cruz"),
        BulkColumn.required(
            STATUS, "Active, Open Market, Fully Paid or Remedial", "Active"),
        new BulkColumn("Amortised", "Y when the loan is amortised", false, Type.YES_NO, "N"),
        new BulkColumn("Maturity Date", "Loan maturity date", false, Type.DATE, "2029-06-30"),
        BulkColumn.optional("Originating Unit", "Originating unit", "Auto Loans"),
        new BulkColumn("Balance", "Outstanding balance", false, Type.NUMBER, "450000.00"),
        BulkColumn.optional("Serial No", "Chassis or serial number of the collateral", "MHFA1234567"),
        BulkColumn.optional("Motor No", "Engine or motor number of the collateral", "2NR1234567"));
  }

  @Override
  public String sanitize(String header, String value) {
    String v = value.trim().replaceAll("\\s+", " ");
    return switch (header) {
      case PN, "Serial No", "Motor No" -> BulkImportHandler.identifier(v);
      case STATUS -> v.toUpperCase(Locale.ROOT).replace(' ', '_').replace("REMEDIAL_(RMU)", "REMEDIAL");
      default -> v;
    };
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(PN);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (row.text(PN) == null) {
      errors.add("PN is required");
    }
    String status = row.text(STATUS);
    if (status != null
        && lovs.activeValues(SubmittedCodes.LOV_LOAN_STATUS, context.businessDate()).stream()
            .noneMatch(v -> v.getCode().equals(status))) {
      errors.add("Loan Status must be Active, Open Market, Fully Paid or Remedial");
    }
    try {
      snapshotDate(context);
    } catch (DateTimeParseException e) {
      errors.add("The snapshot date must be a date (yyyy-MM-dd)");
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    LocalDate date = snapshotDate(context);
    LoanFacts facts =
        new LoanFacts(
            row.text("Borrower"),
            row.text(STATUS),
            row.yes("Amortised"),
            row.date("Maturity Date"),
            row.text("Originating Unit"),
            row.number("Balance"),
            row.text("Serial No"),
            row.text("Motor No"));
    SbmLamdLoan loan =
        loans
            .findByCompanyIdAndSnapshotDateAndPnNo(context.companyId(), date, row.text(PN))
            .map(
                l -> {
                  l.update(facts);
                  return l;
                })
            .orElseGet(
                () ->
                    loans.save(
                        new SbmLamdLoan(
                            context.companyId(), date, row.text(PN), facts, context.jobNo())));
    return loan.getPnNo();
  }

  private static LocalDate snapshotDate(BulkContext context) {
    String value = context.parameter(SNAPSHOT_DATE);
    return value == null || value.isBlank() ? context.businessDate() : LocalDate.parse(value);
  }
}
