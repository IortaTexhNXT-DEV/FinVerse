package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkOutcome;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.MatchCategory;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFile;
import com.iortatechnxt.brokerverse.cashiering.domain.ChannelFileRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Payment;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentMatcher.Kind;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentMatcher.Match;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * The Direct Credit file in BDOI's reading (FRS.CSH.05.01.09; Appendix R, C8, setting {@code
 * CASH_DIRECT_CREDIT_MODE = IDENTIFY}): a row identifies a payment already received and receipted
 * by a Bills Payment file; no second AR is issued.
 *
 * <ol>
 *   <li>Priority 1, file against the payment records: BP filename and transaction number, the paid
 *       amount equal to the amount of the payment, and the payment still Unbooked/Unmatched; else
 *       the row is tagged Invalid BP filename, Invalid Transaction Number or Duplicate.
 *   <li>Priority 2, file against the booked accounts on the reference numbers of the row: the
 *       Unbooked/Unmatched payment is applied to the account found.
 *   <li>Priority 3, against the pre-booked accounts: the payment is tagged Matched on Pre-booked
 *       and is applied by the hourly run once the account is booked; else it stays
 *       Unbooked/Unmatched.
 * </ol>
 */
@Component
public class DirectCreditIdentifyHandler implements BulkImportHandler {

  /** Outcomes of a row. */
  static final String INVALID_FILE = "INVALID_BP_FILENAME";

  static final String INVALID_TRANSACTION = "INVALID_TRANSACTION_NUMBER";

  static final String DUPLICATE = "DUPLICATE";

  static final String APPLIED = "APPLIED";

  static final String PREBOOKED = "MATCHED_ON_PREBOOKED";

  static final String UNMATCHED = "UNBOOKED_UNMATCHED";

  private final ChannelFileRepository files;
  private final PaymentRepository payments;
  private final UnappliedRepository unapplied;
  private final PaymentMatcher matcher;
  private final AutomatchService automatch;

  /**
   * Creates the handler.
   *
   * @param files payment files received (BP filename)
   * @param payments payments of the Bills Payment runs
   * @param unapplied unapplied payment of a payment
   * @param matcher accounts of a reference
   * @param automatch application of an unapplied payment
   */
  public DirectCreditIdentifyHandler(
      ChannelFileRepository files,
      PaymentRepository payments,
      UnappliedRepository unapplied,
      PaymentMatcher matcher,
      AutomatchService automatch) {
    this.files = files;
    this.payments = payments;
    this.unapplied = unapplied;
    this.matcher = matcher;
    this.automatch = automatch;
  }

  @Override
  public String code() {
    return ChannelFileService.DC_IDENTIFY;
  }

  @Override
  public String title() {
    return "Direct Credit File (identification of payments received)";
  }

  @Override
  public String permission() {
    return PaymentFileHandler.PERMISSION;
  }

  @Override
  public String filledBy() {
    return "Cashiering officers, from the bank's Direct Credit file of the payments already received";
  }

  @Override
  public String instructions() {
    return "Each row names the Bills Payment file (BP filename) and the transaction number of a payment"
        + " already received; the payment is identified and applied to its accounts.";
  }

  @Override
  public String uploadPath() {
    return "Cashiering > Payment Files, type Direct Credit";
  }

  @Override
  public List<BulkColumn> columns() {
    return PaymentFileHandlers.directCreditColumns();
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (row.text("BP filename") == null) {
      errors.add("The row carries no BP filename");
    }
    BigDecimal amount = row.number("Paid amount");
    if (amount == null || amount.signum() <= 0) {
      errors.add("The paid amount must be above zero");
    }
    return errors;
  }

  @Override
  public BulkOutcome process(BulkRow row, BulkContext context) {
    String bp = row.text("BP filename");
    String txn = row.text("Transaction no");
    List<ChannelFile> bills =
        files.findByCompanyIdAndFileNameIgnoreCaseAndBulkJobNoNotNullOrderByIdDesc(
            context.companyId(), bp.strip());
    if (bills.isEmpty()) {
      return new BulkOutcome(bp, INVALID_FILE);
    }
    Optional<Payment> found = payment(bills, txn, row.number("Paid amount"));
    if (found.isEmpty()) {
      return new BulkOutcome(bp + " " + txn, INVALID_TRANSACTION);
    }
    Payment payment = found.get();
    if (payment.getMatchCategory() != MatchCategory.UNAPPLIED_NO_MATCH) {
      return new BulkOutcome(payment.getPaymentNo(), DUPLICATE);
    }
    return identify(payment, references(row), context);
  }

  private Optional<Payment> payment(List<ChannelFile> bills, String txn, BigDecimal amount) {
    for (ChannelFile bill : bills) {
      for (Payment p : payments.findByBatchRefOrderByIdAsc(bill.getBulkJobNo())) {
        if (sameTransaction(p.getSourceKey(), txn) && p.getAmount().compareTo(amount) == 0) {
          return Optional.of(p);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Whether a payment's source key ends with the transaction number (leading zeros ignored).
   *
   * @param sourceKey source key of the payment (for example BILLS:2025-04-03:00001)
   * @param txn transaction number of the row
   * @return true when they are the same transaction
   */
  static boolean sameTransaction(String sourceKey, String txn) {
    if (txn == null || txn.isBlank() || sourceKey == null) {
      return false;
    }
    String last = sourceKey.substring(sourceKey.lastIndexOf(':') + 1);
    return strip(last).equals(strip(txn));
  }

  private static String strip(String value) {
    String trimmed = value.strip().replaceFirst("^0+(?=.)", "");
    return trimmed.isEmpty() ? "0" : trimmed;
  }

  private BulkOutcome identify(Payment payment, List<String> refs, BulkContext context) {
    Match match = matcher.match(context.companyId(), refs);
    if (match.kind() == Kind.BOOKED) {
      Optional<Unapplied> item =
          unapplied.findByReceiptIdOrderByIdAsc(payment.getReceiptId()).stream()
              .filter(u -> payment.getId().equals(u.getPaymentId()))
              .filter(u -> Unapplied.STAGE_INITIAL.equals(u.getStage()))
              .findFirst();
      if (item.isPresent() && automatch.identify(item.get(), refs, context.businessDate())) {
        payment.matched(
            MatchCategory.APPLIED,
            match.reference(),
            payment.getAmount(),
            "Identified by the Direct Credit file");
        return new BulkOutcome(payment.getPaymentNo(), APPLIED);
      }
    }
    if (match.kind() == Kind.PREBOOKED) {
      payment.matched(
          MatchCategory.PREBOOKED,
          match.account().getArn(),
          BigDecimal.ZERO,
          "Matched on Pre-booked by the Direct Credit file");
      return new BulkOutcome(payment.getPaymentNo(), PREBOOKED);
    }
    return new BulkOutcome(payment.getPaymentNo(), UNMATCHED);
  }

  private static List<String> references(BulkRow row) {
    List<String> refs = new ArrayList<>();
    for (String column : List.of("Account ref no", "EBIX_RefNo")) {
      String value = row.text(column);
      if (value != null && !value.isBlank()) {
        refs.add(value.strip());
      }
    }
    return refs;
  }

  @Override
  public List<String> outcomeCategories() {
    return List.of(APPLIED, PREBOOKED, UNMATCHED, DUPLICATE, INVALID_FILE, INVALID_TRANSACTION);
  }

  @Override
  public boolean blocksDuplicateFiles() {
    return true;
  }
}
