package com.iortatechnxt.brokerverse.submitted.intake.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan.LoanFacts;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan.LoanKey;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoanRepository;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Upload of a loan file of the bank (BRIDSP-02/13; FRS FR-SP-032, FR-SP-035): the loan report
 * (parameter {@code loanReport}: LAMD, LMS or LAD of the list SBM_LOAN_REPORT, LAMD when blank)
 * and, per loan identified by its PN number or its loan application number, the loan status, the
 * amortised flag, the maturity date, the branch, the segment and the collateral numbers, for the
 * date of the file (parameter {@code snapshotDate}, today when blank). The matching step of the
 * next processing run reads the newest file of each loan. Loading a loan again for the same report
 * and date replaces its row.
 */
@Component
public class LamdSnapshotHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "SBM_LAMD";

  /** Parameter: snapshot date (yyyy-MM-dd). */
  public static final String SNAPSHOT_DATE = "snapshotDate";

  /** Parameter: loan report (list SBM_LOAN_REPORT). */
  public static final String LOAN_REPORT = "loanReport";

  private static final String PN = "PN No";
  private static final String APPLICATION = "Loan Application No";
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
    return "Loan file (LAMD, LMS or LAD)";
  }

  @Override
  public String permission() {
    return "SBM_INTAKE";
  }

  @Override
  public String filledBy() {
    return "The Submitted Policies team, from the LAMD, LMS or LAD loan report of the bank";
  }

  @Override
  public String uploadPath() {
    return "Submitted Policies > Upload & Intake, button Upload Loan File";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional(
            PN,
            "Promissory note number of the loan (or the loan application number)",
            "PN-2026-000123"),
        BulkColumn.optional(
            APPLICATION, "Loan application number (or the PN number)", "LA-2026-004512"),
        BulkColumn.optional("Borrower", "Borrower name", "Juan Dela Cruz"),
        BulkColumn.required(STATUS, "Status of the loan", "Active")
            .values("Active", "Open Market", "Fully Paid", "Remedial"),
        new BulkColumn("Amortised", "Y when the loan is amortised", false, Type.YES_NO, "N"),
        new BulkColumn("Maturity Date", "Loan maturity date", false, Type.DATE, "2029-06-30"),
        BulkColumn.optional("Originating Unit", "Originating unit", "Auto Loans"),
        BulkColumn.optional("Branch", "Branch of the loan", "Makati Main"),
        BulkColumn.optional("Segment", "Segment of the loan", "CBG"),
        new BulkColumn("Balance", "Outstanding balance", false, Type.NUMBER, "450000.00"),
        BulkColumn.optional(
            "Serial No", "Chassis or serial number of the collateral", "MHFA1234567"),
        BulkColumn.optional("Motor No", "Engine or motor number of the collateral", "2NR1234567"));
  }

  @Override
  public String sanitize(String header, String value) {
    String v = value.trim().replaceAll("\\s+", " ");
    return switch (header) {
      case PN, APPLICATION, "Serial No", "Motor No" -> BulkImportHandler.identifier(v);
      case STATUS ->
          v.toUpperCase(Locale.ROOT).replace(' ', '_').replace("REMEDIAL_(RMU)", "REMEDIAL");
      default -> v;
    };
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(PN) + "|" + row.text(APPLICATION);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (row.text(PN) == null && row.text(APPLICATION) == null) {
      errors.add("PN number or loan application number is required");
    }
    String report = loanReport(context);
    if (lovs.activeValues(SubmittedCodes.LOV_LOAN_REPORT, context.businessDate()).stream()
        .noneMatch(v -> v.getCode().equals(report))) {
      errors.add("The loan report must be LAMD, LMS or LAD");
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
    LoanKey key = new LoanKey(loanReport(context), date, row.text(PN), row.text(APPLICATION));
    SbmLamdLoan loan =
        existing(context.companyId(), key)
            .map(
                l -> {
                  l.update(facts);
                  return l;
                })
            .orElseGet(
                () ->
                    loans.save(new SbmLamdLoan(context.companyId(), key, facts, context.jobNo())));
    loan.placeOf(row.text("Branch"), row.text("Segment"));
    return loan.getPnNo() == null ? loan.getLoanApplicationNo() : loan.getPnNo();
  }

  private Optional<SbmLamdLoan> existing(Long companyId, LoanKey key) {
    List<SbmLamdLoan> rows =
        key.pnNo() == null
            ? loans.findByCompanyIdAndLoanReportAndSnapshotDateAndLoanApplicationNo(
                companyId, key.loanReport(), key.snapshotDate(), key.loanApplicationNo())
            : loans.findByCompanyIdAndLoanReportAndSnapshotDateAndPnNo(
                companyId, key.loanReport(), key.snapshotDate(), key.pnNo());
    return rows.stream()
        .filter(l -> Objects.equals(l.getPnNo(), key.pnNo()))
        .filter(l -> Objects.equals(l.getLoanApplicationNo(), key.loanApplicationNo()))
        .findFirst();
  }

  private static String loanReport(BulkContext context) {
    String value = context.parameter(LOAN_REPORT);
    return value == null || value.isBlank()
        ? SbmLamdLoan.DEFAULT_REPORT
        : value.strip().toUpperCase(Locale.ROOT);
  }

  private static LocalDate snapshotDate(BulkContext context) {
    String value = context.parameter(SNAPSHOT_DATE);
    return value == null || value.isBlank() ? context.businessDate() : LocalDate.parse(value);
  }
}
