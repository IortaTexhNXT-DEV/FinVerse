package com.iortatechnxt.brokerverse.frbs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.LineStatus;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeItem;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipient.RecipientValues;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRule;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRule.RuleValues;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeQueryService;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeRunService;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeSetupService;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeTagService;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service-fee runs end to end (FRBS 2.10.0-2.10.2): computation from fully paid invoices, four-eyes
 * approval with the accrual FRBS_SERVICE_FEE_ACCRUE and the payout request to Disbursement (type
 * SERVICE_FEE), release when Disbursement pays, liquidation with the unit's report, return, resend,
 * recompute and cancellation.
 */
@IntegrationTest
class ServiceFeeIT {

  private static final byte[] PDF =
      "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

  @Autowired private FrbsFixtures fx;
  @Autowired private ServiceFeeRunService runs;
  @Autowired private ServiceFeeQueryService queries;
  @Autowired private ServiceFeeTagService tags;
  @Autowired private ServiceFeeSetupService setup;
  @Autowired private WorkflowService workflow;
  @Autowired private JournalBatchRepository journals;
  @Autowired private AsUser as;

  @Test
  void anApprovedRunIsAccruedPaidReleasedAndLiquidated() {
    String invoiceNo = fx.paidInvoice();
    fx.costCentreRule();
    ServiceFeeRun run =
        as.run(
            FrbsFixtures.OFFICER,
            () -> runs.compute(fx.company(), FrbsFixtures.today(), FrbsFixtures.today()));
    assertThat(run.getStage()).isEqualTo(RunStage.COMPUTED);
    assertThat(run.getRunNo()).startsWith("SFR-");
    List<ServiceFeeItem> items = queries.items(run.getId());
    ServiceFeeItem mine =
        items.stream().filter(i -> i.getInvoiceNo().equals(invoiceNo)).findFirst().orElseThrow();
    assertThat(mine.getFee())
        .isEqualByComparingTo(
            mine.getBase()
                .multiply(new BigDecimal("0.025"))
                .setScale(2, java.math.RoundingMode.HALF_UP));
    assertThat(mine.getBase()).isEqualByComparingTo(mine.getCommission().subtract(mine.getWtax()));

    assertThatThrownBy(() -> as.run(FrbsFixtures.OFFICER, () -> runs.approve(run.getId(), null)))
        .hasMessageContaining("COMPUTED");
    as.run(FrbsFixtures.OFFICER, () -> runs.submit(run.getId(), "Please approve"));
    assertThatThrownBy(() -> as.run("glhead", () -> runs.submit(run.getId(), null)))
        .hasMessageContaining("FOR_APPROVAL");
    ServiceFeeRun approved = as.run(FrbsFixtures.LEAD, () -> runs.approve(run.getId(), "OK"));
    assertThat(approved.getStage()).isEqualTo(RunStage.APPROVED);
    assertThat(approved.getApprovedBy()).isEqualTo(FrbsFixtures.LEAD);

    List<ServiceFeeLine> lines = fx.paidLines(run);
    assertThat(lines).isNotEmpty().allMatch(l -> l.getStatus() == LineStatus.SENT);
    ServiceFeeLine first = lines.get(0);
    assertThat(first.getAccrualBatchNo()).isNotNull();
    assertThat(journals.findByCompanyIdAndBatchNo(fx.company(), first.getAccrualBatchNo()))
        .isPresent();
    DisbursementRequest request = fx.request(run, first);
    assertThat(request.getRequestType()).isEqualTo(DisbursementRequest.Type.SERVICE_FEE);
    assertThat(request.getRfpNo()).isEqualTo(run.getRunNo());
    assertThat(request.getAmount()).isEqualByComparingTo(first.getFee());

    for (ServiceFeeLine line : lines) {
      fx.pay(run, line);
    }
    assertThat(fx.paidLines(run)).allMatch(l -> l.getStatus() == LineStatus.RELEASED);
    assertThat(queries.get(run.getId()).getStage()).isEqualTo(RunStage.RELEASED);

    LocalDate today = FrbsFixtures.today();
    assertThatThrownBy(
            () ->
                as.run(
                    FrbsFixtures.OFFICER,
                    () -> tags.liquidate(first.getId(), today, null, "report.pdf", new byte[0])))
        .hasMessageContaining("liquidation report");
    for (ServiceFeeLine line : fx.paidLines(run)) {
      as.run(
          FrbsFixtures.OFFICER,
          () -> tags.liquidate(line.getId(), today, "Received", "report.pdf", PDF));
    }
    assertThat(queries.get(run.getId()).getStage()).isEqualTo(RunStage.LIQUIDATED);
    assertThat(fx.paidLines(run).get(0).getLiquidationRef()).isNotNull();
  }

