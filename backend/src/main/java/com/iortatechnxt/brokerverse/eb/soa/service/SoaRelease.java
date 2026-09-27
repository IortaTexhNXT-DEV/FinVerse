package com.iortatechnxt.brokerverse.eb.soa.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.domain.TatActivity;
import com.iortatechnxt.brokerverse.eb.service.EbActivityLog;
import com.iortatechnxt.brokerverse.eb.service.EbMailer;
import com.iortatechnxt.brokerverse.eb.service.EbMailer.Mail;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import com.iortatechnxt.brokerverse.eb.service.EbTemplates;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Releases a validated SOA (BRID-021; FR-EB-053): e-mailed (protected) to the client's HR contacts
 * that receive the SOA with the template {@code EB_SOA_RELEASE}; Collection is notified and sees
 * the SOA through its access class.
 */
@Service
@Transactional
public class SoaRelease {

  private final EbSoaService soas;
  private final EbRecords records;
  private final EbParties parties;
  private final EbTemplates templates;
  private final EbMailer mailer;
  private final WorkflowService workflow;
  private final NotificationService notifications;
  private final EbActivityLog activity;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param soas SOAs
   * @param records programme look-up
   * @param parties contacts and template values
   * @param templates release template
   * @param mailer e-mails
   * @param workflow workflow engine
   * @param notifications in-app notices
   * @param activity TAT stamps
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public SoaRelease(
      EbSoaService soas,
      EbRecords records,
      EbParties parties,
      EbTemplates templates,
      EbMailer mailer,
      WorkflowService workflow,
      NotificationService notifications,
      EbActivityLog activity,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.soas = soas;
    this.records = records;
    this.parties = parties;
    this.templates = templates;
    this.mailer = mailer;
    this.workflow = workflow;
    this.notifications = notifications;
    this.activity = activity;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Releases a validated SOA to the client and Collection.
   *
   * @param companyId company
   * @param soaId SOA
   * @return the SOA, RELEASED
   */
  public EbSoa release(Long companyId, Long soaId) {
    EbSoa soa = soas.require(companyId, soaId);
    EbSoaService.requireStatus(soa, EbSoa.Status.VALIDATED);
    EbProgramme programme = records.programme(companyId, soa.getProgrammeId());
    List<String> to = EbParties.soaEmails(programme);
    if (to.isEmpty()) {
      throw new BusinessRuleException(
          "EB_CONTACT_REQUIRED",
          "Programme " + programme.getProgrammeNo() + " has no active HR contact");
    }
    Map<String, Object> values = parties.values(programme, null);
    values.put("insurerName", parties.insurerName(companyId, soa.getInsurerCode()));
    values.put("soaNo", soa.getInsurerSoaNo());
    values.put(
        "period", EbParties.date(soa.getPeriodFrom()) + " to " + EbParties.date(soa.getPeriodTo()));
    values.put("amount", soa.getCurrency() + " " + amount(soa.getAmount()));
    MergedText text = templates.merge(EbCodes.TEMPLATE_SOA_RELEASE, values);
    mailer.send(
        companyId,
        EbCodes.PURPOSE_SOA,
        new Mail(
            to,
            parties.aoCopy(programme),
            text.title(),
            text.text(),
            mailer.files(List.of(soa.getAttachmentId()))),
        new RecordLink(EbCodes.ENTITY_SOA, soa.getId().toString(), soa.getSoaNo()));
    soa.released(currentUser.username(), clock.instant());
    workflow.systemTransition(
        EbCodes.ENTITY_SOA, soa.getId().toString(), "release", TransitionNote.NONE);
    activity.released(TatActivity.RELEASE, soa.getSoaNo(), "Released");
    notifications.notifyPermission(
        EbCodes.PERMISSION_COLLECT,
        new Notice(
            soa.getSoaNo() + ": SOA released for collection",
            programme.getClientName() + " - " + soa.getInsurerSoaNo(),
            EbCodes.SOA_LINK + soa.getId(),
            EbCodes.ENTITY_SOA,
            soa.getId().toString()),
        EbCodes.EVENT_SOA_RELEASED);
    audit.record(
        EbCodes.ENTITY_SOA,
        soa.getSoaNo(),
        AuditAction.SUBMIT,
        "Released to " + String.join(", ", to));
    return soa;
  }

  private static String amount(BigDecimal value) {
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH))
        .format(value);
  }
}
