package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.EntryType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordParty;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The validations of an AR or OR creation record before it is saved (FRS.CSH.02.01.06 to 02.01.08,
 * 02.02.09.11 to 02.02.09.13) with the messages of Appendix F: required fields, the paid amount
 * above zero and at most the limit, the booked currency of each account, the check holding period
 * and the lengths of the text fields. Every failed check is returned with its field.
 */
public final class RecordValidation {

  /** Longest payor name. */
  static final int PAYOR_LENGTH = 250;

  /** Longest check number. */
  static final int CHECK_NO_LENGTH = 40;

  /** Longest remarks. */
  static final int REMARKS_LENGTH = 200;

  private static final String AMOUNT = "amount";
  private static final String CHECK_DATE = "checkDate";

  private RecordValidation() {}

  /**
   * Validates a creation record.
   *
   * @param draft the record as entered
   * @param rules settings and facts the checks need
   * @return message per field, empty when the record may be saved
   */
  public static Map<String, String> errors(Draft draft, Rules rules) {
    Map<String, String> errors = new LinkedHashMap<>();
    required(errors, draft, rules);
    money(errors, draft, rules);
    lengths(errors, draft);
    currencies(errors, draft, rules);
    holding(errors, draft.tender(), rules);
    return errors;
  }

  private static void required(Map<String, String> errors, Draft d, Rules rules) {
    RecordParty party = d.party();
    need(
        errors, "receiptType", d.receiptType(), d.kind() == ReceiptKind.OR ? "OR Type" : "AR Type");
    need(errors, "branchId", d.branchId(), "Receipting Branch");
    need(errors, "entryType", party.entryType(), "Entry Type");
    if (party.entryType() == EntryType.CLIENT && blank(party.clientName())) {
      need(errors, "clientCode", party.clientCode(), "Client");
    }
    if (party.entryType() == EntryType.INSURER || rules.nonPremium()) {
      requireInsurer(errors, party, rules);
    }
    need(errors, "payorName", party.payorName(), "Payor Name");
    requiredMoney(errors, d, rules);
  }

  private static void requiredMoney(Map<String, String> errors, Draft d, Rules rules) {
    RecordTender tender = d.tender();
    need(errors, "tenderType", tender.tenderType(), "Payment Type");
    need(errors, "currency", tender.currency(), "Currency");
    need(errors, "bankAccount", tender.bankAccount(), "Post to Bank Account");
    need(errors, AMOUNT, tender.amount(), "Paid Amount");
    if (tender.isCheck()) {
      need(errors, "checkNo", tender.checkOrEmpty().checkNo(), "Check Number");
      need(errors, CHECK_DATE, tender.checkOrEmpty().checkDate(), "Check Date");
    }
    if (rules.remarksRequired()) {
      need(errors, "remarks", tender.remarks(), "Remarks");
    }
    if (d.kind() == ReceiptKind.OR && positive(tender.wtax()) && blank(tender.certificateRef())) {
      errors.put(
          "certificateRef", "BIR 2307 Certificate Reference is required when tax is withheld");
    }
  }

  private static void requireInsurer(Map<String, String> errors, RecordParty party, Rules rules) {
    if (blank(party.insurerCode())) {
      errors.put(
          "insurerCode",
          rules.nonPremium()
              ? "A non-premium AR needs the paying insurer's code"
              : "Insurer is required");
    }
  }

  private static void money(Map<String, String> errors, Draft d, Rules rules) {
    BigDecimal amount = d.tender().amount();
    if (amount != null && (amount.signum() <= 0 || amount.compareTo(rules.maxAmount()) > 0)) {
      errors.put(
          AMOUNT,
          "Paid Amount must be above zero and not more than " + formatted(rules.maxAmount()));
    }
    if (amount != null && !d.accounts().isEmpty()) {
      BigDecimal split =
          d.accounts().stream().map(RecordAccount::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
      if (split.compareTo(amount) != 0) {
        errors.put("accounts", "The amounts of the accounts must add up to the Paid Amount");
      }
    }
  }

  private static void lengths(Map<String, String> errors, Draft d) {
    tooLong(errors, "payorName", d.party().payorName(), PAYOR_LENGTH, "Payor Name");
    tooLong(
        errors, "checkNo", d.tender().checkOrEmpty().checkNo(), CHECK_NO_LENGTH, "Check Number");
    tooLong(errors, "remarks", d.tender().remarks(), REMARKS_LENGTH, "Remarks");
  }

  private static void currencies(Map<String, String> errors, Draft d, Rules rules) {
    String currency = d.tender().currency();
    d.accounts().stream()
        .filter(a -> !blank(currency))
        .filter(a -> rules.accountCurrencies().get(a.reference()) != null)
        .filter(a -> !rules.accountCurrencies().get(a.reference()).equals(currency))
        .findFirst()
        .ifPresent(
            a ->
                errors.put(
                    "currency",
                    "The currency of account "
                        + a.reference()
                        + " is "
                        + rules.accountCurrencies().get(a.reference())
                        + "; the receipt is in "
                        + currency));
  }

  private static void holding(Map<String, String> errors, RecordTender tender, Rules rules) {
    LocalDate checkDate = tender.checkOrEmpty().checkDate();
    if (!tender.isCheck() || checkDate == null || rules.latestCheckDate() == null) {
      return;
    }
    if (checkDate.isAfter(rules.latestCheckDate())) {
      errors.put(
          CHECK_DATE,
          "The check date must be at least "
              + rules.holdingDays()
              + " working days before today (holding period)");
    }
  }

  private static void need(Map<String, String> errors, String field, Object value, String label) {
    boolean missing = value == null || value instanceof String s && s.isBlank();
    if (missing) {
      errors.putIfAbsent(field, label + " is required");
    }
  }

  private static void tooLong(
      Map<String, String> errors, String field, String value, int max, String label) {
    if (value != null && value.length() > max) {
      errors.putIfAbsent(field, label + " can have at most " + max + " characters");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static boolean positive(BigDecimal value) {
    return value != null && value.signum() > 0;
  }

  /**
   * An amount with thousands separators and two decimals.
   *
   * @param amount amount
   * @return for example 1,000,000,000.00
   */
  static String formatted(BigDecimal amount) {
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT))
        .format(amount);
  }

  /**
   * A creation record as entered.
   *
   * @param kind AR or OR
   * @param receiptType AR or OR type
   * @param branchId receipting branch
   * @param party entry type and payor
   * @param tender payment
   * @param accounts accounts with their paid amounts
   */
  public record Draft(
      ReceiptKind kind,
      String receiptType,
      Long branchId,
      RecordParty party,
      RecordTender tender,
      List<RecordAccount> accounts) {

    /** Defensive copy. */
    public Draft {
      accounts = accounts == null ? List.of() : List.copyOf(accounts);
    }
  }

  /**
   * What the checks need.
   *
   * @param remarksRequired remarks required (C25)
   * @param maxAmount highest paid amount
   * @param latestCheckDate latest check date accepted, null when the check date rule is off (C7)
   * @param holdingDays working days of the holding period
   * @param nonPremium a non-premium AR (the insurer is required)
   * @param accountCurrencies booked currency per account reference
   */
  public record Rules(
      boolean remarksRequired,
      BigDecimal maxAmount,
      LocalDate latestCheckDate,
      int holdingDays,
      boolean nonPremium,
      Map<String, String> accountCurrencies) {

    /** Defensive copy. */
    public Rules {
      accountCurrencies = Map.copyOf(accountCurrencies);
    }
  }
}
