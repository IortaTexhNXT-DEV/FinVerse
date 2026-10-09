package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.FormText;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptForm;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptFormRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The AR and OR forms set up online (FRS.CSH.02.06.01 to 02.06.03; Appendix R, C16): a change is a
 * new version effective from a date, used once another authorised user approved it; the earlier
 * versions are kept with their effective dates and every change is in the audit trail.
 */
@Service
@Transactional
public class ReceiptFormService {

  /** Audit entity. */
  public static final String ENTITY = "ReceiptForm";

  /** Permission of the approvers. */
  public static final String APPROVER = "MASTER_AUTHORIZE";

  private static final Set<String> KINDS = Set.of("AR", "OR");

  private final ReceiptFormRepository forms;
  private final NotificationService notifications;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param forms versions
   * @param notifications approvers told of a change
   * @param audit audit trail
   * @param currentUser maker and checker
   * @param clock clock
   */
  public ReceiptFormService(
      ReceiptFormRepository forms,
      NotificationService notifications,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.forms = forms;
    this.notifications = notifications;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The versions of the forms of a company.
   *
   * @param companyId company
   * @return versions
   */
  @Transactional(readOnly = true)
  public List<ReceiptForm> list(Long companyId) {
    return forms.findByCompanyIdOrderByFormKindAscVersionNoDesc(companyId);
  }

  /**
   * The approved version used on a print date.
   *
   * @param companyId company
   * @param kind AR or OR
   * @param date print date
   * @return version, the empty form when none is approved
   */
  @Transactional(readOnly = true)
  public FormText current(Long companyId, String kind, LocalDate date) {
    return forms
        .findFirstByCompanyIdAndFormKindAndStatusAndEffectiveFromLessThanEqualOrderByEffectiveFromDescVersionNoDesc(
            companyId, kind, ReceiptForm.APPROVED, date)
        .map(ReceiptForm::getText)
        .orElse(FormText.EMPTY);
  }

  /**
   * Proposes a new version of a form, effective from a date once approved.
   *
   * @param companyId company
   * @param kind AR or OR
   * @param text header, note and footer lines
   * @param effectiveFrom first print date, today or later
   * @return the version waiting for approval
   */
  public ReceiptForm propose(Long companyId, String kind, FormText text, LocalDate effectiveFrom) {
    if (!KINDS.contains(kind)) {
      throw new BusinessRuleException("FORM_KIND", "Select the AR form or the OR form");
    }
    if (effectiveFrom == null || effectiveFrom.isBefore(BusinessClock.today(clock))) {
      throw new BusinessRuleException(
          "FORM_EFFECTIVE_DATE", "The effective date must be today or a later date");
    }
    int next =
        forms
            .findFirstByCompanyIdAndFormKindOrderByVersionNoDesc(companyId, kind)
            .map(f -> f.getVersionNo() + 1)
            .orElse(1);
    ReceiptForm saved = forms.save(new ReceiptForm(companyId, kind, next, text, effectiveFrom));
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        kind + " form version " + next + " effective " + effectiveFrom + " for approval");
    notifications.notifyPermission(
        APPROVER,
        new Notice(
            kind + " form version " + next + " for approval",
            "Effective " + effectiveFrom + ", changed by " + currentUser.username(),
            "/cashiering/receipt-forms",
            ENTITY,
            saved.getId().toString()),
        "CASH_FORM_FOR_APPROVAL");
    return saved;
  }

  /**
   * Approves or rejects a version (another authorised user than the one who changed it).
   *
   * @param id version
   * @param approve true to approve
   * @param remarks remarks, required to reject
   * @return the version
   */
  public ReceiptForm decide(Long id, boolean approve, String remarks) {
    ReceiptForm form =
        forms.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    if (!approve && (remarks == null || remarks.isBlank())) {
      throw new BusinessRuleException("FORM_REJECT_REASON", "Enter the reason for the rejection");
    }
    form.decide(approve, currentUser.username(), remarks, clock.instant());
    audit.record(
        ENTITY,
        form.getId(),
        approve ? AuditAction.AUTHORIZE : AuditAction.REJECT,
        form.getFormKind()
            + " form version "
            + form.getVersionNo()
            + (approve ? " approved" : " rejected: " + remarks));
    return form;
  }
}
