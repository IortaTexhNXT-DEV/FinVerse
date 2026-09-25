package com.iortatechnxt.brokerverse.payrequest;

import static com.iortatechnxt.brokerverse.payrequest.PayRequestFixtures.HR;
import static com.iortatechnxt.brokerverse.payrequest.PayRequestFixtures.PROCESSOR;
import static com.iortatechnxt.brokerverse.payrequest.PayRequestFixtures.REVIEWER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.PayoutDetails;
import com.iortatechnxt.brokerverse.crm.domain.PayoutMode;
import com.iortatechnxt.brokerverse.crm.service.ClientPayoutAccounts;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.payrequest.domain.ExpenseValues;
import com.iortatechnxt.brokerverse.payrequest.domain.Liquidation;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationStatus;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.service.LiquidationAccounts;
import com.iortatechnxt.brokerverse.payrequest.service.LiquidationService;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestDocuments;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestWorkflowService;
import com.iortatechnxt.brokerverse.payrequest.service.RequestFormService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Cash-advance liquidation (Appendix D, AQ18) posted with event PRQ_CA_LIQUIDATION, the request
 * forms, the request reports (MKT 1.18.1) and the CA / SA information on the client (MKT 2.25.x).
 */
@IntegrationTest
class LiquidationAndReportsIT {

  @Autowired private PayRequestFixtures fx;
  @Autowired private RequestFormService forms;
  @Autowired private PayRequestWorkflowService workflow;
  @Autowired private LiquidationService liquidations;
  @Autowired private LiquidationAccounts accounts;
  @Autowired private PayRequestDocuments documents;
  @Autowired private ClientPayoutAccounts payouts;
  @Autowired private JournalBatchRepository journals;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private PaymentRequest disbursedAdvance(String amount) {
    PaymentRequest ca =
        as.run(
            PROCESSOR,
            () -> forms.createCashAdvance(fx.company(), PayRequestFixtures.cashAdvance(amount)));
    fx.approve(ca);
    as.run(HR, () -> workflow.approve(ca.getId(), null));
    fx.pay(ca, "DV-L-" + ca.getId());
    PaymentRequest paid = fx.reload(ca);
    assertThat(paid.getStage()).isEqualTo(RequestStage.DISBURSED);
    return paid;
  }

  private void configureAccounts() {
    as.run(
        "fmanager",
        () -> {
          for (String role :
              List.of("PER_DIEM", "REPRESENTATION", "TRANSPORT", "LODGING", "OTHER")) {
            accounts.assign(fx.company(), role, "5606");
          }
          return accounts.assign(fx.company(), "CASH", "1101");
        });
  }

  private static ExpenseValues day(String perDiem, String transport) {
    return new ExpenseValues(
        LocalDate.now(),
        "Client visit",
        new BigDecimal(perDiem),
        BigDecimal.ZERO,
        new BigDecimal(transport),
        BigDecimal.ZERO,
        null);
  }

  @Test
  void aDisbursedCashAdvanceIsLiquidatedAndPosted() {
    configureAccounts();
    PaymentRequest ca = disbursedAdvance("1000.00");
    Liquidation draft =
        as.run(
            PROCESSOR,
            () ->
                liquidations.save(
                    ca.getId(),
                    new LiquidationService.Draft(
                        "Officer",
                        "MKT",
                        null,
                        List.of(day("300.00", "200.00"), day("250.00", "0")))));
    assertThat(draft.getLiquidationNo()).startsWith("LIQ-");
    assertThat(draft.getTotalExpenses()).isEqualByComparingTo("750.00");
    assertThat(draft.getOverShort()).isEqualByComparingTo("250.00");
    as.run(PROCESSOR, () -> liquidations.submit(ca.getId()));
    assertThatThrownBy(() -> as.run(PROCESSOR, () -> liquidations.post(ca.getId())))
        .isInstanceOf(BusinessRuleException.class);
    Liquidation posted = as.run(REVIEWER, () -> liquidations.post(ca.getId()));
    assertThat(posted.getStatus()).isEqualTo(LiquidationStatus.POSTED);
    var batch =
        journals.findByCompanyIdAndBatchNo(fx.company(), posted.getJournalBatchNo()).orElseThrow();
    assertThat(batch.getTotalDebit()).isEqualByComparingTo("1000.00");
    assertThat(documents.liquidationForm(ca.getId()).content()).isNotEmpty();
    assertThat(documents.form(ca.getId()).fileName()).endsWith(".pdf");
  }

