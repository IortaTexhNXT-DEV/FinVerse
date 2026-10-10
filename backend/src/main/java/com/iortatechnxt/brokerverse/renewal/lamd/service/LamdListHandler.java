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
import java.util.stream.Collectors;

/**
 * The common part of BDOI's two LAMD list uploads (FRRN.012.01, Annex G): the required PN and
 * Account Officer code, and the serial and motor numbers of a motor loan; each row is saved and
 * matched immediately ({@link LamdLoanService}).
 */
abstract class LamdListHandler implements BulkImportHandler {

  static final String PN = "PN Number";
  static final String TAG = "IBG/CBG Tag";
  static final String EMAIL = "Email Address";
  static final String ADDRESS = "Mailing Address";
  static final String SERIAL = "Serial No.";
  static final String MOTOR = "Motor No.";
  static final String AO_CODE = "Officer (AO Code) PN";

  private final LamdLoanService loans;

  LamdListHandler(LamdLoanService loans) {
    this.loans = loans;
  }

  /** The kind of list: CBG_LOANS or PAID_OFF. */
  abstract String kind();

  /** The header of the product description of the list. */
  abstract String productHeader();

  /** The loan of a row. */
  abstract LamdLoanService.Loan loan(BulkRow row);

  /**
   * The columns that close every LAMD list: the e-mail and mailing address of the borrower, the
   * compliance tags, the source of funds and the motor collateral.
   *
   * @param own the columns of the list before them
   * @return the columns of the list
   */
  static List<BulkColumn> withBorrowerColumns(BulkColumn... own) {
    List<BulkColumn> all = new ArrayList<>(List.of(own));
    all.add(BulkColumn.optional(EMAIL, "E-mail address of the borrower", "juan@example.ph"));
    all.add(BulkColumn.optional(ADDRESS, "Mailing address of the borrower", "1 Ayala Ave, Makati"));
    all.add(
        BulkColumn.optional(
            "Risk & Compliance Tagging (PEP, AMLA High Risk, RPT)", "Compliance tags", ""));
    all.add(BulkColumn.optional("Source of Funds", "Declared source of funds", "SALARY"));
    all.add(BulkColumn.optional(SERIAL, "Serial (chassis) number of a motor collateral", "MHF123"));
    all.add(BulkColumn.optional(MOTOR, "Motor (engine) number of a motor collateral", "2TR456"));
    all.add(BulkColumn.optional("Collateral Desc", "Description of the collateral", "2024 SEDAN"));
    return List.copyOf(all);
  }

  /** Whether the Account Officer code is required in the list. */
  boolean aoRequired() {
    return true;
  }

  @Override
  public String permission() {
    return Permission.RNW_LAMD_UPLOAD.name();
  }

  @Override
  public String filledBy() {
    return "LAMD, the loan report of the bank as it is extracted";
  }

  @Override
  public String uploadPath() {
    return "Renewal > Renewal Home, button LAMD Report Upload";
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of(LamdLoanService.MATCHED, LamdLoanService.UNMATCHED);
  }

  @Override
  public Set<String> optionalHeaders() {
    return columns().stream()
        .filter(c -> !c.required())
        .map(BulkColumn::header)
        .collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public String duplicateKey(BulkRow row) {
    String pn = row.text(PN);
    return pn == null ? null : pn + "|" + row.text(SERIAL) + "|" + row.text(MOTOR);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (row.text(PN) == null) {
      errors.add("The PN Number is missing");
    }
    if (aoRequired() && row.text(AO_CODE) == null) {
      errors.add("The Officer (AO Code) PN is missing");
    }
    if (loans.motor(row.text(productHeader()))
        && (row.text(SERIAL) == null || row.text(MOTOR) == null)) {
      errors.add("The Serial No. and Motor No. of a motor loan are required");
    }
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    return loans.record(context.companyId(), context.jobNo(), row.rowNo(), kind(), loan(row));
  }
}
