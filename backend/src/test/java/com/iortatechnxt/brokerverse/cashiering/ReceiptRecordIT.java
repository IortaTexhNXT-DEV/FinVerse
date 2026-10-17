package com.iortatechnxt.brokerverse.cashiering;

import static com.iortatechnxt.brokerverse.cashiering.RecordFixtures.account;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.RecordStage;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.TenderType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringDecisions;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordPoster;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordPoster.Outcome;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptRecordService;
import com.iortatechnxt.brokerverse.cashiering.service.RecordValidation.Draft;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * BDOI's AR and OR creation records and their posting (Operations Cashiering FRS v3.2:
 * FRS.CSH.02.01, 02.02, 02.05): validations and messages, the record numbers, submission with the
 * notice to the Approver/Poster, posting with the AR number of the branch series, the return with a
 * reason, the maker-checker rule, the depleted series, the order check before cash and the OR of
 * Head Office.
 */
@IntegrationTest
class ReceiptRecordIT {

  @Autowired private CashFixtures fx;
  @Autowired private RecordFixtures rf;
  @Autowired private ReceiptRecordService records;
  @Autowired private ReceiptRecordPoster poster;
  @Autowired private CashReceiptRepository receipts;
  @Autowired private ApplicationRepository applications;
  @Autowired private UnappliedRepository unapplied;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private FieldValidationException refused(Draft draft) {
    return (FieldValidationException)
        catchThrowable(() -> as.run("cashier", () -> records.create(fx.company(), draft)));
  }

  @Test
  void theValidationsOfAnArCreationRecordGiveBdoiMessagesAndSaveNothing() {
    OpsInvoice invoice = fx.motorInvoice();
    Draft good =
        rf.ar(
            new BigDecimal("1000.00"), account(invoice.getInvoiceNo(), new BigDecimal("1000.00")));
    long before = jdbc.queryForObject("select count(*) from csh_receipt_record", Long.class);

    Draft noBank =
        new Draft(
            good.kind(),
            good.receiptType(),
            good.branchId(),
            good.party(),
            withBank(good, null),
            good.accounts());
    assertThat(refused(noBank).getFieldErrors())
        .containsEntry("bankAccount", "Post to Bank Account is required");
    assertThat(refused(rf.ar(new BigDecimal("0.00"))).getFieldErrors())
        .containsEntry(
            "amount", "Paid Amount must be above zero and not more than 1,000,000,000.00");
    assertThat(refused(rf.ar(new BigDecimal("1500000000.00"))).getFieldErrors())
        .containsKey("amount");
    Draft usd =
        new Draft(
            good.kind(),
            good.receiptType(),
            good.branchId(),
            good.party(),
            withCurrency(good, "USD"),
            good.accounts());
    assertThat(refused(usd).getFieldErrors().get("currency"))
        .isEqualTo(
            "The currency of account " + invoice.getInvoiceNo() + " is PHP; the receipt is in USD");
    Draft recentCheck =
        rf.ar(
            TenderType.CHECK,
            RecordFixtures.today(),
            new BigDecimal("1000.00"),
            account(invoice.getInvoiceNo(), new BigDecimal("1000.00")));
    assertThat(refused(recentCheck).getFieldErrors())
        .containsEntry(
            "checkDate",
            "The check date must be at least 4 working days before today (holding period)");
    assertThat(jdbc.queryForObject("select count(*) from csh_receipt_record", Long.class))
        .isEqualTo(before);

    Draft oldCheck =
        rf.ar(
            TenderType.CHECK,
            RecordFixtures.today().minusDays(14),
            new BigDecimal("1000.00"),
            account(invoice.getInvoiceNo(), new BigDecimal("1000.00")));
    assertThat(as.run("cashier", () -> records.create(fx.company(), oldCheck)).getStage())
        .isEqualTo(RecordStage.CREATED);
  }

