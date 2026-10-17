package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentChannel;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.OrAmounts;
import com.iortatechnxt.brokerverse.cashiering.domain.PaymentIntake;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptLine;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordParty;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender.RecordCheck;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.ArIssue;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.OrIssue;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.Allocation;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeResult;
import com.iortatechnxt.brokerverse.cashiering.service.PaymentIntakeService.IntakeTarget;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues the AR or OR of a posted creation record (FRS.CSH.02.05.06): the AR number is the next of
 * the receipting branch's series and the OR number the next of Head Office's; the payment is
 * applied to each account selected (less the PR 2307 of an account with the BIR 2307 tag), any
 * excess becomes an Excess payment record of the Unapplied list, an account not yet booked a
 * Pre-booked payment, and the journals are posted with the AR / OR number as GL reference.
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class RecordIssuer {

  private static final String RECORD_REF = "REC:";
  private static final int RATIO_SCALE = 10;

  private final PaymentIntakeService intake;
  private final CashReceiptService receipts;
  private final Clock clock;

  /**
   * Creates the issuer.
   *
   * @param intake payment intake (matching and application)
   * @param receipts AR and OR issuance
   * @param clock clock
   */
  public RecordIssuer(PaymentIntakeService intake, CashReceiptService receipts, Clock clock) {
    this.intake = intake;
    this.receipts = receipts;
    this.clock = clock;
  }

  /**
   * Issues the receipt of a creation record and marks the record posted.
   *
   * @param record creation record For Posting
   * @param poster Approver/Poster
   * @param at time of the posting
   * @return AR or OR number
   */
  public String issue(ReceiptRecord record, String poster, Instant at) {
    Receipt receipt = record.getReceiptKind() == ReceiptKind.OR ? issueOr(record) : issueAr(record);
    record.posted(receipt, poster, at, receipt.getJournalBatchNo());
    return receipt.getReceiptNo();
  }

  private Receipt issueAr(ReceiptRecord record) {
    RecordTender money = record.getTender();
    RecordParty party = record.getParty();
    LocalDate date = money.receiptDate() == null ? today() : money.receiptDate();
    if (ReceiptRecordService.NON_PREMIUM.contains(record.getReceiptType())) {
      return receipts.issueAr(
          new ArIssue(
              record.getCompanyId(),
              record.getBranchId(),
              record.getReceiptType(),
              date,
              party.insurerCode(),
              party.payorName(),
              null,
              null,
              money.currency(),
              money.amount(),
              tender(record, mode(money))),
          money.bankAccount());
    }
    List<IntakeResult> results =
        intake.receiveAllocated(
            new IntakeTarget(record.getCompanyId(), record.getBranchId(), "OTC", ReceiptSource.OTC),
            payment(record, date),
            record.getAccounts().stream()
                .map(a -> new Allocation(a.reference(), a.amount()))
                .toList(),
            money.bankAccount());
    return results.get(0).receipt();
  }

  private static PaymentIntake payment(ReceiptRecord record, LocalDate date) {
    RecordTender money = record.getTender();
    RecordCheck check = money.checkOrEmpty();
    return new PaymentIntake(
        PaymentChannel.OTC,
        record.getRecordNo(),
        RECORD_REF + record.getRecordNo(),
        null,
        null,
        List.of(),
        new PaymentIntake.Payor(record.getParty().partyCode(), record.getParty().payorName()),
        record.getParty().clientName(),
        new PaymentIntake.Money(money.amount(), money.currency(), date),
        new PaymentIntake.Tender(mode(money), check.checkNo(), check.checkBank(), null, false));
  }

  private Receipt issueOr(ReceiptRecord record) {
    RecordTender money = record.getTender();
    RecordParty party = record.getParty();
    return receipts.issueOr(
        new OrIssue(
            record.getCompanyId(),
            record.getBranchId(),
            record.getReceiptType(),
            today(),
            party.partyCode(),
            party.payorName(),
            money.currency(),
            lines(record),
            tender(record, mode(money)),
            false),
        money.bankAccount());
  }

  /**
   * The lines of an OR: one per account with its share of the gross, VAT and withholding tax, or
   * one line without accounts. The gross is the paid amount less VAT plus the tax withheld.
   *
   * @param record OR creation record
   * @return lines
   */
  static List<ReceiptLine> lines(ReceiptRecord record) {
    RecordTender money = record.getTender();
    BigDecimal vat = nz(money.vat());
    BigDecimal wtax = nz(money.wtax());
    BigDecimal gross = money.amount().subtract(vat).add(wtax);
    String text = record.getReceiptType() + nzText(money.remarks());
    List<RecordAccount> accounts = record.getAccounts();
    if (accounts.isEmpty()) {
      return List.of(
          CashReceiptService.line(
              money.otherIncomeRef(), party(record), new OrAmounts(gross, vat, wtax), text));
    }
    List<ReceiptLine> lines = new ArrayList<>();
    OrAmounts left = new OrAmounts(gross, vat, wtax);
    for (int i = 0; i < accounts.size(); i++) {
      RecordAccount a = accounts.get(i);
      OrAmounts share =
          i == accounts.size() - 1 ? left : share(new OrAmounts(gross, vat, wtax), a, money);
      left =
          new OrAmounts(
              left.gross().subtract(share.gross()),
              left.vat().subtract(share.vat()),
              left.wtax().subtract(share.wtax()));
      lines.add(CashReceiptService.line(a.reference(), party(record), share, text));
    }
    return lines;
  }

  private static OrAmounts share(OrAmounts whole, RecordAccount account, RecordTender money) {
    BigDecimal ratio = account.amount().divide(money.amount(), RATIO_SCALE, RoundingMode.HALF_UP);
    return new OrAmounts(
        Money.round(whole.gross().multiply(ratio)),
        Money.round(whole.vat().multiply(ratio)),
        Money.round(whole.wtax().multiply(ratio)));
  }

  private static String party(ReceiptRecord record) {
    return record.getParty().insurerCode();
  }

  private static ReceiptTender tender(ReceiptRecord record, PaymentMode mode) {
    RecordTender money = record.getTender();
    RecordCheck check = money.checkOrEmpty();
    return new ReceiptTender(
        mode,
        check.checkNo(),
        check.checkBank(),
        check.checkDate(),
        money.certificateRef(),
        ReceiptSource.OTC,
        CashieringSettings.MODULE,
        RECORD_REF + record.getRecordNo(),
        money.remarks());
  }

  private static PaymentMode mode(RecordTender money) {
    return switch (money.tenderType()) {
      case CHECK -> PaymentMode.CHECK;
      case DIRECT_CREDIT -> PaymentMode.DIRECT_CREDIT;
      case CASH -> PaymentMode.CASH;
    };
  }

  private LocalDate today() {
    return BusinessClock.today(clock);
  }

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO.setScale(2) : value;
  }

  private static String nzText(String remarks) {
    return remarks == null || remarks.isBlank() ? "" : " - " + remarks.strip();
  }
}
