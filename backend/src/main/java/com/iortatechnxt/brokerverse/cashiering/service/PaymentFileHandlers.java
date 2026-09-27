package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake.Money;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake.Payor;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake.Tender;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentFileHandler.Spec;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The four payment file handlers of CSHID.008 with the BRD field lists (section 6.3 of the
 * requirements baseline): Bills Payment (IT-DCO FS01), Trade (CIB), CLPC and Direct Credit
 * (FS01/04). Record layouts, encryption and control totals are parked (OQ03/OQ04); the layout of
 * each TXT file is set in the layout table.
 */
@Configuration(proxyBeanMethods = false)
public class PaymentFileHandlers {

  private static final String SAMPLE_AMOUNT = "17027.86";
  private static final String SAMPLE_DATE = "2026-09-24";

  private static final String PHP = "PHP";
  private static final String AMOUNT = "Amount";
  private static final String ASSURED = "Assured";
  private static final String TX_DATE = "Transaction date";
  private static final String PN = "PN number";
  private static final String NOTE =
      "Every row is a payment: an AR is issued and the payment is matched at once. "
          + "The same file cannot be uploaded twice.";

  /**
   * Bills Payment (IT-DCO FS01).
   *
   * @param intake payment intake
   * @param layouts layouts
   * @param settings settings
   * @return handler
   */
  @Bean
  PaymentFileHandler billsPaymentHandler(
      PaymentIntakeService intake, PaymentFileLayouts layouts, CashieringSettings settings) {
    return new PaymentFileHandler(
        new Spec(
            "PAY_BILLS",
            "Bills Payment File",
            PaymentChannel.BILLS_PAYMENT,
            "BILLS_PAYMENT",
            List.of(
                BulkColumn.required("Incremental #", "Line number of the file", "1"),
                BulkColumn.required(
                    "Invoice #", "Invoice, ARN, policy or PN number", "BI-2026-000001"),
                BulkColumn.required("Assured's name", "Assured / payor", "Maria Clara Santos"),
                number(AMOUNT, "Amount paid", SAMPLE_AMOUNT),
                new BulkColumn("Late deposit (Y/N)", "Late deposit", false, Type.YES_NO, "N"),
                BulkColumn.optional("Phone #", "Phone number", "09171234567"),
                BulkColumn.optional("Branch code", "Paying branch", "001"),
                date("Date of payment", SAMPLE_DATE),
                BulkColumn.optional("Time of payment", "Time of payment", "10:15")),
            NOTE,
            PaymentFileHandlers::bills),
        intake,
        layouts,
        settings);
  }

  /**
   * Trade payment (CIB).
   *
   * @param intake payment intake
   * @param layouts layouts
   * @param settings settings
   * @return handler
   */
  @Bean
  PaymentFileHandler tradePaymentHandler(
      PaymentIntakeService intake, PaymentFileLayouts layouts, CashieringSettings settings) {
    return new PaymentFileHandler(
        new Spec(
            "PAY_TRADE",
            "Trade Payment File",
            PaymentChannel.TRADE,
            "TRADE",
            List.of(
                date(TX_DATE, SAMPLE_DATE),
                BulkColumn.optional("Branch name", "Branch", "Makati"),
                BulkColumn.required(
                    "Transaction description",
                    "Description with the invoice, ARN, policy or PN",
                    "PAYMENT BI-2026-000001"),
                new BulkColumn("Debit", "Debit", false, Type.NUMBER, "0"),
                number("Credit", "Amount credited", SAMPLE_AMOUNT),
                new BulkColumn("Running balance", "Running balance", false, Type.NUMBER, "100000"),
                BulkColumn.optional("Check no.", "Check number", "0012345")),
            NOTE + " Rows without a credit are ignored as invalid.",
            PaymentFileHandlers::trade),
        intake,
        layouts,
        settings);
  }

  /**
   * CLPC payment.
   *
   * @param intake payment intake
   * @param layouts layouts
   * @param settings settings
   * @return handler
   */
  @Bean
  PaymentFileHandler clpcPaymentHandler(
      PaymentIntakeService intake, PaymentFileLayouts layouts, CashieringSettings settings) {
    return new PaymentFileHandler(
        new Spec(
            "PAY_CLPC",
            "CLPC Payment File",
            PaymentChannel.CLPC,
            "CLPC",
            List.of(
                BulkColumn.required("Count", "Line count", "1"),
                date("Date credit", SAMPLE_DATE),
                number(AMOUNT, "Amount credited", SAMPLE_AMOUNT),
                BulkColumn.optional("AR number", "AR number of the bank", ""),
                BulkColumn.optional("Invoice", "Invoice or ARN", "BI-2026-000001"),
                BulkColumn.required(ASSURED, "Assured", "Maria Clara Santos"),
                BulkColumn.optional(PN, "Promissory note number", "PN-0001"),
                BulkColumn.optional("Corp. dept", "Corporate department", ""),
                BulkColumn.optional("Risk code", "Risk code", "MC"),
                BulkColumn.optional("Remarks", "Remarks", "")),
            NOTE,
            PaymentFileHandlers::clpc),
        intake,
        layouts,
        settings);
  }