  @Test
  void creationRecordsAreNumberedInSequenceAndACancelledRecordKeepsItsNumber() {
    ReceiptRecord first =
        as.run("cashier", () -> records.create(fx.company(), rf.ar(new BigDecimal("50.00"))));
    ReceiptRecord second =
        as.run("cashier", () -> records.create(fx.company(), rf.ar(new BigDecimal("60.00"))));
    assertThat(first.getRecordNo()).matches("CR-AR-\\d{6}");
    assertThat(seq(second.getRecordNo())).isEqualTo(seq(first.getRecordNo()) + 1);

    ReceiptRecord cancelled = as.run("cashier", () -> records.cancelRecord(first.getId()));
    assertThat(cancelled.label()).isEqualTo("Record Cancelled");
    ReceiptRecord third =
        as.run("cashier", () -> records.create(fx.company(), rf.ar(new BigDecimal("70.00"))));
    assertThat(seq(third.getRecordNo())).isEqualTo(seq(second.getRecordNo()) + 1);
    assertThat(records.get(first.getId()).getRecordNo()).isEqualTo(first.getRecordNo());
    as.run("cashier", () -> records.submit(third.getId()));
    assertThatThrownBy(() -> as.run("cashier", () -> records.cancelRecord(third.getId())))
        .extracting("code")
        .isEqualTo("RECORD_NOT_CREATED");
  }

