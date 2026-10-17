package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EodStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Payment confirmations of an end-of-day run (DIS 2.7.12): each payee with an e-mail address gets
 * the payment advice of its voucher with the DV attached. The remittance schedule of a remittance
 * DV is sent by remittance itself ({@code ScheduleDispatch}); attaching it here waits for BDOI's
 * confirmation of the recipients (AQ09). A run is confirmed once.
 */
@Component
public class EodConfirmations {

  private final VoucherRepository vouchers;
  private final InstrumentRepository instruments;
  private final PayeeRepository payees;
  private final DisbursementForms forms;
  private final FormFactsReader facts;
  private final MessageService messages;
  private final AuditTrailService audit;

  /**
   * Creates the helper.
   *
   * @param vouchers vouchers
   * @param instruments instruments
   * @param payees payees (e-mail)
   * @param forms advice text and DV
   * @param facts form facts
   * @param messages e-mail
   * @param audit audit trail
   */
  public EodConfirmations(
      VoucherRepository vouchers,
      InstrumentRepository instruments,
      PayeeRepository payees,
      DisbursementForms forms,
      FormFactsReader facts,
      MessageService messages,
      AuditTrailService audit) {
    this.vouchers = vouchers;
    this.instruments = instruments;
    this.payees = payees;
    this.forms = forms;
    this.facts = facts;
    this.messages = messages;
    this.audit = audit;
  }

  /**
   * E-mails the payment advices of a run.
   *
   * @param run end-of-day run
   * @return e-mails queued
   */
  public int confirm(EodRun run) {
    if (run.getStatus() == EodStatus.CONFIRMED) {
      throw new BusinessRuleException(
          "EOD_CONFIRMED", "The confirmations of " + run.getRunNo() + " were already sent");
    }
    int sent = 0;
    for (Voucher v : vouchers.findByEodRunIdOrderByIdAsc(run.getId())) {
      Optional<String> email =
          payees.findById(v.getPayeeId()).map(Payee::getEmail).filter(e -> !e.isBlank());
      if (email.isPresent()) {
        send(v, email.get());
        sent++;
      }
    }
    run.confirmed(sent);
    audit.record(
        DisbursementSettings.MODULE,
        run.getRunNo(),
        AuditAction.UPDATE,
        sent + " payment confirmation(s) e-mailed");
    return sent;
  }

  private void send(Voucher v, String email) {
    Voucher full = vouchers.findWithLinesById(v.getId()).orElse(v);
    MergedText advice = forms.advice(full, instruments.findByVoucherId(v.getId()).orElse(null));
    messages.queueEmail(
        new OutboundEmail(
            v.getCompanyId(),
            "DISB_PAYMENT_ADVICE",
            List.of(email),
            List.of(),
            advice.title(),
            advice.text(),
            List.of(
                new MessageFile(
                    v.getDvNo() + ".pdf",
                    DisbursementForms.PDF,
                    forms.voucher(full, facts.of(full)))),
            null,
            new RecordLink(DisbursementSettings.VOUCHER, v.getId().toString(), v.getDvNo())));
  }
}
