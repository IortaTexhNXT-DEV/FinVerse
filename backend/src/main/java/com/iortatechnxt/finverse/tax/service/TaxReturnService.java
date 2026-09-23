package com.iortatechnxt.finverse.tax.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.finverse.tax.domain.RemittanceFacts;
import com.iortatechnxt.finverse.tax.domain.RemittancePosting;
import com.iortatechnxt.finverse.tax.domain.ReturnHeader;
import com.iortatechnxt.finverse.tax.domain.ReturnStatus;
import com.iortatechnxt.finverse.tax.domain.TaxForm;
import com.iortatechnxt.finverse.tax.domain.TaxPeriod;
import com.iortatechnxt.finverse.tax.domain.TaxRemittance;
import com.iortatechnxt.finverse.tax.domain.TaxRemittanceRepository;
import com.iortatechnxt.finverse.tax.domain.TaxReturn;
import com.iortatechnxt.finverse.tax.domain.TaxReturnRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tax returns: prepare from the worksheet (DRAFT), refresh, file (four eyes), pay (posts the
 * remittance) and cancel. Every action is audited.
 */
@Service
@Transactional
public class TaxReturnService {

  /** Audit entity name. */
  public static final String ENTITY = "TaxReturn";

  private final TaxReturnRepository returns;
  private final TaxRemittanceRepository remittances;
  private final TaxFormService forms;
  private final TaxWorksheetService worksheets;
  private final TaxRemittancePoster poster;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param returns returns
   * @param remittances remittances
   * @param forms tax forms
   * @param worksheets worksheets
   * @param poster remittance posting
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public TaxReturnService(
      TaxReturnRepository returns,
      TaxRemittanceRepository remittances,
      TaxFormService forms,
      TaxWorksheetService worksheets,
      TaxRemittancePoster poster,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.returns = returns;
    this.remittances = remittances;
    this.forms = forms;
    this.worksheets = worksheets;
    this.poster = poster;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Returns whose period starts in a year, optionally filtered.
   *
   * @param companyId company
   * @param year calendar year
   * @param formCode form, null for all
   * @param status status, null for all
   * @return returns ordered by period and form
   */
  @Transactional(readOnly = true)
  public List<TaxReturn> list(Long companyId, int year, String formCode, ReturnStatus status) {
    LocalDate first = LocalDate.of(year, 1, 1);
    return returns.search(companyId, first, first.plusYears(1).minusDays(1), formCode, status);
  }

  /**
   * Gets a return with its lines.
   *
   * @param id id
   * @return return
   */
  @Transactional(readOnly = true)
  public TaxReturn get(Long id) {
    return returns
        .findWithLinesById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * The remittance of a return.
   *
   * @param returnId return
   * @return remittance when paid
   */
  @Transactional(readOnly = true)
  public Optional<TaxRemittance> remittance(Long returnId) {
    return remittances.findByReturnId(returnId);
  }

  /**
   * Remittances paid in a date range.
   *
   * @param companyId company
   * @param from paid on from
   * @param to paid on to
   * @return remittances ordered by payment date and form
   */
  @Transactional(readOnly = true)
  public List<TaxRemittance> remittances(Long companyId, LocalDate from, LocalDate to) {
    return remittances.findByCompanyIdAndPaidOnBetweenOrderByPaidOnAscFormCodeAsc(
        companyId, from, to);
  }

  /**
   * Prepares a draft return from the worksheet of its period.
   *
   * @param companyId company
   * @param formCode form
   * @param periodStart first day of the filing period
   * @return draft
   */
  public TaxReturn create(Long companyId, String formCode, LocalDate periodStart) {
    TaxForm form = forms.requireActive(companyId, formCode);
    if (!form.isTrackFiling()) {
      throw new BusinessRuleException(
          "FORM_NOT_TRACKED", "Form " + formCode + " is prepared outside FinVerse");
    }
    TaxPeriod period = form.schedule().periodStarting(periodStart);
    returns
        .findByCompanyIdAndFormCodeAndPeriodStartAndStatusNot(
            companyId, formCode, periodStart, ReturnStatus.CANCELLED)
        .ifPresent(
            r -> {
              throw new DuplicateResourceException(ENTITY, r.getReturnNo());
            });
    String returnNo = numbers.next("TXR-" + period.from().getYear());
    TaxReturn draft =
        new TaxReturn(
            new ReturnHeader(
                companyId,
                returnNo,
                formCode,
                form.getWorksheet(),
                period,
                form.schedule().dueDate(period.to())),
            currentUser.username(),
            clock.instant());
    compute(draft);
    TaxReturn saved = returns.save(draft);
    audit.record(
        ENTITY, returnNo, AuditAction.CREATE, "Prepared " + formCode + " " + period.label());
    return saved;
  }

  /**
   * Recomputes a draft from the current worksheet.
   *
   * @param id return
   * @return draft
   */
  public TaxReturn refresh(Long id) {
    TaxReturn r = get(id);
    r.clearLines();
    returns.flush();
    compute(r);
    audit.record(ENTITY, r.getReturnNo(), AuditAction.UPDATE, "Recomputed from the worksheet");
    return r;
  }

  /**
   * Records the filing of a draft; the filer must not be the preparer.
   *
   * @param id return
   * @param filedOn filing date
   * @param reference eFPS / eBIRForms confirmation number
   * @return filed return
   */
  public TaxReturn file(Long id, LocalDate filedOn, String reference) {
    TaxReturn r = get(id);
    r.file(currentUser.username(), clock.instant(), filedOn, reference);
    audit.record(ENTITY, r.getReturnNo(), AuditAction.SUBMIT, "Filed, reference " + reference);
    return r;
  }

  /**
   * Pays a filed return: posts the remittance and records it.
   *
   * @param id return
   * @param facts payment facts
   * @return paid return
   */
  public TaxReturn pay(Long id, RemittanceFacts facts) {
    TaxReturn r = get(id);
    if (facts.paidOn().isBefore(r.getPeriodStart())) {
      throw new BusinessRuleException(
          "INVALID_PAYMENT_DATE", "Payment date precedes the start of the period");
    }
    TaxForm form = forms.requireActive(r.getCompanyId(), r.getFormCode());
    r.markPaid();
    RemittancePosting posting = poster.post(r, form, facts);
    remittances.save(new TaxRemittance(r, facts, posting));
    audit.record(
        ENTITY,
        r.getReturnNo(),
        AuditAction.POST,
        "Paid " + posting.amount() + (posting.batchNo() == null ? "" : ", " + posting.batchNo()));
    return r;
  }

  /**
   * Cancels a draft.
   *
   * @param id return
   * @param reason reason
   * @return cancelled return
   */
  public TaxReturn cancel(Long id, String reason) {
    TaxReturn r = get(id);
    r.cancel(reason);
    audit.record(ENTITY, r.getReturnNo(), AuditAction.DEACTIVATE, "Cancelled: " + reason);
    return r;
  }

  private void compute(TaxReturn r) {
    TaxWorksheet w = worksheets.compute(r.getCompanyId(), r.getWorksheet(), r.period());
    r.recompute(w.figures(), w.lines(), currentUser.username(), clock.instant());
  }
}
