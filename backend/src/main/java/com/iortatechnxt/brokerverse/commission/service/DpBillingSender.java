package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.commission.domain.DpBilling;
import com.iortatechnxt.brokerverse.commission.domain.DpItem;
import com.iortatechnxt.brokerverse.commission.domain.DpItemRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.messaging.service.QueuedEmail;
import com.iortatechnxt.brokerverse.opsledger.domain.ExtractFile;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DropContent;
import com.iortatechnxt.brokerverse.opsledger.service.port.FileDropPort.DroppedFile;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sends a commission billing to its insurer (CMRID.009/011/012): the billing file (one row per
 * account with the columns the insurer answers in: Decision, Reason, Comment) stored in the extract
 * repository and e-mailed password protected, the accounts tagged "billed", and the feedback due
 * date set to {@code CMR_FEEDBACK_WORKING_DAYS} working days later. The billing format is parked
 * (OQ38); the insurer's addresses default to its placement e-mails in the catalog.
 */
@Service
@Transactional
public class DpBillingSender {

  /** Columns of the billing file, also read back from the insurer's answer. */
  public static final List<String> COLUMNS =
      List.of(
          DpResponseLayout.BILLING,
          DpResponseLayout.INVOICE,
          "Policy No.",
          "Assured Name",
          "Premium",
          "Commission",
          "VAT",
          "Withholding Tax",
          "Net Commission",
          DpResponseLayout.DECISION,
          DpResponseLayout.REASON,
          DpResponseLayout.COMMENT);

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final DpBillingService billings;
  private final DpItemRepository items;
  private final DocumentComposer composer;
  private final FileDropPort drop;
  private final MessageService messages;
  private final InsurerService insurers;
  private final FeedbackCalendar calendar;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the sender.
   *
   * @param billings billings
   * @param items DP accounts
   * @param composer workbook writer
   * @param drop extract repository
   * @param messages outbound e-mail
   * @param insurers insurer profiles
   * @param calendar feedback due date
   * @param workflow workflow engine
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public DpBillingSender(
      DpBillingService billings,
      DpItemRepository items,
      DocumentComposer composer,
      FileDropPort drop,
      MessageService messages,
      InsurerService insurers,
      FeedbackCalendar calendar,
      WorkflowService workflow,
      AuditTrailService audit,
      Clock clock) {
    this.billings = billings;
    this.items = items;
    this.composer = composer;
    this.drop = drop;
    this.messages = messages;
    this.insurers = insurers;
    this.calendar = calendar;
    this.workflow = workflow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Sends a billing.
   *
   * @param id billing
   * @param to recipients; the insurer's placement addresses when empty
   * @param cc copy recipients
   * @return the billing
   */
  public DpBilling send(Long id, List<String> to, List<String> cc) {
    DpBilling billing = billings.require(id);
    if (!DpBilling.FOR_BILLING.equals(billing.getStage())) {
      throw new BusinessRuleException(
          "DP_BILLING_SENT", "Billing " + billing.getBillingNo() + " was already sent");
    }
    Optional<InsurerProfile> insurer = profile(billing);
    List<String> recipients =
        to == null || to.isEmpty()
            ? insurer.map(InsurerProfile::getPlacementEmailList).orElse(List.of())
            : to;
    if (recipients.isEmpty()) {
      throw new BusinessRuleException(
          "DP_NO_RECIPIENT", "Enter the e-mail address of " + billing.getInsurerCode());
    }
    List<DpItem> accounts = items.findByBillingIdOrderByIdAsc(id);
    String fileName = billing.getBillingNo() + ".xlsx";
    byte[] workbook = composer.xlsx(sheet(billing, accounts));
    DroppedFile file =
        drop.drop(
            billing.getCompanyId(),
            new ExtractFile.Location(
                DpIntakeService.MODULE + "/" + billing.getInsurerCode(), fileName),
            new DropContent(XLSX, workbook),
            new ExtractFile.Origin(DpIntakeService.MODULE, billing.getBillingNo()));
    QueuedEmail queued =
        mail(
            billing,
            new Addressees(recipients, cc),
            insurer.map(InsurerProfile::getName).orElse(billing.getInsurerCode()),
            new MessageFile(fileName, XLSX, workbook));
    LocalDate today = LocalDate.now(clock);
    billing.sent(
        new DpBilling.FileRef(file.id(), fileName),
        clock.instant(),
        calendar.dueDate(billing.getCompanyId(), today),
        new DpBilling.Sent(queued.messageId(), String.join(", ", recipients)));
    accounts.forEach(DpItem::billed);
    workflow.transition(
        DpBillingService.ENTITY,
        String.valueOf(id),
        "bill",
        TransitionNote.comment("Sent to " + String.join(", ", recipients)));
    audit.record(
        DpBillingService.ENTITY,
        billing.getBillingNo(),
        AuditAction.UPDATE,
        "Sent to " + String.join(", ", recipients) + ", feedback due " + billing.getSlaDue());
    return billing;
  }

  private QueuedEmail mail(DpBilling billing, Addressees to, String insurerName, MessageFile file) {
    return messages.queueEmail(
        new OutboundEmail(
            billing.getCompanyId(),
            "DP_BILLING",
            to.to(),
            to.cc(),
            "Direct payment commission billing " + billing.getBillingNo(),
            body(billing, insurerName),
            List.of(file),
            new OutboundEmail.Protection(null, true, null),
            new RecordLink(
                DpBillingService.ENTITY, String.valueOf(billing.getId()), billing.getBillingNo())));
  }

  /**
   * Recipients of an e-mail.
   *
   * @param to recipients
   * @param cc copy recipients
   */
  private record Addressees(List<String> to, List<String> cc) {}

  private static SheetSpec sheet(DpBilling billing, List<DpItem> accounts) {
    List<List<Object>> rows = new ArrayList<>();
    for (DpItem i : accounts) {
      List<Object> row = new ArrayList<>();
      row.add(billing.getBillingNo());
      row.add(i.getInvoiceNo());
      row.add(i.getPolicyNo());
      row.add(i.getAssuredName());
      row.add(i.getPremium());
      row.add(i.getCommission());
      row.add(i.getCommissionVat());
      row.add(i.getWtax());
      row.add(i.getNetCommission());
      row.add(null);
      row.add(null);
      row.add(null);
      rows.add(row);
    }
    return new SheetSpec("Commission Billing", COLUMNS, rows);
  }

  private static String body(DpBilling billing, String insurerName) {
    return "Dear "
        + insurerName
        + ",\n\nAttached is our billing "
        + billing.getBillingNo()
        + " of the commission on "
        + billing.getItemCount()
        + " direct payment account(s), net commission "
        + billing.getTotalNet()
        + ".\n\nPlease confirm each account in the Decision column (APPROVED or REJECTED, with the"
        + " reason of a rejection) and return the file. The file is password protected; the"
        + " password is sent in a separate e-mail.\n\nBDO Insurance and Reinsurance Brokers, Inc."
        + " - Commission Receivables";
  }

  private Optional<InsurerProfile> profile(DpBilling billing) {
    return insurers.insurers(billing.getCompanyId()).stream()
        .filter(i -> i.getPartyCode().equals(billing.getInsurerCode()))
        .findFirst();
  }
}
