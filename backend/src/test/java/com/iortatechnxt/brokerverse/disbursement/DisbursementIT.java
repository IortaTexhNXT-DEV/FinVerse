package com.iortatechnxt.brokerverse.disbursement;

import static com.iortatechnxt.brokerverse.disbursement.DisbursementFixtures.APPROVER;
import static com.iortatechnxt.brokerverse.disbursement.DisbursementFixtures.LEADER;
import static com.iortatechnxt.brokerverse.disbursement.DisbursementFixtures.PROCESSOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.OutputKind;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PostingStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutput;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.service.CancellationHandoffs;
import com.iortatechnxt.brokerverse.disbursement.service.EodService;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentActions;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService.Change;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentUploads;
import com.iortatechnxt.brokerverse.disbursement.service.RequestIntakeService;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherActions;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest.Spec;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest.Status;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.HandoffService;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway.DisbursementTicket;
import com.iortatechnxt.brokerverse.remittance.RemittanceFixtures;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The Disbursement exit criteria (ACCOUNTING_DISBURSEMENT_DESIGN 14, A1-DSB): a remittance batch
 * becomes an automatic DV for approval that is approved (posted), printed and negotiated; a refund
 * DV is paid by credit to account through the end of day; cancelling an approved DV reverses it and
 * tells the source; a request without payee waits for it (DIS 2.2-3.28).
 */
@IntegrationTest
class DisbursementIT {

  @Autowired private DisbursementFixtures fx;
  @Autowired private RemittanceFixtures remittance;
  @Autowired private RequestIntakeService intake;
  @Autowired private VoucherActions actions;
  @Autowired private InstrumentActions instrumentActions;
  @Autowired private InstrumentService instruments;
  @Autowired private InstrumentUploads uploads;
  @Autowired private EodService eod;
  @Autowired private DisbursementQueueService queue;
  @Autowired private DisbursementGateway gateway;
  @Autowired private SystemParameterService parameters;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private HandoffService handoffs;

  private int events(String sourceReference) {
    Integer n =
        jdbc.queryForObject(
            "select count(*) from acc_event_log where source_reference = ? and status = 'POSTED'",
            Integer.class,
            sourceReference);
    return n == null ? 0 : n;
  }

  private RemittanceBatch approvedRemittanceBatch() {
    fx.payee(DisbursementFixtures.INSURER, "INSURER", DisbursementMode.CHECK, null);
    OpsInvoice paid = remittance.paidInvoice();
    invoiceNo = paid.getInvoiceNo();
    return remittance.approvedBatch(paid);
  }

  private String invoiceNo;

  @Test
  void aRemittanceBatchBecomesAVoucherThatIsApprovedPrintedAndNegotiated() {
    RemittanceBatch batch = approvedRemittanceBatch();
    IntakeRequest request =
        fx.as(PROCESSOR, () -> intake.byReference("REMITTANCE", batch.getBatchNo()));
    assertThat(request.getStatus()).isEqualTo(RequestStatus.IN_VOUCHER);
    Voucher auto = fx.voucherOf(request);
    assertThat(auto.getStage()).isEqualTo(VoucherStage.FOR_APPROVAL);
    assertThat(auto.isAutoCreated()).isTrue();
    assertThat(auto.getGross()).isEqualByComparingTo(batch.getTotals().payable());
    assertThat(auto.getLines()).isNotEmpty();
    assertThat(auto.debits()).isEqualByComparingTo(auto.credits());
    DisbursementRequest queued = queue.find("REMITTANCE", batch.getBatchNo()).orElseThrow();
    assertThat(queued.getDvStatus()).isEqualTo("FOR_APPROVAL");

    Voucher approved = fx.as(APPROVER, () -> actions.approve(auto.getId(), "OK"));
    assertThat(approved.getStage()).isEqualTo(VoucherStage.APPROVED);
    assertThat(approved.getPostingStatus()).isEqualTo(PostingStatus.POSTED);
    assertThat(approved.getJournalNo()).isNotBlank();
    assertThat(events("DV:" + approved.getDvNo())).isEqualTo(1);
    DisbursementRequest assigned = queue.get(queued.getId());
    assertThat(assigned.getStatus()).isEqualTo(Status.DV_ASSIGNED);
    assertThat(assigned.getDvNo()).isEqualTo(approved.getDvNo());
    assertThat(remittance.batchOf(invoiceNo).getStage())
        .isIn(BatchStage.FULLY_REMITTED, BatchStage.PARTIALLY_REMITTED);

    Instrument printed =
        fx.as(PROCESSOR, () -> instrumentActions.print(approved.getId(), Change.user("Print")));
    assertThat(printed.getStatus()).isEqualTo(InstrumentStatus.PRINTED);
    assertThat(printed.getInstrumentNo()).isNotBlank();
    fx.as(PROCESSOR, () -> instrumentActions.release(approved.getId(), "Messenger"));
    assertThat(queue.get(queued.getId()).getStatus()).isEqualTo(Status.PAID);

    Instrument negotiated =
        fx.as(
            PROCESSOR,
            () ->
                uploads.negotiated(
                    printed.getInstrumentNo(), printed.getAmount(), "Deposited", "T"));
    assertThat(negotiated.getStatus()).isEqualTo(InstrumentStatus.NEGOTIATED);
    assertThat(events("CHK:" + printed.getId() + ":NEG")).isEqualTo(1);
    assertThat(fx.as(PROCESSOR, () -> instruments.history(printed.getId())))
        .extracting(e -> e.getToStatus())
        .containsSubsequence(
            InstrumentStatus.PENDING,
            InstrumentStatus.PRINTED,
            InstrumentStatus.RELEASED,
            InstrumentStatus.NEGOTIATED);
    assertThat(queue.get(queued.getId()).getInstrumentStatus()).isEqualTo("NEGOTIATED");
  }