  /**
   * Direct Credit (FS01/04).
   *
   * @param intake payment intake
   * @param layouts layouts
   * @param settings settings
   * @return handler
   */
  @Bean
  PaymentFileHandler directCreditHandler(
      PaymentIntakeService intake, PaymentFileLayouts layouts, CashieringSettings settings) {
    return new PaymentFileHandler(
        new Spec(
            "PAY_DIRECT_CREDIT",
            "Direct Credit File",
            PaymentChannel.DIRECT_CREDIT,
            "DIRECT_CREDIT",
            List.of(
                date(TX_DATE, SAMPLE_DATE),
                BulkColumn.optional("BP filename", "Bills payment file name", ""),
                BulkColumn.required("Transaction no", "Transaction number", "DC-0001"),
                number("Paid amount", "Amount paid", SAMPLE_AMOUNT),
                BulkColumn.optional("Payment type", "Payment type", "ONLINE"),
                BulkColumn.required("Payor", "Payor", "Maria Clara Santos"),
                BulkColumn.optional(
                    "Account ref no", "Invoice, ARN, policy or PN number", "BI-2026-000001"),
                BulkColumn.optional(ASSURED, ASSURED, ""),
                BulkColumn.optional("EBIX_RefNo", "EBIX reference", ""),
                BulkColumn.optional("Logged by", "Logged by", ""),
                BulkColumn.optional("Requestor", "Requestor", "")),
            NOTE,
            PaymentFileHandlers::directCredit),
        intake,
        layouts,
        settings);
  }

  private static BulkColumn number(String header, String description, String example) {
    return new BulkColumn(header, description, true, Type.NUMBER, example);
  }

  private static BulkColumn date(String header, String example) {
    return new BulkColumn(header, header, true, Type.DATE, example);
  }

  static PaymentIntake bills(BulkRow r, String job) {
    return new PaymentIntake(
        PaymentChannel.BILLS_PAYMENT,
        job,
        "BILLS:" + r.text("Date of payment") + ":" + r.text("Incremental #"),
        r.rowNo(),
        r.text("Invoice #"),
        List.of(),
        new Payor(null, r.text("Assured's name")),
        r.text("Assured's name"),
        new Money(r.number(AMOUNT), PHP, r.date("Date of payment")),
        new Tender(
            PaymentMode.BILLS_PAYMENT,
            null,
            null,
            r.text("Time of payment"),
            r.yes("Late deposit (Y/N)")));
  }

  static PaymentIntake trade(BulkRow r, String job) {
    String description = r.text("Transaction description");
    List<String> tokens =
        description == null ? List.of() : Arrays.asList(description.split("[\\s,;/]+"));
    return new PaymentIntake(
        PaymentChannel.TRADE,
        job,
        "TRADE:"
            + r.text(TX_DATE)
            + ":"
            + description
            + ":"
            + r.text("Credit")
            + ":"
            + r.text("Check no."),
        r.rowNo(),
        null,
        tokens,
        new Payor(null, description == null ? "Trade payment" : description),
        null,
        new Money(r.number("Credit"), PHP, r.date(TX_DATE)),
        new Tender(PaymentMode.TRADE, r.text("Check no."), null, null, false));
  }

  static PaymentIntake clpc(BulkRow r, String job) {
    return new PaymentIntake(
        PaymentChannel.CLPC,
        job,
        "CLPC:"
            + r.text("Date credit")
            + ":"
            + r.text("Count")
            + ":"
            + r.text("Invoice")
            + ":"
            + r.text(PN),
        r.rowNo(),
        r.text("Invoice"),
        r.text(PN) == null ? List.of() : List.of(r.text(PN)),
        new Payor(null, r.text(ASSURED)),
        r.text(ASSURED),
        new Money(r.number(AMOUNT), PHP, r.date("Date credit")),
        Tender.of(PaymentMode.CLPC));
  }

  static PaymentIntake directCredit(BulkRow r, String job) {
    return new PaymentIntake(
        PaymentChannel.DIRECT_CREDIT,
        job,
        "DC:" + r.text("Transaction no"),
        r.rowNo(),
        r.text("Account ref no"),
        r.text("EBIX_RefNo") == null ? List.of() : List.of(r.text("EBIX_RefNo")),
        new Payor(null, r.text("Payor")),
        r.text(ASSURED),
        new Money(r.number("Paid amount"), PHP, r.date(TX_DATE)),
        Tender.of(PaymentMode.DIRECT_CREDIT));
  }
}
