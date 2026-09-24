package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.tax.domain.TaxForm;
import com.iortatechnxt.brokerverse.tax.domain.TaxFormRepository;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maintenance of the tax forms of the filing calendar (maker-checker, audited). */
@Service
@Transactional
public class TaxFormService {

  /** Audit entity name. */
  public static final String ENTITY = "TaxForm";

  private final TaxFormRepository forms;
  private final TaxMasterSupport support;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param forms repository
   * @param support validations
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public TaxFormService(
      TaxFormRepository forms,
      TaxMasterSupport support,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.forms = forms;
    this.support = support;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists the forms of a company.
   *
   * @param companyId company
   * @return forms ordered by code
   */
  @Transactional(readOnly = true)
  public List<TaxForm> list(Long companyId) {
    return forms.findByCompanyIdOrderByCode(companyId);
  }

  /**
   * Authorized forms of a company.
   *
   * @param companyId company
   * @return active forms ordered by code
   */
  @Transactional(readOnly = true)
  public List<TaxForm> active(Long companyId) {
    return forms.findByCompanyIdAndRecordStatusOrderByCode(companyId, RecordStatus.ACTIVE);
  }

  /**
   * Gets a form.
   *
   * @param id id
   * @return form
   */
  @Transactional(readOnly = true)
  public TaxForm get(Long id) {
    return forms.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Gets an authorized form by code.
   *
   * @param companyId company
   * @param code form code
   * @return form
   * @throws BusinessRuleException when the form is not authorized
   */
  @Transactional(readOnly = true)
  public TaxForm requireActive(Long companyId, String code) {
    TaxForm form =
        forms
            .findByCompanyIdAndCode(companyId, code)
            .orElseThrow(() -> new ResourceNotFoundException(ENTITY, code));
    if (!form.isActive()) {
      throw new BusinessRuleException("TAX_FORM_NOT_ACTIVE", "Form " + code + " is not authorized");
    }
    return form;
  }

  /**
   * Creates a form (pending authorization).
   *
   * @param c values
   * @return form
   */
  public TaxForm create(TaxFormCommand c) {
    if (forms.existsByCompanyIdAndCode(c.companyId(), c.code())) {
      throw new DuplicateResourceException(ENTITY, c.code());
    }
    TaxForm form = new TaxForm(c.companyId(), c.code());
    apply(form, c);
    TaxForm saved = forms.save(form);
    audit.record(ENTITY, saved.getCode(), AuditAction.CREATE, "Created tax form " + c.name());
    return saved;
  }

  /**
   * Updates a form; it returns to pending authorization.
   *
   * @param id id
   * @param c values (code and company are immutable)
   * @return form
   */
  public TaxForm update(Long id, TaxFormCommand c) {
    TaxForm form = get(id);
    apply(form, c);
    form.markModified();
    audit.record(ENTITY, form.getCode(), AuditAction.UPDATE, "Updated tax form");
    return form;
  }

  /**
   * Authorizes a form (checker).
   *
   * @param id id
   * @return form
   */
  public TaxForm authorize(Long id) {
    TaxForm form = get(id);
    form.authorize(currentUser.username(), clock.instant());
    audit.record(ENTITY, form.getCode(), AuditAction.AUTHORIZE, "Authorized tax form");
    return form;
  }

  private void apply(TaxForm form, TaxFormCommand c) {
    String payable = TaxMasterSupport.blankToNull(c.payableAccountCode());
    String credit = TaxMasterSupport.blankToNull(c.creditAccountCode());
    if (c.trackFiling() && (c.worksheet() == WorksheetKind.NONE || payable == null)) {
      throw new BusinessRuleException(
          "TRACKED_FORM_INCOMPLETE",
          "A tracked form needs a worksheet and the tax payable account its payment clears");
    }
    if (payable != null) {
      support.requirePostable(c.companyId(), payable, "Tax payable");
    }
    if (credit != null) {
      support.requirePostable(c.companyId(), credit, "Tax credit");
    }
    form.setName(c.name());
    form.setAuthority(c.authority());
    form.setFrequency(c.frequency());
    form.setWorksheet(c.worksheet());
    form.setDueMonthsAfter(c.dueMonthsAfter());
    form.setDueDay(c.dueDay());
    form.setPayableAccountCode(payable);
    form.setCreditAccountCode(credit);
    form.setTrackFiling(c.trackFiling());
    form.setEffectiveFrom(c.effectiveFrom());
  }
}