  @Test
  void cancellingAnApprovedVoucherReversesItAndTellsTheSource() {
    RemittanceBatch batch = approvedRemittanceBatch();
    Voucher auto =
        fx.voucherOf(fx.as(PROCESSOR, () -> intake.byReference("REMITTANCE", batch.getBatchNo())));
    fx.as(APPROVER, () -> actions.approve(auto.getId(), "OK"));
    assertThatThrownBy(
            () -> fx.as(PROCESSOR, () -> actions.cancel(auto.getId(), "DUPLICATE", null)))
        .isInstanceOf(BusinessRuleException.class);

    OpsHandoff handoff =
        fx.as(
            PROCESSOR,
            () ->
                handoffs.record(
                    auto.getCompanyId(),
                    CancellationHandoffs.PORT,
                    "DISB_APPROVE",
                    new OpsHandoff.Spec(
                        "PAYREQUEST",
                        "CKC-" + auto.getDvNo(),
                        auto.getDvNo(),
                        auto.getNet(),
                        auto.getCurrency(),
                        "Cancel DV " + auto.getDvNo(),
                        null)));

    Voucher cancelled =
        fx.as(APPROVER, () -> actions.cancel(auto.getId(), "REQUESTED_BY_SOURCE", "Wrong batch"));
    assertThat(cancelled.getStage()).isEqualTo(VoucherStage.CANCELLED);
    assertThat(cancelled.getPostingStatus()).isEqualTo(PostingStatus.REVERSED);
    assertThat(cancelled.getCancelJournalNo()).isNotBlank();
    assertThat(events("DV:" + cancelled.getDvNo() + ":CANCEL")).isEqualTo(1);
    DisbursementRequest queued = queue.find("REMITTANCE", batch.getBatchNo()).orElseThrow();
    assertThat(queued.getStatus()).isEqualTo(Status.CANCELLED);
    assertThat(queued.getCancelReason()).contains("REQUESTED_BY_SOURCE");
    assertThat(fx.as(PROCESSOR, () -> intake.get(auto.getRequestId())).getStatus())
        .isEqualTo(RequestStatus.CANCELLED);
    assertThat(fx.as(PROCESSOR, () -> instruments.forVoucher(auto.getId())).getStatus())
        .isEqualTo(InstrumentStatus.CANCELLED);
    assertThat(
            jdbc.queryForObject(
                "select status from ops_handoff where id = ?", String.class, handoff.getId()))
        .isEqualTo("CLOSED");
  }

