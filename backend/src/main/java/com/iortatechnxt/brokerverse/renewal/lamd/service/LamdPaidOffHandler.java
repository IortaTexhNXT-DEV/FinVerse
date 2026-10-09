package com.iortatechnxt.brokerverse.renewal.lamd.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_LAMD_PAID_OFF} (FRRN.012.01, FRRN.012.05, Annex G): the list of paid-off
 * accounts of LAMD; each loan is Loan Fully Paid, routes its renewal Not for Renewal and is kept in
 * the paid-off repository once.
 */
@Component
public class LamdPaidOffHandler extends LamdListHandler {

  /** Handler code. */
  public static final String CODE = "RNW_LAMD_PAID_OFF";

  private static final String PRODUCT = "Product Type Desc";

  /**
   * Creates the handler.
   *
   * @param loans LAMD lists
   */
  public LamdPaidOffHandler(LamdLoanService loans) {
    super(loans);
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - LAMD list of paid-off accounts";
  }

  @Override
  String kind() {
    return LamdLoanService.PAID_OFF;
  }

  @Override
  String productHeader() {
    return PRODUCT;
  }

  @Override
  boolean aoRequired() {
    return false;
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(PN, "Promissory Note number of the loan", "7001234567"),
        BulkColumn.optional(TAG, "IBG or CBG", "CBG"),
        BulkColumn.optional("Product Type", "Loan product classification", "201"),
        BulkColumn.optional(PRODUCT, "Description of the loan product", "AUTO LOAN"),
        new BulkColumn(
            "Posting Date",
            "Date the payment was posted",
            false,
            BulkColumn.Type.DATE,
            "2027-05-31"),
        new BulkColumn(
            "Next Rate Review Date", "Next rate review", false, BulkColumn.Type.DATE, ""),
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
        null,
        null,
        null,
        row.text(PRODUCT),
        null,
        null,
        row.text(EMAIL),
        row.text(ADDRESS),
        row.text(SERIAL),
        row.text(MOTOR),
        null);
  }
}
