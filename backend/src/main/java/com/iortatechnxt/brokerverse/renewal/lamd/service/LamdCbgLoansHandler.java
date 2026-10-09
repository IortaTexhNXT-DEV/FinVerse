package com.iortatechnxt.brokerverse.renewal.lamd.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_LAMD_CBG_LOANS} (FRRN.012.01, Annex G): the CBG loans of LAMD excluding
 * personal loans; the loan status is Active, RMU (Account Officer in the RMU list) or Unmatched.
 */
@Component
public class LamdCbgLoansHandler extends LamdListHandler {

  /** Handler code. */
  public static final String CODE = "RNW_LAMD_CBG_LOANS";

  private static final String PRODUCT = "Product Description";
  private static final String BRANCH = "Logical Branch Description PN";

  /**
   * Creates the handler.
   *
   * @param loans LAMD lists
   */
  public LamdCbgLoansHandler(LamdLoanService loans) {
    super(loans);
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - LAMD CBG loans excluding personal";
  }

  @Override
  String kind() {
    return LamdLoanService.CBG_LOANS;
  }

  @Override
  String productHeader() {
    return PRODUCT;
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.optional("Original Source", "Source system of the loan record", "LOANS"),
        BulkColumn.required(PN, "Promissory Note number of the loan", "7001234567"),
        BulkColumn.optional("Short name", "Short name of the borrower", "DELA CRUZ J"),
        BulkColumn.optional("Logical Branch PN", "Branch code of the PN", "123"),
        BulkColumn.optional(BRANCH, "Branch name of the PN", "MAKATI MAIN"),
        BulkColumn.optional("Product Type", "Loan product classification", "201"),
        BulkColumn.optional(PRODUCT, "Description of the loan product", "AUTO LOAN"),
        new BulkColumn(
            "Book Date", "Date the loan was booked", false, BulkColumn.Type.DATE, "2024-03-15"),
        new BulkColumn(
            "Maturity Date", "Date the loan matures", false, BulkColumn.Type.DATE, "2029-03-15"),
        BulkColumn.optional("Amount Financed (Orig)", "Amount financed at booking", "1200000"),
        BulkColumn.optional("O/S Bal. (LCYE)", "Outstanding balance at year end", "850000"),
        BulkColumn.optional("Customer No.", "Customer number of the bank", "100200300"),
        BulkColumn.optional("Park Branch", "Branch servicing the account", "123"),
        BulkColumn.required(AO_CODE, "Account Officer code of the loan", "AO1234"),
        BulkColumn.optional("AO Name PN", "Account Officer name", "Ana Officer"),
        BulkColumn.optional(TAG, "IBG or CBG", "CBG"),
        BulkColumn.optional("Home Contact No.", "Home telephone", "028881234"),
        BulkColumn.optional("Business Contact No.", "Business telephone", "028885678"),
        BulkColumn.optional("Mobile No.", "Mobile phone", "09171234567"),
        BulkColumn.optional(EMAIL, "E-mail address of the borrower", "juan@example.ph"),
        BulkColumn.optional(ADDRESS, "Mailing address of the borrower", "1 Ayala Ave, Makati"),
        BulkColumn.optional(
            "Risk & Compliance Tagging (PEP, AMLA High Risk, RPT)", "Compliance tags", ""),
        BulkColumn.optional("Source of Funds", "Declared source of funds", "SALARY"),
        BulkColumn.optional(SERIAL, "Serial (chassis) number of a motor collateral", "MHF123"),
        BulkColumn.optional(MOTOR, "Motor (engine) number of a motor collateral", "2TR456"),
        BulkColumn.optional("Collateral Desc", "Description of the collateral", "2024 SEDAN"));
  }

  @Override
  LamdLoanService.Loan loan(BulkRow row) {
    return new LamdLoanService.Loan(
        row.text(PN),
        row.text(TAG),
        row.date("Book Date"),
        row.date("Maturity Date"),
        row.text(BRANCH),
        row.text(PRODUCT),
        row.text(AO_CODE),
        row.text("AO Name PN"),
        row.text(EMAIL),
        row.text(ADDRESS),
        row.text(SERIAL),
        row.text(MOTOR),
        row.text("Short name"));
  }
}
