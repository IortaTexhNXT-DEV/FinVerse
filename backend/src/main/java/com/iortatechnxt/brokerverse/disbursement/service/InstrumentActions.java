package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EventSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.RequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest.RequestFacts;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.service.InstrumentService.Change;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBook;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBookStatus;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.payables.service.BankAccountService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User actions on the instrument of an approved voucher (DIS 2.7.7-2.7.9, 2.8.1-2.8.4, 2.16.2,
 * 3.26.3-3.26.7): print the check (next leaf of the paying account's check series, with the alert
 * {@code CHECK_SERIES_LOW}) or the ATD / MC-DD / credit ticket / TT form (numbered), e-mail the ATD
 * to the processing branch with the requestor in copy, release a check or MC / DD, confirm a branch
 * or BOB debit, receive an MC / DD from the branch, and re-issue a stale check as a new request of
 * type STALE_REISSUE.
 */
@Service
@Transactional
public class InstrumentActions {

  private static final String STALE_REISSUE = "STALE_REISSUE";
  private static final String SERIES_LOW = "CHECK_SERIES_LOW";
  private static final int DEFAULT_WARNING = 20;

  private final InstrumentService instruments;
  private final DisbursementForms forms;
  private final FormFactsReader facts;
  private final BankAccountService bankService;
  private final BankAccountQueryService banks;
  private final DocumentNumberService numbers;
  private final MessageService messages;
  private final AlertService alerts;
  private final SystemParameterService parameters;
  private final RequestIntakeService intake;
  private final Clock clock;

  /**
   * Creates the actions.
   *
   * @param instruments instrument life cycle
   * @param forms printed forms
   * @param facts form facts
   * @param bankService check leaves
   * @param banks bank accounts and check books
   * @param numbers form numbers
   * @param messages e-mail
   * @param alerts alerts
   * @param parameters business parameters
   * @param intake re-issue requests
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public InstrumentActions(
      InstrumentService instruments,
      DisbursementForms forms,
      FormFactsReader facts,
      BankAccountService bankService,
      BankAccountQueryService banks,
      DocumentNumberService numbers,
      MessageService messages,
      AlertService alerts,
      SystemParameterService parameters,
      RequestIntakeService intake,
      Clock clock) {
    this.instruments = instruments;
    this.forms = forms;
    this.facts = facts;
    this.bankService = bankService;
    this.banks = banks;
    this.numbers = numbers;
    this.messages = messages;
    this.alerts = alerts;
    this.parameters = parameters;
    this.intake = intake;
    this.clock = clock;
  }

  /**
   * Prints the check or form of a voucher (PENDING -> PRINTED, DIS 2.16.2, 3.26.3, 3.26.5-3.26.6).
   *
   * @param voucherId voucher
   * @param source who prints it
   * @return instrument
   */
  public Instrument print(Long voucherId, Change source) {
    Instrument i = instruments.forVoucher(voucherId);
    if (!DisbursementForms.hasForm(i.getMode())) {
      throw new BusinessRuleException(
          "DISB_NO_FORM", i.getMode() + " has no printed form; it is processed by file");
    }
    LocalDate today = LocalDate.now(clock);
    String number =
        i.getMode() == DisbursementMode.CHECK
            ? checkNo(i)
            : numbers.next(
                DisbursementSettings.series(i.getMode().name().replace('_', '-'), today));
    i.numbered(number, today);
    return instruments.advance(i, InstrumentStatus.PRINTED, source);
  }

  private String checkNo(Instrument i) {
    String number = bankService.nextChequeNo(i.getBankAccountId());
    long remaining =
        banks.chequeBooks(i.getBankAccountId()).stream()
            .filter(b -> b.getStatus() == ChequeBookStatus.ACTIVE)
            .mapToLong(ChequeBook::remaining)
            .sum();
    int warning = parameters.intValue(DisbursementSettings.CHECK_SERIES_WARNING, DEFAULT_WARNING);
    if (remaining <= warning) {
      var bank = banks.get(i.getBankAccountId());
      alerts.raise(
          SERIES_LOW,
          new AlertFacts(
              bank.getCompanyId(),
              bank.getBranchId(),
              "BankAccount",
              bank.getCode(),
              "Bank account " + bank.getCode() + " has " + remaining + " check leaves left",
              null,
              SERIES_LOW + ":" + bank.getCode()));
    }
    return number;
  }

  /**
   * The printed form of a voucher's instrument.
   *
   * @param voucherId voucher
   * @return PDF
   */
  @Transactional(readOnly = true)
  public byte[] document(Long voucherId) {
    Instrument i = instruments.forVoucher(voucherId);
    Voucher v = instruments.voucherOf(i);
    return forms.instrument(v, i, facts.of(v));
  }