  @Test
  void aShortLiquidationIsReturnedAndCannotStartBeforePayment() {
    configureAccounts();
    PaymentRequest notPaid =
        as.run(
            PROCESSOR,
            () -> forms.createCashAdvance(fx.company(), PayRequestFixtures.cashAdvance("100.00")));
    assertThatThrownBy(
            () ->
                as.run(
                    PROCESSOR,
                    () ->
                        liquidations.save(
                            notPaid.getId(),
                            new LiquidationService.Draft(null, "MKT", null, List.of()))))
        .isInstanceOf(BusinessRuleException.class);
    PaymentRequest ca = disbursedAdvance("100.00");
    as.run(
        PROCESSOR,
        () ->
            liquidations.save(
                ca.getId(),
                new LiquidationService.Draft(null, "MKT", null, List.of(day("150.00", "0")))));
    as.run(PROCESSOR, () -> liquidations.submit(ca.getId()));
    Liquidation back = as.run(REVIEWER, () -> liquidations.sendBack(ca.getId(), "Attach receipts"));
    assertThat(back.getStatus()).isEqualTo(LiquidationStatus.DRAFT);
    as.run(PROCESSOR, () -> liquidations.submit(ca.getId()));
    Liquidation posted = as.run(REVIEWER, () -> liquidations.post(ca.getId()));
    assertThat(posted.getOverShort()).isEqualByComparingTo("-50.00");
  }

  @Test
  void theRequestReportsAndFormsRunAndExport() {
    OpsInvoice invoice = fx.invoice();
    PaymentRequest r =
        fx.raise(
            PayRequestFixtures.refund(PayRequestFixtures.line(invoice, "OVERPAYMENT", "90.00")));
    assertThat(documents.form(r.getId()).content()).isNotEmpty();
    Map<String, String> params =
        Map.of(
            "companyId", String.valueOf(fx.company()),
            "from", LocalDate.now().minusDays(1).toString(),
            "to", LocalDate.now().plusDays(1).toString(),
            "kind", "ALL");
    String csv =
        as.run(
            REVIEWER,
            () -> {
              for (String code : List.of("PRQ-STATUS", "PRQ-REGISTER")) {
                assertThat(reports.run(code, params).code()).isEqualTo(code);
              }
              return new String(
                  reports.export("PRQ-REGISTER", params, ExportFormat.CSV).content(),
                  StandardCharsets.UTF_8);
            });
    assertThat(csv).contains(r.getRequestNo());
    assertThat(
            as.run(
                    REVIEWER,
                    () ->
                        reports.run(
                            "PRQ-STATUS",
                            Map.of(
                                "companyId", String.valueOf(fx.company()),
                                "from", LocalDate.now().toString(),
                                "to", LocalDate.now().toString(),
                                "kind", "CASH_ADVANCE")))
                .code())
        .isEqualTo("PRQ-STATUS");
  }

  @Test
  void payoutAccountsAreAddedOnceAndValidated() {
    OpsInvoice invoice = fx.invoice();
    String account = "9876543210" + (System.nanoTime() % 100);
    PayoutDetails cta = new PayoutDetails(PayoutMode.CTA, "Juan Dela Cruz", account);
    var first =
        as.run(
            PROCESSOR,
            () -> payouts.record(fx.company(), invoice.getClientCode(), cta, "TEST", "T-1"));
    var second =
        as.run(
            PROCESSOR,
            () -> payouts.record(fx.company(), invoice.getClientCode(), cta, "TEST", "T-2"));
    assertThat(first.created()).isTrue();
    assertThat(second.created()).isFalse();
    assertThat(second.account().getId()).isEqualTo(first.account().getId());
    assertThatThrownBy(() -> payouts.validate(new PayoutDetails(PayoutMode.CTA, "Juan", "12AB")))
        .isInstanceOf(BusinessRuleException.class);
    var deactivated =
        as.run(
            "mktcoll",
            () -> payouts.deactivate(first.account().getClientId(), first.account().getId()));
    assertThat(deactivated.isActive()).isFalse();
  }
}
