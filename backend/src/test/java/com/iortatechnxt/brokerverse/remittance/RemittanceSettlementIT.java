package com.iortatechnxt.brokerverse.remittance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.IssueRequest;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.disbursement.DisbursementFixtures;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.service.RequestIntakeService;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherActions;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.FlowInHandler.FlowInFile;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRule;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.DeductionStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.Terms;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.IncentiveBasis;
import com.iortatechnxt.brokerverse.remittance.service.BatchService;
import com.iortatechnxt.brokerverse.remittance.service.DeductionService;
import com.iortatechnxt.brokerverse.remittance.service.IncentiveRuleService;
import com.iortatechnxt.brokerverse.remittance.service.InsurerOrUploads;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The BRD-5 settlement of remittance batches (A1-OPSX): CPC2 per line with its posting (DIS
 * 3.29.2), the early-incentive service invoice with 2% withholding tax (DIS 3.29.1), insurer
 * deductions consumed and capped (ACSL 2.9.2), and a cancelled DV that restores the batch so it is
 * sent again (DIS 2.20.0).
 */
@IntegrationTest
class RemittanceSettlementIT {

  private static final String REMIT = "remit";
  private static final String REMITTL = "remittl";
  private static final String INSURER = "INS-MGIC";
  private static final String MODULE = "REMITTANCE";