  @Test
  void aRefundIsPaidByCreditToAccountThroughTheEndOfDay() {
    Payee client = fx.clientPayee();
    Voucher draft = fx.voucherOf(fx.encoded("REFUND", client, "2500.00"));
    assertThat(draft.getStage()).isEqualTo(VoucherStage.IN_PROCESS);
    assertThat(draft.getMode()).isEqualTo(DisbursementMode.CTA);
    fx.as(PROCESSOR, () -> actions.submit(draft.getId(), "Complete"));
    assertThatThrownBy(() -> fx.as(PROCESSOR, () -> actions.submitForApproval(draft.getId(), null)))
        .isInstanceOf(BusinessRuleException.class);
    fx.as(LEADER, () -> actions.submitForApproval(draft.getId(), "Checked"));
    assertThatThrownBy(() -> fx.as(LEADER, () -> actions.approve(draft.getId(), null)))
        .isInstanceOf(RuntimeException.class);
    Voucher approved = fx.as(APPROVER, () -> actions.approve(draft.getId(), "OK"));
    assertThat(fx.as(PROCESSOR, () -> instruments.forVoucher(approved.getId())).getStatus())
        .isEqualTo(InstrumentStatus.PENDING);

    EodRun run = fx.as(LEADER, () -> eod.run(fx.company(), DisbursementFixtures.uniqueDate()));
    assertThat(run.getCredits()).isPositive();
    List<EodOutput> outputs = fx.as(LEADER, () -> eod.outputs(run.getId()));
    EodOutput dctf =
        outputs.stream().filter(o -> o.getKind() == OutputKind.DCTF).findFirst().orElseThrow();
    assertThat(new String(dctf.getContent(), StandardCharsets.US_ASCII))
        .contains(approved.getDvNo())
        .contains("000000002500.00");
    assertThat(outputs)
        .extracting(EodOutput::getKind)
        .contains(OutputKind.VOUCHERS, OutputKind.REPORT);
    assertThat(fx.voucher(approved.getId()).getEodRunId()).isEqualTo(run.getId());
    assertThatThrownBy(() -> fx.as(LEADER, () -> eod.run(fx.company(), run.getBusinessDate())))
        .isInstanceOf(BusinessRuleException.class);

    Instrument credited =
        fx.as(
            PROCESSOR, () -> uploads.credited(approved.getDvNo(), new BigDecimal("2500.00"), "T"));
    assertThat(credited.getStatus()).isEqualTo(InstrumentStatus.CREDITED);
    assertThat(fx.as(LEADER, () -> eod.confirm(run.getId())).getEmails()).isPositive();
  }

  @Test
  void aRequestWithoutPayeeWaitsForItAndContinuesOnceAuthorised() {
    String code = "NOP-" + BookingFixtures.token();
    DisbursementTicket ticket = send(code);
    assertThat(ticket.status()).isEqualTo(Status.SENT);
    IntakeRequest waiting = fx.as(PROCESSOR, () -> intake.byReference("TESTMOD", "REF-" + code));
    assertThat(waiting.getStatus()).isEqualTo(RequestStatus.NO_PAYEE);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from dsb_payee_request where payee_code = ? and status = 'OPEN'",
                Integer.class,
                code))
        .isEqualTo(1);

    fx.payee(code, "CLIENT", DisbursementMode.CTA, "009988776655");
    IntakeRequest resumed = fx.as(PROCESSOR, () -> intake.get(waiting.getId()));
    assertThat(resumed.getStatus()).isEqualTo(RequestStatus.IN_VOUCHER);
    assertThat(fx.voucherOf(resumed).getStage()).isEqualTo(VoucherStage.FOR_APPROVAL);

    fx.as("admin", () -> parameters.update("DISB_NO_PAYEE_ACTION", "RETURN"));
    try {
      String other = "NOP-" + BookingFixtures.token();
      assertThat(send(other).status()).isEqualTo(Status.RETURNED);
      assertThat(send(other).message()).contains("PAYEE_NOT_MAINTAINED");
    } finally {
      fx.as("admin", () -> parameters.update("DISB_NO_PAYEE_ACTION", "HOLD"));
    }
  }

  private DisbursementTicket send(String payeeCode) {
    return fx.as(
        "cashier",
        () ->
            gateway.send(
                fx.company(),
                new Spec(
                    DisbursementRequest.Type.REFUND,
                    "TESTMOD",
                    "REF-" + payeeCode,
                    payeeCode,
                    null,
                    "PHP",
                    new BigDecimal("750.00"),
                    "Refund test",
                    null)));
  }
}
