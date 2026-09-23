package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.finverse.accounting.service.BusinessEvent;
import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.payables.domain.BankAccount;
import com.iortatechnxt.finverse.payables.domain.IssuedPdc;
import com.iortatechnxt.finverse.payables.domain.IssuedPdcEvent;
import com.iortatechnxt.finverse.payables.domain.IssuedPdcEventRepository;
import com.iortatechnxt.finverse.payables.domain.IssuedPdcRepository;
import com.iortatechnxt.finverse.payables.domain.IssuedPdcStatus;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucherRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Register of post-dated cheques issued, and their accounting.
 *
 * <p>Accounting treatment (demo rules in V950; configurable per company):
 *
 * <ol>
 *   <li><b>Issue</b> (approval of a PDC payment voucher, voucher date): the party's payment event
 *       with role BANK = the bank account's PDC clearing account: <i>Dr party payable / Cr PDC
 *       issued clearing (2511)</i>. The supplier is settled in the sub-ledger; the company still
 *       owes the amount to whoever presents the cheque, so the liability sits in PDC clearing until
 *       maturity and the bank balance is not reduced before the cheque date.
 *   <li><b>Due</b>: cheque date reached (daily job or on request) — status only, no posting.
 *   <li><b>Presented</b> (confirmation on or after the cheque date): {@code PDC_ISSUED_PRESENTED}:
 *       <i>Dr PDC issued clearing / Cr bank</i>, dated the presentation (bank) date.
 *   <li><b>Cancelled</b> (stopped before presentation): reversal of the issue posting (<i>Dr PDC
 *       clearing / Cr party payable</i>) and reinstatement of the paid payables; the voucher
 *       becomes VOIDED.
 *   <li><b>Replaced</b>: a new cheque number / date for the same payment; no posting (the liability
 *       remains in PDC clearing).
 * </ol>
 *
 * Every transition is written to the status history (confirmation audit trail).
 */
@Service
@Transactional
public class IssuedPdcService {

  /** Event type posted on presentation. */
  public static final String PRESENTED_EVENT = "PDC_ISSUED_PRESENTED";

  private static final String ENTITY = "IssuedPdc";