  /**
   * Releases a check or MC / DD to the payee (DIS 2.8.1, 2.8.4).
   *
   * @param voucherId voucher
   * @param releasedTo who received it
   * @return instrument
   */
  public Instrument release(Long voucherId, String releasedTo) {
    Instrument i = instruments.forVoucher(voucherId);
    i.releasedTo(releasedTo);
    return instruments.advance(
        i, InstrumentStatus.RELEASED, Change.user("Released to " + releasedTo));
  }

  /**
   * E-mails the ATD to the processing branch with the requestor in copy (DIS 2.7.7, 2.8.2); an ATD
   * not yet printed is printed first.
   *
   * @param voucherId voucher
   * @param to branch mailbox(es) (LOV {@code BRANCH_EMAIL})
   * @param cc copies (requestor)
   * @return instrument
   */
  public Instrument emailAtd(Long voucherId, List<String> to, List<String> cc) {
    Instrument i = instruments.forVoucher(voucherId);
    if (i.getMode() != DisbursementMode.ATD) {
      throw new BusinessRuleException("DISB_NOT_ATD", "Only an authority to debit is e-mailed");
    }
    if (i.getStatus() == InstrumentStatus.PENDING) {
      print(voucherId, new Change(EventSource.SYSTEM, "Printed for e-mail", null));
    }
    Voucher v = instruments.voucherOf(i);
    MergedText mail = forms.atdEmail(v, i);
    messages.queueEmail(
        new OutboundEmail(
            v.getCompanyId(),
            "DISB_ATD",
            to,
            cc,
            mail.title(),
            mail.text(),
            List.of(
                new MessageFile(
                    i.getInstrumentNo() + ".pdf",
                    DisbursementForms.PDF,
                    forms.instrument(v, i, facts.of(v)))),
            null,
            new RecordLink(DisbursementSettings.VOUCHER, v.getId().toString(), v.getDvNo())));
    return instruments.advance(
        i, InstrumentStatus.EMAILED, Change.user("E-mailed to " + String.join(", ", to)));
  }

  /**
   * The branch or BOB confirmed the debit (DIS 2.8.2-2.8.3, 3.26.7).
   *
   * @param voucherId voucher
   * @param reference branch or BOB reference, may be null
   * @return instrument
   */
  public Instrument confirmDebit(Long voucherId, String reference) {
    Instrument i = instruments.forVoucher(voucherId);
    if (reference != null && !reference.isBlank()) {
      i.referenced(reference.strip());
    }
    return instruments.advance(
        i, InstrumentStatus.DEBITED, Change.user("Debit confirmed " + nz(reference)));
  }

  /**
   * The MC / DD was received from the branch (DIS 2.8.4).
   *
   * @param voucherId voucher
   * @param reference MC / DD number, may be null
   * @return instrument
   */
  public Instrument receive(Long voucherId, String reference) {
    Instrument i = instruments.forVoucher(voucherId);
    if (reference != null && !reference.isBlank()) {
      i.referenced(reference.strip());
    }
    return instruments.advance(
        i, InstrumentStatus.RECEIVED, Change.user("Received from the branch " + nz(reference)));
  }

  /**
   * Re-issues a stale check (design 6 row 10): a new request of type STALE_REISSUE for the same
   * payee and amount, whose voucher debits Miscellaneous Liability - stale checks.
   *
   * @param voucherId voucher of the stale check
   * @return the new request
   */
  public IntakeRequest reissue(Long voucherId) {
    Instrument i = instruments.forVoucher(voucherId);
    if (i.getStatus() != InstrumentStatus.STALE) {
      throw new BusinessRuleException("CHECK_NOT_STALE", "Only a stale check is re-issued");
    }
    Voucher v = instruments.voucherOf(i);
    return intake.register(
        new RequestFacts(
            v.getCompanyId(),
            RequestSource.ENCODED,
            DisbursementSettings.MODULE,
            "REISSUE:" + i.getId(),
            null,
            STALE_REISSUE,
            v.getPayeeClass(),
            v.getPayeeCode(),
            v.getPayeeName(),
            v.getCurrency(),
            i.getAmount(),
            "Re-issue of stale check " + i.label() + " of DV " + v.getDvNo(),
            v.getRootInvoiceNo(),
            List.of(),
            List.of(),
            false,
            null,
            null,
            null,
            v.getCostCenter()));
  }

  private static String nz(String text) {
    return text == null ? "" : text;
  }
}