  @Autowired private RemittanceFixtures fx;
  @Autowired private DisbursementFixtures disbursement;
  @Autowired private BatchService batches;
  @Autowired private DeductionService deductions;
  @Autowired private IncentiveRuleService incentives;
  @Autowired private ServiceInvoiceService serviceInvoices;
  @Autowired private DisbursementQueueService queue;
  @Autowired private RequestIntakeService intake;
  @Autowired private VoucherActions vouchers;
  @Autowired private InsurerOrUploads orUploads;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private int events(String sourceReference) {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from acc_event_log where source_reference = ?",
            Integer.class,
            sourceReference);
    return n == null ? 0 : n;
  }

  private RemittanceBatch submitted(OpsInvoice paid) {
    fx.extract(paid.getInvoiceNo());
    Long id = fx.batchOf(paid.getInvoiceNo()).getId();
    return as.run(REMIT, () -> batches.submit(id, "Checked"));
  }

  @Test
  void packagedMotorLinesEarnCpc2PostedSeparatelyFromTheCommission() {
    OpsInvoice paid = fx.paidInvoice();
    fx.extract(paid.getInvoiceNo());
    BatchLine line = fx.lineOf(paid.getInvoiceNo());
    assertThat(line.getCpc2Code()).isEqualTo("CPC2");
    assertThat(line.getCpc2Rate()).isEqualByComparingTo("1");
    assertThat(line.getAmounts().cpc2())
        .isPositive()
        .isEqualByComparingTo(Money.round(line.getBasicPremium().movePointLeft(2)));
    assertThat(line.getAmounts().cpc2Vat()).isPositive();

    Long id = fx.batchOf(paid.getInvoiceNo()).getId();
    as.run(REMIT, () -> batches.submit(id, "Checked"));
    RemittanceBatch approved = as.run(REMITTL, () -> batches.approve(id, "OK"));
    assertThat(approved.getTotals().cpc2()).isEqualByComparingTo(line.getAmounts().cpc2());
    assertThat(events("RMB:" + approved.getBatchNo() + ":CPC2")).isEqualTo(1);
    DisbursementRequest request = queue.find(MODULE, approved.getBatchNo()).orElseThrow();
    assertThat(request.getAmount())
        .isEqualByComparingTo(line.getAmounts().netDue().subtract(line.getAmounts().cpc2Total()))
        .isEqualByComparingTo(approved.amountDue());
    assertThat(request.getRfpNo()).isEqualTo(approved.getBatchNo());
    assertThat(request.getPayeeClass()).isEqualTo("INSURER");
    assertThat(request.getRootInvoiceNo()).isEqualTo(paid.getRootInvoiceNo());
  }

  @Test
  void theEarlyIncentiveIsInvoicedAutomaticallyWithTwoPercentWithholdingTax() {
    EarlyIncentiveRule rule =
        as.run(
            REMITTL,
            () ->
                incentives.create(
                    fx.company(),
                    terms(true, "Test early incentive SI " + BookingFixtures.token())));
    try {
      OpsInvoice invoice = fx.invoice("RETAIL");
      fx.payInFull(invoice, RemittanceFixtures.PAID_ON);
      RemittanceBatch batch = fx.approvedBatch(fx.reload(invoice.getInvoiceNo()));
      BigDecimal incentive = batch.getTotals().incentive();
      assertThat(incentive).isPositive();
      String siNo = batch.getSettlement().getEarlySiNo();
      assertThat(siNo).startsWith("SI-");
      ServiceInvoice si = as.run(REMIT, () -> serviceInvoices.requireBySiNo(siNo));
      assertThat(si.getTypeCode()).isEqualTo("EARLY_INCENTIVE");
      assertThat(si.getRecipientCode()).isEqualTo(INSURER);
      assertThat(si.getCommission()).isEqualByComparingTo(incentive);
      assertThat(si.getVatOnCommission()).isEqualByComparingTo(batch.getTotals().incentiveVat());
      assertThat(si.getWtaxAmount())
          .isEqualByComparingTo(Money.round(incentive.multiply(new BigDecimal("0.02"))))
          .isEqualByComparingTo(batch.getSettlement().getEarlySiWtax());
      assertThat(batch.getIncentiveOrStatus()).isEqualTo("ISSUED");

      assertThatThrownBy(
              () ->
                  as.run(
                      REMITTL,
                      () ->
                          serviceInvoices.issueManual(
                              new IssueRequest(
                                  fx.company(),
                                  "EARLY_INCENTIVE",
                                  null,
                                  null,
                                  INSURER,
                                  null,
                                  null,
                                  "PHP",
                                  BigDecimal.TEN,
                                  BigDecimal.ONE,
                                  null,
                                  "By hand"))))
          .isInstanceOf(BusinessRuleException.class)
          .hasMessageContaining("automatically");
    } finally {
      as.run(REMITTL, () -> incentives.update(rule.getId(), terms(false, "Closed")));
    }
  }

  private static EarlyIncentiveRule.Terms terms(boolean active, String description) {
    return new EarlyIncentiveRule.Terms(
        INSURER,
        "MOTOR",
        "RETAIL",
        new BigDecimal("2"),
        366,
        IncentiveBasis.BOOKING,
        LocalDate.of(2026, 1, 1),
        null,
        active,
        description);
  }

  @Test
  void aConfirmedDeductionIsConsumedByTheNextBatchesAndCappedAtTheAmountPayable() {
    RemittanceBatch first = submitted(fx.paidInvoice());
    RemittanceBatch second = submitted(fx.paidInvoice());
    BigDecimal payable = first.getTotals().payable();
    BigDecimal amount = payable.add(BigDecimal.ONE);
    String source = "ARI-" + BookingFixtures.token();
    RemittanceDeduction draft =
        as.run(
            "acsl",
            () ->
                deductions.create(
                    fx.company(),
                    new Terms(
                        INSURER,
                        "PHP",
                        "AR_INSURER_REFUND",
                        source,
                        null,
                        amount,
                        null,
                        null,
                        "Return premium")));
    assertThat(draft.getDeductionNo()).startsWith("RDN-");
    assertThat(draft.getStage()).isEqualTo(DeductionStage.DRAFT);
    assertThatThrownBy(() -> as.run("acsl", () -> deductions.submit(draft.getId(), null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("confirmation");
    as.run(
        "acsl",
        () ->
            deductions.update(
                draft.getId(),
                new Terms(
                    INSURER,
                    "PHP",
                    "AR_INSURER_REFUND",
                    source,
                    null,
                    amount,
                    "INS-LTR-" + source,
                    LocalDate.of(2026, 9, 20),
                    "Return premium")));
    as.run("acsl", () -> deductions.submit(draft.getId(), "Insurer letter attached"));
    assertThatThrownBy(() -> as.run("acsl", () -> deductions.confirm(draft.getId(), null)))
        .isInstanceOf(BusinessRuleException.class);
    RemittanceDeduction confirmed =
        as.run("acsltl", () -> deductions.confirm(draft.getId(), "Confirmed by insurer"));
    assertThat(confirmed.getStage()).isEqualTo(DeductionStage.CONFIRMED);

    RemittanceBatch capped = as.run(REMITTL, () -> batches.approve(first.getId(), "OK"));
    assertThat(capped.getSettlement().getDeductionAmount()).isEqualByComparingTo(payable);
    assertThat(capped.amountDue()).isZero();
    assertThat(capped.getDisbursementRequestNo()).isNull();
    assertThat(capped.getStage()).isIn(BatchStage.FULLY_REMITTED, BatchStage.PARTIALLY_REMITTED);
    assertThat(events("RMB:" + first.getBatchNo() + ":" + draft.getDeductionNo())).isEqualTo(1);
    RemittanceDeduction partly = as.run("acsl", () -> deductions.get(draft.getId()));
    assertThat(partly.getStage()).isEqualTo(DeductionStage.CONFIRMED);
    assertThat(partly.remaining()).isEqualByComparingTo(BigDecimal.ONE);

    RemittanceBatch rest = as.run(REMITTL, () -> batches.approve(second.getId(), "OK"));
    assertThat(rest.getSettlement().getDeductionAmount()).isEqualByComparingTo(BigDecimal.ONE);
    DisbursementRequest request = queue.find(MODULE, rest.getBatchNo()).orElseThrow();
    assertThat(request.getAmount())
        .isEqualByComparingTo(rest.getTotals().payable().subtract(BigDecimal.ONE));
    RemittanceDeduction used = as.run("acsl", () -> deductions.get(draft.getId()));
    assertThat(used.getStage()).isEqualTo(DeductionStage.CONFIRMED);
    assertThat(used.remaining()).isZero();
    assertThat(as.run("acsl", () -> deductions.applications(draft.getId()))).hasSize(2);
    assertThat(
            as.run(REMIT, () -> deductions.pending(fx.company(), INSURER, "PHP")).stream()
                .map(RemittanceDeduction::getId))
        .doesNotContain(draft.getId());

    as.run(
        "disb",
        () -> tx.execute(s -> queue.assignDv(request.getId(), "DV-" + BookingFixtures.token())));
    insurerOr(capped);
    assertThat(as.run("acsl", () -> deductions.get(draft.getId())).getStage())
        .isEqualTo(DeductionStage.CONFIRMED);
    insurerOr(fx.batchOf(rest.getLines().get(0).getInvoiceNo()));
    assertThat(as.run("acsl", () -> deductions.get(draft.getId())).getStage())
        .isEqualTo(DeductionStage.APPLIED);
  }

  private void insurerOr(RemittanceBatch batch) {
    StringBuilder csv = new StringBuilder("batchNo,invoiceNo,orNo,orDate,orAmount\n");
    for (BatchLine line : batch.included()) {
      csv.append(batch.getBatchNo())
          .append(',')
          .append(line.getInvoiceNo())
          .append(",OR-")
          .append(BookingFixtures.token())
          .append(",2026-09-21,")
          .append(line.getAmounts().paidAr().toPlainString())
          .append('\n');
    }
    as.run(
        REMIT,
        () ->
            orUploads.upload(
                new FlowInFile("or.csv", csv.toString().getBytes(StandardCharsets.UTF_8))));
    assertThat(fx.batchOf(batch.getLines().get(0).getInvoiceNo()).getStage())
        .isEqualTo(BatchStage.OR_RECEIVED);
  }

  @Test
  void aCancelledDvRestoresTheBatchWhichIsSentAgainUnderANewReference() {
    disbursement.payee(INSURER, "INSURER", DisbursementMode.CHECK, null);
    OpsInvoice paid = fx.paidInvoice();
    RemittanceBatch batch = fx.approvedBatch(paid);
    String batchNo = batch.getBatchNo();
    Voucher auto =
        disbursement.voucherOf(disbursement.as("disb", () -> intake.byReference(MODULE, batchNo)));
    disbursement.as("disbappr", () -> vouchers.approve(auto.getId(), "OK"));
    assertThat(fx.batchOf(paid.getInvoiceNo()).getStage())
        .isIn(BatchStage.FULLY_REMITTED, BatchStage.PARTIALLY_REMITTED);

    disbursement.as(
        "disbappr", () -> vouchers.cancel(auto.getId(), "REQUESTED_BY_SOURCE", "Wrong amount"));

    RemittanceBatch restored = fx.batchOf(paid.getInvoiceNo());
    assertThat(restored.getStage()).isEqualTo(BatchStage.REVIEW_IN_PROCESS);
    assertThat(restored.getSettlement().getSendCycle()).isEqualTo(2);
    assertThat(restored.getSettlement().getCancelledDvNo()).isEqualTo(auto.getDvNo());
    assertThat(restored.getSettlement().getCancelReason()).contains("REQUESTED_BY_SOURCE");
    assertThat(restored.getDisbursementStatus()).isEqualTo("CANCELLED");
    assertThat(restored.cycleReference()).isEqualTo(batchNo + "/R2");
    assertThat(events("RMB:" + batchNo + ":" + paid.getInvoiceNo() + ":CANCEL")).isEqualTo(1);
    OpsInvoice back = fx.reload(paid.getInvoiceNo());
    assertThat(back.getRemittanceStatus()).isEqualTo(RemittanceStatus.REVIEW_IN_PROCESS);
    assertThat(back.getLockOwner()).isEqualTo(MODULE);
    assertThat(back.component(LedgerComponent.DTIP).getBalance()).isPositive();

    as.run(REMIT, () -> batches.submit(restored.getId(), "Send again"));
    RemittanceBatch again = as.run(REMITTL, () -> batches.approve(restored.getId(), "OK"));
    assertThat(again.getStage()).isEqualTo(BatchStage.APPROVED);
    DisbursementRequest second = queue.find(MODULE, batchNo + "/R2").orElseThrow();
    assertThat(second.getRequestNo()).isEqualTo(again.getDisbursementRequestNo());
    assertThat(second.getAmount()).isEqualByComparingTo(again.amountDue());
    assertThat(events("RMB:" + batchNo + "/R2:" + paid.getInvoiceNo())).isEqualTo(1);
    assertThat(fx.reload(paid.getInvoiceNo()).component(LedgerComponent.DTIP).getBalance())
        .isZero();
  }
}
