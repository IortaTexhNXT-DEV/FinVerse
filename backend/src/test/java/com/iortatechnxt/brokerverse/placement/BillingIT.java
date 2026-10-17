package com.iortatechnxt.brokerverse.placement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.placement.domain.BillingBatch;
import com.iortatechnxt.brokerverse.placement.domain.BillingBatchStatus;
import com.iortatechnxt.brokerverse.placement.domain.BillingItem;
import com.iortatechnxt.brokerverse.placement.domain.BillingItemStatus;
import com.iortatechnxt.brokerverse.placement.domain.MatchStatus;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReport;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportKind;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportLine;
import com.iortatechnxt.brokerverse.placement.domain.PaymentReportStatus;
import com.iortatechnxt.brokerverse.placement.service.BillingService;
import com.iortatechnxt.brokerverse.placement.service.BillingService.BillingFile;
import com.iortatechnxt.brokerverse.placement.service.PaymentGateService;
import com.iortatechnxt.brokerverse.placement.service.PaymentReportConfirmation;
import com.iortatechnxt.brokerverse.placement.service.PaymentReportService;
import com.iortatechnxt.brokerverse.placement.service.PaymentReportService.ReportUpload;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/** CLPC billing file and payment report matching (BRNB.067/068). */
@IntegrationTest
class BillingIT {

  @Autowired private BillingService billing;
  @Autowired private PaymentReportService reports;
  @Autowired private PaymentReportConfirmation confirmation;
  @Autowired private PaymentGateService gate;
  @Autowired private AccountQueryService queries;
  @Autowired private PlacementTestData fx;
  @Autowired private AsUser as;

  private PaymentReport upload(PaymentReportKind kind, Long batchId, String csv) {
    return as.run(
        "proc",
        () ->
            reports.upload(
                new ReportUpload(
                    fx.company(),
                    kind,
                    batchId,
                    "report.csv",
                    csv.getBytes(StandardCharsets.UTF_8))));
  }

  private static PaymentReportLine line(PaymentReport report, int row) {
    return report.getLines().stream().filter(l -> l.getRowNo() == row).findFirst().orElseThrow();
  }