  private final IssuedPdcRepository pdcs;
  private final IssuedPdcEventRepository events;
  private final PaymentVoucherRepository vouchers;
  private final BankAccountQueryService banks;
  private final BankAccountService bankService;
  private final PaymentPoster poster;
  private final AccountingEventPublisher publisher;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param pdcs register
   * @param events status history
   * @param vouchers vouchers
   * @param banks bank accounts
   * @param bankService cheque allocation
   * @param poster payment posting / reversal
   * @param publisher accounting engine
   * @param audit audit trail
   * @param clock clock
   */
  public IssuedPdcService(
      IssuedPdcRepository pdcs,
      IssuedPdcEventRepository events,
      PaymentVoucherRepository vouchers,
      BankAccountQueryService banks,
      BankAccountService bankService,
      PaymentPoster poster,
      AccountingEventPublisher publisher,
      AuditTrailService audit,
      Clock clock) {
    this.pdcs = pdcs;
    this.events = events;
    this.vouchers = vouchers;
    this.banks = banks;
    this.bankService = bankService;
    this.poster = poster;
    this.publisher = publisher;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Lists the register.
   *
   * @param companyId company
   * @param statuses statuses
   * @param from cheque date from
   * @param to cheque date to
   * @return cheques
   */
  @Transactional(readOnly = true)
  public List<IssuedPdc> search(
      Long companyId, Collection<IssuedPdcStatus> statuses, LocalDate from, LocalDate to) {
    return pdcs.search(companyId, statuses, from, to);
  }

  /**
   * Gets a cheque.
   *
   * @param id id
   * @return cheque
   */
  @Transactional(readOnly = true)
  public IssuedPdc get(Long id) {
    return pdcs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Issued PDC", id));
  }

  /**
   * Status history of a cheque.
   *
   * @param id cheque
   * @return events
   */
  @Transactional(readOnly = true)
  public List<IssuedPdcEvent> history(Long id) {
    return events.findByPdcIdOrderByIdAsc(id);
  }

  /**
   * Registers the cheque of an approved PDC voucher (called by the approval).
   *
   * @param voucher approved voucher
   * @return register entry
   */
  public IssuedPdc register(PaymentVoucher voucher) {
    IssuedPdc pdc = pdcs.save(new IssuedPdc(voucher));
    events.save(
        new IssuedPdcEvent(
            pdc.getId(),
            voucher.getVoucherDate(),
            null,
            IssuedPdcStatus.ISSUED,
            voucher.getJournalBatchNo(),
            "Issued with " + voucher.getVoucherNo()));
    return pdc;
  }

  /**
   * Marks issued cheques whose date is reached as DUE.
   *
   * @param asOf date
   * @return number of cheques marked
   */
  public int refreshDue(LocalDate asOf) {
    int count = 0;
    for (IssuedPdc pdc :
        pdcs.findByStatusAndChequeDateLessThanEqual(IssuedPdcStatus.ISSUED, asOf)) {
      pdc.markDueIfReached(asOf);
      events.save(
          new IssuedPdcEvent(
              pdc.getId(),
              pdc.getChequeDate(),
              IssuedPdcStatus.ISSUED,
              IssuedPdcStatus.DUE,
              null,
              "Cheque date reached"));
      count++;
    }
    return count;
  }

  /** Daily job: flags cheques that fell due. */
  @Scheduled(cron = "${finverse.payables.pdc-due-cron:0 15 0 * * *}")
  public void refreshDueDaily() {
    refreshDue(LocalDate.now(clock));
  }

  /**
   * Confirms presentation and clears the PDC liability against the bank.
   *
   * @param id cheque
   * @param date presentation (bank) date
   * @return cheque
   */
  public IssuedPdc present(Long id, LocalDate date) {
    IssuedPdc pdc = get(id);
    IssuedPdcStatus from = pdc.getStatus();
    BankAccount bank = banks.get(pdc.getBankAccountId());
    PaymentVoucher voucher = voucher(pdc);
    String batchNo =
        publisher
            .publish(
                new BusinessEvent(
                    PRESENTED_EVENT,
                    pdc.getCompanyId(),
                    pdc.getBranchId(),
                    date,
                    pdc.getCurrency(),
                    PayablesSupport.MODULE,
                    "PDC:" + pdc.getId() + ":PRESENTED",
                    voucher.getVoucherNo(),
                    pdc.getPartyCode(),
                    null,
                    null,
                    "PDC " + pdc.getChequeNo() + " presented - " + pdc.getPayeeName(),
                    Map.of("AMOUNT", pdc.getAmount()),
                    Map.of(
                        "PDC_CLEARING", PaymentPoster.creditAccount(voucher, bank),
                        "BANK", bank.getGlAccountCode())))
            .getBatchNo();
    pdc.present(date, batchNo);
    record(pdc, date, from, batchNo, "Presented and confirmed");
    return pdc;
  }

  /**
   * Records that a presented cheque appears on the bank statement.
   *
   * @param id cheque
   * @param date statement date
   * @return cheque
   */
  public IssuedPdc clear(Long id, LocalDate date) {
    IssuedPdc pdc = get(id);
    pdc.clear(date);
    record(pdc, date, IssuedPdcStatus.PRESENTED, null, "Cleared by the bank");
    return pdc;
  }

  /**
   * Stops an outstanding cheque: the payment is reversed and the payables reinstated.
   *
   * @param id cheque
   * @param date cancellation date
   * @param reason reason
   * @return cheque
   */
  public IssuedPdc cancel(Long id, LocalDate date, String reason) {
    IssuedPdc pdc = get(id);
    IssuedPdcStatus from = pdc.getStatus();
    PaymentVoucher voucher = voucher(pdc);
    String batchNo = poster.reverse(voucher, banks.get(pdc.getBankAccountId()), date, reason);
    pdc.cancel(date, batchNo, reason);
    voucher.markVoided(date, batchNo, reason);
    record(pdc, date, from, batchNo, reason);
    return pdc;
  }

  /**
   * Replaces an outstanding cheque by a new leaf of the same bank account.
   *
   * @param id cheque
   * @param newChequeDate date of the new cheque
   * @param date replacement date
   * @param reason reason
   * @return the new cheque
   */
  public IssuedPdc replace(Long id, LocalDate newChequeDate, LocalDate date, String reason) {
    IssuedPdc old = get(id);
    IssuedPdcStatus from = old.getStatus();
    PaymentVoucher voucher = voucher(old);
    String newNo = bankService.nextChequeNo(old.getBankAccountId());
    IssuedPdc replacement = pdcs.save(IssuedPdc.replacementOf(old, newNo, newChequeDate, date));
    old.replaceWith(date, replacement, reason);
    voucher.replaceCheque(newNo, newChequeDate);
    record(old, date, from, null, "Replaced by cheque " + newNo + ": " + reason);
    events.save(
        new IssuedPdcEvent(
            replacement.getId(),
            date,
            null,
            IssuedPdcStatus.ISSUED,
            null,
            "Replaces cheque " + old.getChequeNo()));
    return replacement;
  }

  private PaymentVoucher voucher(IssuedPdc pdc) {
    return vouchers
        .findWithAllocationsById(pdc.getVoucherId())
        .orElseThrow(() -> new ResourceNotFoundException("Payment voucher", pdc.getVoucherId()));
  }

  private void record(
      IssuedPdc pdc, LocalDate date, IssuedPdcStatus from, String batchNo, String remarks) {
    events.save(new IssuedPdcEvent(pdc.getId(), date, from, pdc.getStatus(), batchNo, remarks));
    audit.record(
        ENTITY,
        pdc.getChequeNo(),
        AuditAction.UPDATE,
        "Cheque " + pdc.getChequeNo() + " " + from + " -> " + pdc.getStatus());
  }
}
