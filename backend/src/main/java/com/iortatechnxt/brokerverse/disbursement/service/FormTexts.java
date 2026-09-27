package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherLine;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The texts printed on the Disbursement documents: amounts, dates, accounts and entry rows. */
final class FormTexts {

  /** Printed for a missing value. */
  static final String DASH = "-";

  private FormTexts() {}

  static List<Field> voucherFields(Voucher v, DisbursementForms.FormFacts f) {
    return List.of(
        new Field("Request", f.requestNo()),
        new Field("Payee", v.getPayeeCode() + " - " + v.getPayeeName()),
        new Field("Disbursement type", v.getDisbursementType()),
        new Field("Mode of payment", v.getMode() == null ? DASH : v.getMode().name()),
        new Field("Paying account", bankText(f.bank())),
        new Field("Value date", text(v.getValueDate())),
        new Field("Gross", v.getCurrency() + " " + amount(v.getGross())),
        new Field("Withholding tax", amount(v.getEwt())),
        new Field("Net amount", v.getCurrency() + " " + amount(v.getNet())),
        new Field("Root invoice", orDash(v.getRootInvoiceNo())),
        new Field("Purpose", orDash(v.getPurpose())));
  }

  static List<String> lineRow(VoucherLine l) {
    boolean debit = l.getSide() == BalanceSide.DEBIT;
    return List.of(
        l.getAccountCode(),
        orEmpty(l.getPartyCode()),
        orEmpty(l.getCostCenter()),
        debit ? amount(l.getAmount()) : "",
        debit ? "" : amount(l.getAmount()),
        l.getOrigin().name());
  }

  static Map<String, Object> accountValues(DisbursementForms.FormFacts f) {
    BankAccount bank = f.bank();
    PayeeAccount payee = f.payeeAccount();
    return Map.of(
        "bankName", bank == null ? "" : bank.getBankName(),
        "bankAccountNo", bank == null ? "" : bank.getAccountNo(),
        "payeeAccountNo", payee == null ? "" : payee.getAccountNo(),
        "payeeBank", payee == null ? "" : payee.getBankName());
  }

  static String orEmpty(String value) {
    return value == null ? "" : value;
  }

  static String orDash(String value) {
    return value == null ? DASH : value;
  }

  static String bankText(BankAccount bank) {
    return bank == null
        ? DASH
        : bank.getCode() + " - " + bank.getBankName() + " " + bank.getAccountNo();
  }

  static String payeeAccountText(PayeeAccount a) {
    return a == null
        ? DASH
        : a.getBankName() + " " + a.getAccountNo() + " (" + a.getAccountName() + ")";
  }

  static String text(LocalDate date) {
    return date == null ? DASH : date.toString();
  }

  /**
   * An amount with thousands separators and two decimals.
   *
   * @param value amount
   * @return text
   */
  static String amount(BigDecimal value) {
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
  }
}
