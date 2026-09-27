package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeSource;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee.PayeeDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount.AccountDetails;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * {@code DISB_PAYEE_MIGRATION} (DIS 2.2.8, AQ11): payees migrated from the current system. Each
 * valid row is saved as a payee of source MIGRATION and submitted for authorisation, so a checker
 * authorises the migrated master; the upload report is the reconciliation of rows read, loaded and
 * failed. The file of the current system is parked (AQ11).
 */
@Component
public class PayeeMigrationHandler implements BulkImportHandler {

  private static final String CODE = "Payee code";
  private static final String MODES = "Allowed modes";
  private static final String DEFAULT_MODE = "Default mode";
  private static final String ACCOUNT_NO = "Account no";

  private final PayeeService payees;

  /**
   * Creates the handler.
   *
   * @param payees payee maintenance
   */
  public PayeeMigrationHandler(PayeeService payees) {
    this.payees = payees;
  }

  @Override
  public String code() {
    return "DISB_PAYEE_MIGRATION";
  }

  @Override
  public String title() {
    return "Payee Migration";
  }

  @Override
  public String permission() {
    return "DISB_PAYEE_MAINTAIN";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(CODE, "Party code", "SUP-0001"),
        BulkColumn.required("Payee class", "LOV PAYEE_CLASS", "SUPPLIER"),
        BulkColumn.required("Name", "Payee name", "Acme Office Supply"),
        BulkColumn.optional("Address", "Address", "Makati City"),
        BulkColumn.optional("Email", "E-mail for payment advices", "ap@acme.example"),
        BulkColumn.optional("TIN", "Tax identification number", "123-456-789-000"),
        BulkColumn.required(
            DEFAULT_MODE, "CTA, ATD, MC_DD, CREDIT_TICKET, TT, ONLINE_BANKING, CHECK", "CHECK"),
        BulkColumn.required(MODES, "Allowed modes separated by '|'", "CHECK|CTA"),
        BulkColumn.required("Currency", "ISO currency", "PHP"),
        BulkColumn.optional("Bank", "Bank of the payee account", "BDO Unibank"),
        BulkColumn.optional(ACCOUNT_NO, "Payee account number", "001234567890"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(CODE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    try {
      List<DisbursementMode> modes = modes(row.text(MODES));
      if (!modes.contains(DisbursementMode.valueOf(upper(row.text(DEFAULT_MODE))))) {
        errors.add("The default mode must be one of the allowed modes");
      }
    } catch (IllegalArgumentException ex) {
      errors.add("Unknown mode of payment: " + ex.getMessage());
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    List<DisbursementMode> modes = modes(row.text(MODES));
    String currency = upper(row.text("Currency"));
    List<AccountDetails> accounts = new ArrayList<>();
    if (row.text(ACCOUNT_NO) != null) {
      DisbursementMode accountMode =
          modes.contains(DisbursementMode.CTA) ? DisbursementMode.CTA : DisbursementMode.TT;
      accounts.add(
          new AccountDetails(
              row.text("Bank") == null ? "Unknown bank" : row.text("Bank"),
              null,
              row.text(ACCOUNT_NO),
              row.text("Name"),
              currency,
              accountMode,
              true));
    }
    Payee payee =
        payees.create(
            context.companyId(),
            row.text(CODE),
            new PayeeDetails(
                upper(row.text("Payee class")),
                row.text("Name"),
                row.text("Address"),
                row.text("Email"),
                row.text("TIN"),
                DisbursementMode.valueOf(upper(row.text(DEFAULT_MODE))),
                modes,
                List.of(),
                currency,
                null,
                "Migrated by " + context.jobNo()),
            accounts,
            PayeeSource.MIGRATION);
    payees.submit(payee.getId());
    return payee.getPayeeCode();
  }

  private static List<DisbursementMode> modes(String text) {
    return Arrays.stream(text.split("\\|"))
        .map(String::strip)
        .filter(s -> !s.isEmpty())
        .map(s -> DisbursementMode.valueOf(upper(s)))
        .toList();
  }

  private static String upper(String text) {
    return text == null ? null : text.strip().toUpperCase(Locale.ROOT);
  }
}
