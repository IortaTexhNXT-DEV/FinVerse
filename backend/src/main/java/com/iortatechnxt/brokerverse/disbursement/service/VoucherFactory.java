package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher.VoucherFacts;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher.VoucherTerms;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Builds the disbursement voucher of a payment request (DIS 2.7.5, 3.25.0): the DV number {@code
 * DV-<yyyy>-nnnnnn}, the payee's default mode of payment, primary bank account and cost centre, the
 * first active paying account in the request currency, today's value date and the proforma entry
 * from the accounting rule. A voucher built automatically for a gateway refund or remittance goes
 * straight to the approver; any other voucher starts with the processor. It runs inside the
 * caller's transaction and never fails the source module: a proforma the rule cannot build is left
 * empty for the processor to complete (DIS 2.7.4).
 */
@Component
public class VoucherFactory {

  private static final Logger LOG = LoggerFactory.getLogger(VoucherFactory.class);

  private final VoucherRepository vouchers;
  private final ProformaBuilder proforma;
  private final BankAccountQueryService banks;
  private final DocumentNumberService numbers;
  private final WorkflowService workflow;
  private final GatewaySync gateway;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the factory.
   *
   * @param vouchers vouchers
   * @param proforma proforma entry
   * @param banks paying bank accounts
   * @param numbers DV numbers
   * @param workflow voucher workflow
   * @param gateway Operations request progress
   * @param audit audit trail
   * @param clock clock
   */
  public VoucherFactory(
      VoucherRepository vouchers,
      ProformaBuilder proforma,
      BankAccountQueryService banks,
      DocumentNumberService numbers,
      WorkflowService workflow,
      GatewaySync gateway,
      AuditTrailService audit,
      Clock clock) {
    this.vouchers = vouchers;
    this.proforma = proforma;
    this.banks = banks;
    this.numbers = numbers;
    this.workflow = workflow;
    this.gateway = gateway;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Creates the voucher of a matched request.
   *
   * @param request payment request (RECEIVED, payee matched)
   * @param payee payee
   * @param straight route to the approver at once (DIS 3.25.0)
   * @return the voucher
   */
  public Voucher create(IntakeRequest request, Payee payee, boolean straight) {
    LocalDate today = LocalDate.now(clock);
    Optional<BankAccount> paying =
        banks.listActive(request.getCompanyId(), request.getCurrency()).stream().findFirst();
    Voucher v =
        new Voucher(
            numbers.next(DisbursementSettings.series("DV", today)),
            new VoucherFacts(
                request.getCompanyId(),
                paying.map(BankAccount::getBranchId).orElse(null),
                request.getId(),
                payee.getId(),
                payee.getPayeeCode(),
                payee.getName(),
                payee.getPayeeClass(),
                request.getDisbursementType(),
                request.getCurrency(),
                request.getAmount(),
                request.getRootInvoiceNo(),
                straight));
    v.setTerms(termsOf(request, payee, paying, today));
    Voucher saved = vouchers.save(v);
    buildProforma(saved);
    request.inVoucher(saved.getId());
    payee.markUsed();
    startCase(saved, request, straight);
    gateway.track(request, saved.getStage().name(), null);
    audit.record(
        DisbursementSettings.VOUCHER,
        saved.getDvNo(),
        AuditAction.CREATE,
        "DV for "
            + request.getRequestNo()
            + " "
            + saved.getCurrency()
            + " "
            + saved.getGross()
            + " to "
            + saved.getPayeeCode()
            + (straight ? ", routed to the approver" : ""));
    return saved;
  }

  private static VoucherTerms termsOf(
      IntakeRequest request, Payee payee, Optional<BankAccount> paying, LocalDate today) {
    return new VoucherTerms(
        payee.getDefaultMode(),
        paying.map(BankAccount::getId).orElse(null),
        payee.primaryAccount().map(PayeeAccount::getId).orElse(null),
        BigDecimal.ZERO,
        request.getPurpose() == null
            ? "Payment request " + request.getRequestNo()
            : request.getPurpose(),
        today,
        request.getCostCenter() != null ? request.getCostCenter() : payee.getDefaultCostCenter(),
        request.getExpenseAccount());
  }

  private void startCase(Voucher saved, IntakeRequest request, boolean straight) {
    workflow.start(
        new StartCase(
            saved.getCompanyId(),
            DisbursementSettings.WF_VOUCHER,
            new CaseRecord(
                DisbursementSettings.VOUCHER,
                saved.getId().toString(),
                saved.getDvNo(),
                saved.getPayeeName() + " - " + saved.getDisbursementType(),
                DisbursementSettings.voucherLink(saved.getId()),
                request.getSourceModule()),
            null));
    if (straight) {
      workflow.systemTransition(
          DisbursementSettings.VOUCHER,
          saved.getId().toString(),
          "route_to_approver",
          TransitionNote.comment("Built automatically from " + request.getRequestNo()));
    }
  }

  private void buildProforma(Voucher v) {
    try {
      v.replaceLines(proforma.fromRule(v), false);
    } catch (BusinessRuleException ex) {
      LOG.info("DV {}: proforma left to the processor - {}", v.getDvNo(), ex.getMessage());
    }
  }
}