  @Test
  void anArIsIssuedOnlyWhenTheApproverPostsItsRecordAndNotByItsCreator() {
    OpsInvoice invoice = fx.motorInvoice();
    BigDecimal due = invoice.premiumBalance();
    ReceiptRecord record = rf.submitted(rf.ar(due, account(invoice.getInvoiceNo(), due)));
    assertThat(record.getStage()).isEqualTo(RecordStage.FOR_POSTING);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = 'cashtl' and title = ?",
                Long.class,
                record.getRecordNo() + " for posting"))
        .isEqualTo(1L);
    assertThat(record.getReceiptNo()).isNull();

    List<Outcome> byCreator = as.run("cashier", () -> poster.post(List.of(record.getId())));
    assertThat(byCreator.get(0).posted()).isFalse();
    assertThat(byCreator.get(0).message())
        .isEqualTo("The requester cannot approve " + record.getRecordNo());

    Outcome done = rf.post(record.getId()).get(0);
    assertThat(done.posted()).isTrue();
    ReceiptRecord posted = records.get(record.getId());
    assertThat(posted.label()).isEqualTo("AR/OR Issued");
    Receipt ar = receipts.findById(posted.getReceiptId()).orElseThrow();
    assertThat(ar.getReceiptNo()).isEqualTo(done.receiptNo());
    assertThat(ar.getStatus()).isEqualTo(ReceiptStatus.ISSUED);
    assertThat(ar.getBranchId()).isEqualTo(fx.ho());
    assertThat(ar.getBankAccount()).isEqualTo(RecordFixtures.PHP_BANK);
    assertThat(ar.getAppliedAmount()).isEqualByComparingTo(due);
    assertThat(fx.invoice(invoice.getInvoiceNo()).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = 'cashier' and title = ?",
                Long.class,
                record.getRecordNo() + " posted"))
        .isEqualTo(1L);
  }

  @Test
  void aReturnedRecordShowsItsReasonCanBeEditedAndIsSubmittedAgain() {
    ReceiptRecord record = rf.submitted(rf.ar(new BigDecimal("300.00")));
    assertThatThrownBy(() -> as.run("cashtl", () -> poster.returnToCreator(record.getId(), " ")))
        .extracting("code")
        .isEqualTo("RECORD_RETURN_REASON");
    ReceiptRecord returned =
        as.run("cashtl", () -> poster.returnToCreator(record.getId(), "Wrong account"));
    assertThat(returned.label()).isEqualTo("Returned");
    assertThat(returned.getReturnReason()).isEqualTo("Wrong account");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = 'cashier' and title = ?"
                    + " and body like '%Wrong account%'",
                Long.class, record.getRecordNo() + " returned"))
        .isEqualTo(1L);
    ReceiptRecord edited =
        as.run("cashier", () -> records.edit(record.getId(), rf.ar(new BigDecimal("350.00"))));
    assertThat(edited.total()).isEqualByComparingTo("350.00");
    assertThat(as.run("cashier", () -> records.submit(record.getId())).getStage())
        .isEqualTo(RecordStage.FOR_POSTING);
  }

  @Test
  void postingSeveralRecordsPostsTheOthersWhenOneSeriesIsDepletedAndChecksGoFirst() {
    OpsInvoice invoice = fx.motorInvoice();
    Draft cash =
        rf.ar(new BigDecimal("100.00"), account(invoice.getInvoiceNo(), new BigDecimal("100.00")));
    Draft check =
        rf.ar(
            TenderType.CHECK,
            RecordFixtures.today().minusDays(14),
            new BigDecimal("200.00"),
            account(invoice.getInvoiceNo(), new BigDecimal("200.00")));
    Draft noSeries =
        new Draft(
            ReceiptKind.AR,
            "PREMIUM",
            branchWithoutSeries(),
            cash.party(),
            cash.tender(),
            List.of());
    ReceiptRecord cashRecord = rf.submitted(cash);
    ReceiptRecord checkRecord = rf.submitted(check);
    ReceiptRecord depleted = rf.submitted(noSeries);

    List<Outcome> outcomes = rf.post(cashRecord.getId(), checkRecord.getId(), depleted.getId());
    assertThat(outcomes).extracting(Outcome::recordNo).first().isEqualTo(checkRecord.getRecordNo());
    assertThat(outcomes).filteredOn(Outcome::posted).hasSize(2);
    Outcome failed = outcomes.stream().filter(o -> !o.posted()).findFirst().orElseThrow();
    assertThat(failed.recordNo()).isEqualTo(depleted.getRecordNo());
    assertThat(failed.message())
        .isEqualTo(
            "Receipt Number series for this Branch is depleted. Contact your Administrator.");
    assertThat(records.get(depleted.getId()).getStage()).isEqualTo(RecordStage.FOR_POSTING);
    Application checkApp =
        applications
            .findByReceiptIdOrderByIdAsc(records.get(checkRecord.getId()).getReceiptId())
            .get(0);
    Application cashApp =
        applications
            .findByReceiptIdOrderByIdAsc(records.get(cashRecord.getId()).getReceiptId())
            .get(0);
    assertThat(checkApp.getId()).isLessThan(cashApp.getId());
  }

  @Test
  void aPaymentSplitOverAccountsAppliesEachAndKeepsTheExcessAndThePrebooked() {
    OpsInvoice one = fx.motorInvoice();
    OpsInvoice two = fx.motorInvoice();
    String pending = fx.unbooked().getArn();
    BigDecimal over = one.premiumBalance().add(new BigDecimal("500.00"));
    BigDecimal part = new BigDecimal("1000.00");
    BigDecimal waiting = new BigDecimal("250.00");
    BigDecimal total = over.add(part).add(waiting);
    ReceiptRecord record =
        rf.submitted(
            rf.ar(
                total,
                account(one.getInvoiceNo(), over),
                account(two.getInvoiceNo(), part),
                account(pending, waiting)));
    rf.post(record.getId());
    Long arId = records.get(record.getId()).getReceiptId();
    assertThat(applications.findByReceiptIdOrderByIdAsc(arId))
        .extracting(Application::getInvoiceNo)
        .containsExactly(one.getInvoiceNo(), two.getInvoiceNo());
    assertThat(unapplied.findByReceiptIdOrderByIdAsc(arId))
        .anySatisfy(
            u -> {
              assertThat(u.getOrigin()).isEqualTo(UnappliedOrigin.EXCESS);
              assertThat(u.getAmount()).isEqualByComparingTo("500.00");
            });
    assertThat(
            jdbc.queryForObject(
                "select amount from csh_prebooked where receipt_id = ? and arn = ?",
                BigDecimal.class,
                arId,
                pending))
        .isEqualByComparingTo(waiting);
    assertThat(fx.invoice(two.getInvoiceNo()).getPaymentStatus())
        .isEqualTo(PaymentStatus.PARTIALLY_PAID);
  }

  @Test
  void anOrRecordIsSavedByHeadOfficeAndIssuedFromTheHeadOfficeSeries() {
    OpsInvoice invoice = fx.motorInvoice();
    String insurer = invoice.getInsurerCode();
    ReceiptRecord fee =
        as.run(
            "cashier",
            () ->
                records.create(
                    fx.company(),
                    rf.or(
                        "SERVICE_FEE",
                        insurer,
                        new BigDecimal("11200.00"),
                        new BigDecimal("1200.00"),
                        BigDecimal.ZERO)));
    assertThat(fee.getRecordNo()).matches("CR-OR-\\d{6}");
    assertThat(fee.label()).isEqualTo("Created");
    Draft commission =
        rf.or(
            "COMMISSION",
            insurer,
            new BigDecimal("9800.00"),
            BigDecimal.ZERO,
            new BigDecimal("200.00"));
    Draft withoutCertificate =
        new Draft(
            commission.kind(),
            commission.receiptType(),
            commission.branchId(),
            commission.party(),
            withCertificate(commission, null),
            List.of());
    assertThat(refused(withoutCertificate).getFieldErrors())
        .containsEntry(
            "certificateRef", "BIR 2307 Certificate Reference is required when tax is withheld");
    Draft branch =
        new Draft(
            commission.kind(),
            commission.receiptType(),
            fx.branch("CEB"),
            commission.party(),
            commission.tender(),
            List.of());
    assertThat(refused(branch).getFieldErrors())
        .containsEntry("branchId", "Official receipts are issued by Head Office only");

    as.run("cashier", () -> records.submit(fee.getId()));
    Outcome posted = rf.post(fee.getId()).get(0);
    Receipt or = receipts.findById(records.get(fee.getId()).getReceiptId()).orElseThrow();
    assertThat(or.getKind()).isEqualTo(ReceiptKind.OR);
    assertThat(or.getReceiptNo()).isEqualTo(posted.receiptNo());
    assertThat(or.getGross()).isEqualByComparingTo("10000.00");
    assertThat(or.getVat()).isEqualByComparingTo("1200.00");
  }

  @Test
  void aNonPremiumArNeedsTheInsurerAndDirectIssuanceWaitsForThePostingStep() {
    Draft premium = rf.ar(new BigDecimal("25000.00"));
    Draft refund =
        new Draft(ReceiptKind.AR, "REFUND", fx.ho(), premium.party(), premium.tender(), List.of());
    assertThat(refused(refund).getFieldErrors())
        .containsEntry("insurerCode", "A non-premium AR needs the paying insurer's code");
    assertThat(records.receiptingBranches(fx.company(), ReceiptKind.OR))
        .allMatch(b -> b.isHeadOffice());
    assertThat(records.receiptingBranches(fx.company(), ReceiptKind.AR))
        .extracting(b -> b.getId())
        .contains(fx.ho());
    assertThatThrownBy(() -> decisions.requireDirectIssue(ReceiptKind.AR))
        .extracting("code")
        .isEqualTo("RECEIPT_POSTING_STEP");
  }

  private Long branchWithoutSeries() {
    return jdbc.queryForObject(
        "select min(b.id) from org_branch b where b.company_id = ? and not exists"
            + " (select 1 from csh_receipt_series s where s.branch_id = b.id and s.kind = 'AR'"
            + " and s.record_status = 'ACTIVE' and s.next_no <= s.to_no)",
        Long.class,
        fx.company());
  }

  private static int seq(String recordNo) {
    return Integer.parseInt(recordNo.substring(recordNo.lastIndexOf('-') + 1));
  }

  private static RecordTender withBank(Draft d, String bank) {
    var t = d.tender();
    return new RecordTender(
        t.tenderType(),
        t.currency(),
        bank,
        t.amount(),
        t.vat(),
        t.wtax(),
        t.certificateRef(),
        t.check(),
        t.receiptDate(),
        t.remarks(),
        t.otherIncomeRef());
  }

  private static RecordTender withCurrency(Draft d, String currency) {
    var t = d.tender();
    return new RecordTender(
        t.tenderType(),
        currency,
        t.bankAccount(),
        t.amount(),
        t.vat(),
        t.wtax(),
        t.certificateRef(),
        t.check(),
        t.receiptDate(),
        t.remarks(),
        t.otherIncomeRef());
  }

  private static RecordTender withCertificate(Draft d, String ref) {
    var t = d.tender();
    return new RecordTender(
        t.tenderType(),
        t.currency(),
        t.bankAccount(),
        t.amount(),
        t.vat(),
        t.wtax(),
        ref,
        t.check(),
        t.receiptDate(),
        t.remarks(),
        t.otherIncomeRef());
  }

  @Autowired private CashieringDecisions decisions;
}
