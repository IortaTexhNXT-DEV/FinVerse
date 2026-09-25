package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EventSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.InstrumentStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentEvent;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentEventRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentLifecycle;
import com.iortatechnxt.brokerverse.disbursement.domain.InstrumentRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.IntakeRequestRepository;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherRepository;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The life of payment instruments (DIS 2.8.0, 3.26.0-3.26.7, 3.27.1): an instrument is issued when
 * its voucher is approved and moves along {@link InstrumentLifecycle}; every change is kept in the
 * status history with its source (user, system, upload, job), audited and reported on the
 * Operations request (a paid status marks the request PAID). A negotiated check clears the checks
 * outstanding against the bank and a stale check moves them to Miscellaneous Liability, through the
 * events {@code DISB_CHECK_NEGOTIATED} and {@code DISB_CHECK_STALE} while {@code
 * DISB_CHECK_CLEARING} is ON.
 */
@Service
@Transactional
public class InstrumentService {

  private final InstrumentRepository instruments;
  private final InstrumentEventRepository events;
  private final VoucherRepository vouchers;
  private final IntakeRequestRepository requests;
  private final GatewaySync gateway;
  private final ProformaBuilder proforma;
  private final AccountingEventPublisher publisher;
  private final BankAccountQueryService banks;
  private final BookRates rates;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param instruments instruments
   * @param events status history
   * @param vouchers vouchers
   * @param requests payment requests
   * @param gateway Operations request progress
   * @param proforma clearing parameter
   * @param publisher accounting engine
   * @param banks bank accounts
   * @param rates BOOK rates
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public InstrumentService(
      InstrumentRepository instruments,
      InstrumentEventRepository events,
      VoucherRepository vouchers,
      IntakeRequestRepository requests,
      GatewaySync gateway,
      ProformaBuilder proforma,
      AccountingEventPublisher publisher,
      BankAccountQueryService banks,
      BookRates rates,
      AuditTrailService audit,
      Clock clock) {
    this.instruments = instruments;
    this.events = events;
    this.vouchers = vouchers;
    this.requests = requests;
    this.gateway = gateway;
    this.proforma = proforma;
    this.publisher = publisher;
    this.banks = banks;
    this.rates = rates;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Issues the instrument of an approved voucher in the first status of its mode.
   *
   * @param v voucher
   * @return instrument
   */
  public Instrument issue(Voucher v) {
    Instrument i =
        instruments.save(
            new Instrument(
                v.getId(), v.getMode(), v.getNet(), v.getCurrency(), v.getBankAccountId()));
    events.save(
        new InstrumentEvent(
            i.getId(), null, i.getStatus(), EventSource.SYSTEM, "DV approved", null));
    return i;
  }

  /**
   * Moves an instrument to its next status.
   *
   * @param i instrument
   * @param to status
   * @param change who changed it, why, and the file
   * @return instrument
   */
  public Instrument advance(Instrument i, InstrumentStatus to, Change change) {
    InstrumentStatus from = i.move(to, clock.instant());
    recorded(i, from, change);
    return i;
  }

  /**
   * Applies a status approved as a status edit (DIS 2.8.5).
   *
   * @param i instrument
   * @param to status
   * @param change change
   * @return instrument
   */
  public Instrument correct(Instrument i, InstrumentStatus to, Change change) {
    InstrumentStatus from = i.correct(to, clock.instant());
    recorded(i, from, change);
    return i;
  }

  private void recorded(Instrument i, InstrumentStatus from, Change change) {
    events.save(
        new InstrumentEvent(
            i.getId(), from, i.getStatus(), change.source(), change.note(), change.fileRef()));
    audit.record(
        DisbursementSettings.INSTRUMENT,
        i.getId(),
        AuditAction.UPDATE,
        i.getMode() + " " + i.label() + ": " + from + " -> " + i.getStatus());
    IntakeRequest request = requestOf(i);
    if (InstrumentLifecycle.isPaid(i.getStatus())) {
      gateway.paid(request, i.getStatus().name());
    } else {
      gateway.track(request, null, i.getStatus().name());
    }
  }

  /**
   * Cancels the instrument of a cancelled approved voucher (DIS 2.20.0); a negotiated or stale
   * check cannot be cancelled.
   *
   * @param v voucher
   */
  public void cancelFor(Voucher v) {
    instruments
        .findByVoucherId(v.getId())
        .ifPresent(
            i -> {
              if (!InstrumentLifecycle.allows(
                  i.getMode(), i.getStatus(), InstrumentStatus.CANCELLED)) {
                throw new BusinessRuleException(
                    "INSTRUMENT_FINAL",
                    i.getMode()
                        + " "
                        + i.label()
                        + " is "
                        + i.getStatus()
                        + " and cannot be cancelled");
              }
              InstrumentStatus from = i.move(InstrumentStatus.CANCELLED, clock.instant());
              events.save(
                  new InstrumentEvent(
                      i.getId(), from, i.getStatus(), EventSource.SYSTEM, "DV cancelled", null));
            });
  }

  /**
   * A released check was deposited (DIS 3.26.1, 3.27.1): negotiated, and the checks outstanding
   * cleared against the bank. A check still printed is released first.
   *
   * @param i check
   * @param change upload or bank-reconciliation match
   * @return instrument
   */
  public Instrument negotiated(Instrument i, Change change) {
    if (i.getStatus() == InstrumentStatus.PRINTED) {
      advance(
          i, InstrumentStatus.RELEASED, new Change(change.source(), "Released (deposited)", null));
    }
    advance(i, InstrumentStatus.NEGOTIATED, change);
    if (proforma.clearingOn()) {
      post(i, DisbursementSettings.EVENT_NEGOTIATED, ":NEG", true);
    }
    return i;
  }

  /**
   * A check reached {@code DISB_STALE_DAYS} (DIS 3.26.2, 3.27.1): stale, and the checks outstanding
   * moved to Miscellaneous Liability - stale checks of the payee.
   *
   * @param i check
   * @param change job or user
   * @return instrument
   */
  public Instrument stale(Instrument i, Change change) {
    advance(i, InstrumentStatus.STALE, change);
    if (proforma.clearingOn()) {
      post(i, DisbursementSettings.EVENT_STALE, ":STALE", false);
    }
    return i;
  }

  private void post(Instrument i, String eventType, String suffix, boolean withBank) {
    Voucher v = voucherOf(i);
    LocalDate today = LocalDate.now(clock);
    Map<String, String> accounts =
        withBank ? Map.of("BANK", banks.get(i.getBankAccountId()).getGlAccountCode()) : Map.of();
    publisher.publish(
        rates.price(
            new BusinessEvent(
                eventType,
                v.getCompanyId(),
                v.getBranchId(),
                today,
                v.getCurrency(),
                DisbursementSettings.MODULE,
                "CHK:" + i.getId() + suffix,
                i.label(),
                v.getPayeeCode(),
                null,
                v.getCostCenter(),
                "Check " + i.label() + " of DV " + v.getDvNo(),
                Map.of("AMOUNT", i.getAmount()),
                accounts)));
  }

  /**
   * The instrument of a voucher.
   *
   * @param voucherId voucher
   * @return instrument
   */
  @Transactional(readOnly = true)
  public Instrument forVoucher(Long voucherId) {
    return instruments
        .findByVoucherId(voucherId)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "DV_NOT_APPROVED", "The DV has no instrument until it is approved"));
  }

  /**
   * An instrument.
   *
   * @param id id
   * @return instrument
   */
  @Transactional(readOnly = true)
  public Instrument get(Long id) {
    return instruments
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(DisbursementSettings.INSTRUMENT, id));
  }

  /**
   * The status history of an instrument.
   *
   * @param instrumentId instrument
   * @return events, oldest first
   */
  @Transactional(readOnly = true)
  public List<InstrumentEvent> history(Long instrumentId) {
    return events.findByInstrumentIdOrderByIdAsc(instrumentId);
  }

  /**
   * The voucher of an instrument.
   *
   * @param i instrument
   * @return voucher
   */
  @Transactional(readOnly = true)
  public Voucher voucherOf(Instrument i) {
    return vouchers
        .findWithLinesById(i.getVoucherId())
        .orElseThrow(
            () -> new ResourceNotFoundException(DisbursementSettings.VOUCHER, i.getVoucherId()));
  }

  private IntakeRequest requestOf(Instrument i) {
    Long requestId = voucherOf(i).getRequestId();
    return requests
        .findById(requestId)
        .orElseThrow(() -> new ResourceNotFoundException(DisbursementSettings.REQUEST, requestId));
  }

  /**
   * Who changed an instrument status, why, and from which file.
   *
   * @param source source
   * @param note note, may be null
   * @param fileRef upload reference, may be null
   */
  public record Change(EventSource source, String note, String fileRef) {

    /**
     * A change by a user.
     *
     * @param note note
     * @return change
     */
    public static Change user(String note) {
      return new Change(EventSource.USER, note, null);
    }
  }
}