  @Test
  void cbgFireAccountsAreBilledAndTheClpcReportOpensTheirGate() {
    Account a = fx.awaitingPayment(fx.fire());
    Account b = fx.awaitingPayment(fx.fire());
    Account c = fx.awaitingPayment(fx.fire());
    Account motor = fx.awaitingPayment(fx.motor());
    List<String> candidates =
        billing.candidates(fx.company()).stream().map(Account::getArn).toList();
    assertThat(candidates)
        .contains(a.getArn(), b.getArn(), c.getArn())
        .doesNotContain(motor.getArn());
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () -> billing.create(fx.company(), List.of(a.getArn(), motor.getArn()))))
        .extracting("code")
        .isEqualTo("BILLING_NOT_CANDIDATE");

    BillingBatch batch =
        as.run(
            "proc",
            () -> billing.create(fx.company(), List.of(a.getArn(), b.getArn(), c.getArn())));
    assertThat(batch.getItemCount()).isEqualTo(3);
    assertThat(batch.getStatus()).isEqualTo(BillingBatchStatus.GENERATED);
    BillingItem first = batch.getItems().get(0);
    assertThat(first.getPnNumbers()).isEqualTo(a.getPnNumbers().get(0));
    assertThat(first.getLoanApplicationNo()).isEqualTo(a.getLoanApplicationNo());
    assertThat(first.getBorrower()).isEqualTo(a.getClientName());
    assertThat(first.getPremium()).isEqualByComparingTo(a.getPremium().grossPremium());
    assertThat(billing.candidates(fx.company()))
        .extracting(Account::getArn)
        .doesNotContain(a.getArn());
    BillingFile xlsx = as.run("proc", () -> billing.file(batch.getId(), "xlsx"));
    BillingFile ods = as.run("proc", () -> billing.file(batch.getId(), "ods"));
    assertThat(xlsx.fileName()).endsWith(".xlsx");
    assertThat(ods.content()).isNotEmpty();
    assertThat(ods.toString()).contains(".ods");
    assertThat(xlsx).isNotEqualTo(ods).hasSameHashCodeAs(xlsx);
    assertThatThrownBy(() -> as.run("proc", () -> billing.file(batch.getId(), "pdf")))
        .extracting("code")
        .isEqualTo("BILLING_FORMAT");
    assertThat(billing.batches(fx.company(), PageRequest.of(0, 5))).isNotEmpty();

    assertThatThrownBy(() -> upload(PaymentReportKind.CLPC, null, "PN No.\nX\n"))
        .extracting("code")
        .isEqualTo("PAYMENT_REPORT_BATCH_REQUIRED");
    PaymentReport report =
        upload(
            PaymentReportKind.CLPC,
            batch.getId(),
            "PN No.,Loan Application No.,Status,Amount\n"
                + a.getPnNumbers().get(0)
                + ",,PAID,100\n"
                + ","
                + b.getLoanApplicationNo()
                + ",PAID,100\n"
                + c.getPnNumbers().get(0)
                + ",,UNPAID,\n"
                + "PN-NOT-BILLED,,PAID,5\n");
    assertThat(report.getStatus()).isEqualTo(PaymentReportStatus.REVIEW);
    assertThat(line(report, 2).getMatchStatus()).isEqualTo(MatchStatus.MATCHED);
    assertThat(line(report, 3).getArn()).isEqualTo(b.getArn());
    assertThat(line(report, 4).getMatchStatus()).isEqualTo(MatchStatus.UNPAID);
    assertThat(line(report, 5).getMatchStatus()).isEqualTo(MatchStatus.UNMATCHED);
    assertThat(billing.get(batch.getId()).getStatus())
        .isEqualTo(BillingBatchStatus.REPORT_RECEIVED);

    PaymentReport confirmed = as.run("proc", () -> confirmation.confirm(report.getId()));
    assertThat(confirmed.getStatus()).isEqualTo(PaymentReportStatus.CONFIRMED);
    assertThat(line(confirmed, 2).isApplied()).isTrue();
    assertThat(queries.requireByArn(a.getArn()).getStatus())
        .isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(queries.requireByArn(b.getArn()).getStatus())
        .isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(queries.requireByArn(c.getArn()).getStatus())
        .isEqualTo(AccountStatus.AWAITING_PAYMENT);
    assertThat(gate.view(a.getArn()).evidence()).hasSize(1);
    BillingBatch closed = billing.get(batch.getId());
    assertThat(closed.getStatus()).isEqualTo(BillingBatchStatus.CLOSED);
    assertThat(closed.getItems())
        .extracting(BillingItem::getPaymentStatus)
        .containsExactly(BillingItemStatus.PAID, BillingItemStatus.PAID, BillingItemStatus.UNPAID);
    assertThat(billing.candidates(fx.company())).extracting(Account::getArn).contains(c.getArn());
    assertThatThrownBy(() -> as.run("proc", () -> confirmation.confirm(report.getId())))
        .extracting("code")
        .isEqualTo("PAYMENT_REPORT_CLOSED");
    assertThatThrownBy(() -> upload(PaymentReportKind.CLPC, batch.getId(), "PN No.\nX\n"))
        .extracting("code")
        .isEqualTo("BILLING_BATCH_CLOSED");
  }

  @Test
  void otherSegmentsAreMatchedByArnWithAReviewOfUnmatchedLines() {
    Account liability = fx.awaitingPayment(fx.liability());
    Account second = fx.awaitingPayment(fx.liability());
    String ready = fx.ready(fx.liability());
    PaymentReport report =
        upload(
            PaymentReportKind.REFERENCE,
            null,
            "ARN,Status,Payment Date\n"
                + liability.getArn()
                + ",PAID,2026-09-10\n"
                + "ARN-1999-000001,PAID,\n"
                + ready
                + ",PAID,\n"
                + second.getArn()
                + ",N,\n");
    assertThat(line(report, 2).getMatchStatus()).isEqualTo(MatchStatus.MATCHED);
    assertThat(line(report, 3).getMatchStatus()).isEqualTo(MatchStatus.UNMATCHED);
    assertThat(line(report, 4).getMessage()).contains("not awaiting payment");
    assertThat(line(report, 5).getMatchStatus()).isEqualTo(MatchStatus.UNPAID);
    assertThat(reports.reports(fx.company(), PageRequest.of(0, 5))).isNotEmpty();

    Long lineId = line(report, 3).getId();
    assertThatThrownBy(
            () -> as.run("proc", () -> reports.resolveLine(report.getId(), lineId, ready)))
        .extracting("code")
        .isEqualTo("ACCOUNT_NOT_AWAITING_PAYMENT");
    Long matchedId = line(report, 2).getId();
    assertThatThrownBy(
            () ->
                as.run(
                    "proc", () -> reports.resolveLine(report.getId(), matchedId, second.getArn())))
        .extracting("code")
        .isEqualTo("PAYMENT_LINE_MATCHED");
    PaymentReport resolved =
        as.run("proc", () -> reports.resolveLine(report.getId(), lineId, second.getArn()));
    assertThat(line(resolved, 3).isManuallyMatched()).isTrue();
    assertThat(line(resolved, 3).getMatchStatus()).isEqualTo(MatchStatus.MATCHED);

    PaymentReport confirmed = as.run("proc", () -> confirmation.confirm(report.getId()));
    assertThat(queries.requireByArn(liability.getArn()).getStatus())
        .isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(queries.requireByArn(second.getArn()).getStatus())
        .isEqualTo(AccountStatus.READY_FOR_PLACEMENT);
    assertThat(line(confirmed, 2).getApplyMessage()).isEqualTo("Payment gate opened");

    PaymentReport other =
        upload(PaymentReportKind.REFERENCE, null, "Reference\n" + liability.getArn() + "\n");
    PaymentReport discarded = as.run("proc", () -> reports.discard(other.getId()));
    assertThat(discarded.getStatus()).isEqualTo(PaymentReportStatus.DISCARDED);
  }
}