  @Test
  void theApproverIsNotThePreparerAndAReturnedLineIsSentAgain() {
    ServiceFeeRun run = fx.computedRun();
    as.run(FrbsFixtures.OFFICER, () -> runs.submit(run.getId(), null));
    assertThatThrownBy(() -> as.run(FrbsFixtures.OFFICER, () -> runs.approve(run.getId(), null)))
        .hasMessageContaining("other than its preparer");
    as.run(FrbsFixtures.LEAD, () -> runs.approve(run.getId(), null));
    ServiceFeeLine line = fx.paidLines(run).get(0);
    fx.returnLine(run, line);
    ServiceFeeLine returned =
        queries.lines(run.getId()).stream()
            .filter(l -> l.getId().equals(line.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(returned.getStatus()).isEqualTo(LineStatus.RETURNED);
    assertThat(returned.getGatewayMessage()).isEqualTo("Payee unknown");
    ServiceFeeLine resent = as.run(FrbsFixtures.OFFICER, () -> runs.resend(line.getId()));
    assertThat(resent.getStatus()).isEqualTo(LineStatus.SENT);
    assertThat(resent.getSendCount()).isEqualTo(2);
    assertThat(fx.request(run, resent).getSourceRef()).endsWith(":2");
    ServiceFeeLine released =
        as.run(FrbsFixtures.OFFICER, () -> tags.release(line.getId(), FrbsFixtures.today()));
    assertThat(released.getReleasedBy()).isEqualTo(FrbsFixtures.OFFICER);
    assertThatThrownBy(
            () ->
                as.run(
                    FrbsFixtures.OFFICER,
                    () -> tags.release(line.getId(), FrbsFixtures.today().plusDays(2))))
        .hasMessageContaining("future");
  }

  @Test
  void aComputedRunIsRecomputedOrCancelledAndFreesItsInvoices() {
    ServiceFeeRun run = fx.computedRun();
    int before = queries.items(run.getId()).size();
    String extra = fx.paidInvoice();
    ServiceFeeRun again = as.run(FrbsFixtures.OFFICER, () -> runs.recompute(run.getId()));
    assertThat(again.getInvoiceCount()).isGreaterThan(before - 1);
    assertThat(queries.items(run.getId())).extracting(ServiceFeeItem::getInvoiceNo).contains(extra);
    Long caseId =
        as.run(
            FrbsFixtures.OFFICER,
            () ->
                workflow
                    .transition(
                        "FrbsServiceFeeRun",
                        String.valueOf(run.getId()),
                        "cancel",
                        new TransitionNote("DUPLICATE", "Computed twice"))
                    .getId());
    assertThat(caseId).isNotNull();
    assertThat(queries.get(run.getId()).getStage()).isEqualTo(RunStage.CANCELLED);
    assertThat(queries.items(run.getId())).noneMatch(ServiceFeeItem::isLive);
    assertThat(queries.lines(run.getId())).allMatch(l -> l.getStatus() == LineStatus.CANCELLED);
    ServiceFeeRun next =
        as.run(
            FrbsFixtures.OFFICER,
            () -> runs.compute(fx.company(), FrbsFixtures.today(), FrbsFixtures.today()));
    assertThat(queries.items(next.getId()))
        .extracting(ServiceFeeItem::getInvoiceNo)
        .contains(extra);
    assertThatThrownBy(
            () ->
                as.run(
                    FrbsFixtures.OFFICER,
                    () ->
                        runs.compute(
                            fx.company(), FrbsFixtures.today(), FrbsFixtures.today().plusDays(3))))
        .hasMessageContaining("before today");
  }

  @Test
  void rulesAndRecipientsAreMaintained() {
    ServiceFeeRule rule =
        as.run(
            FrbsFixtures.LEAD,
            () ->
                setup.createRule(
                    new RuleValues(
                        "OTHERS",
                        List.of("INSTITUTIONAL"),
                        new BigDecimal("0.5"),
                        true,
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 12, 31),
                        false,
                        "Test")));
    assertThat(rule.getMarketSegments()).containsExactly("INSTITUTIONAL");
    ServiceFeeRule changed =
        as.run(
            FrbsFixtures.LEAD,
            () ->
                setup.updateRule(
                    rule.getId(),
                    new RuleValues(
                        "OTHERS",
                        List.of("INSTITUTIONAL"),
                        new BigDecimal("0.75"),
                        false,
                        LocalDate.of(2020, 1, 1),
                        LocalDate.of(2020, 6, 30),
                        false,
                        "Test")));
    assertThat(changed.getRate()).isEqualByComparingTo("0.75");
    assertThatThrownBy(
            () ->
                as.run(
                    FrbsFixtures.LEAD,
                    () ->
                        setup.createRule(
                            new RuleValues(
                                "OTHERS",
                                List.of("INSTITUTIONAL"),
                                new BigDecimal("1"),
                                true,
                                LocalDate.of(2020, 6, 1),
                                LocalDate.of(2020, 1, 1),
                                true,
                                null))))
        .hasMessageContaining("ends before");
    String unit = "TU" + com.iortatechnxt.brokerverse.booking.BookingFixtures.token();
    var recipient =
        as.run(
            FrbsFixtures.LEAD,
            () ->
                setup.createRecipient(
                    fx.company(), unit, new RecipientValues("BR-9", "Branch 9", "NB-CBG-M", true)));
    assertThat(recipient.getSalesUnit()).isEqualTo(unit.toUpperCase(java.util.Locale.ROOT));
    assertThat(
            as.run(
                    FrbsFixtures.LEAD,
                    () ->
                        setup.updateRecipient(
                            recipient.getId(),
                            new RecipientValues("BR-10", "Branch 10", null, false)))
                .getPayeeCode())
        .isEqualTo("BR-10");
    assertThatThrownBy(
            () ->
                as.run(
                    FrbsFixtures.LEAD,
                    () ->
                        setup.createRecipient(
                            fx.company(),
                            unit,
                            new RecipientValues("BR-9", "Branch 9", null, true))))
        .hasMessageContaining(unit.toUpperCase(java.util.Locale.ROOT));
  }
}
