package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceRepository;
import com.iortatechnxt.brokerverse.booking.domain.SiBatch;
import com.iortatechnxt.brokerverse.booking.domain.SiBatchRepository;
import com.iortatechnxt.brokerverse.booking.domain.SiBatchStatus;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.messaging.domain.MessageFile;
import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily service-invoice batches per insurer (G09): the service invoices issued to an insurer on a
 * business date and not yet in a batch form one batch per company, insurer and date, e-mailed to
 * the insurer's billing address with every service invoice attached. Generating again for a date
 * leaves the existing batches unchanged. The delivery outcome is recorded after the commit ({@link
 * SiBatchDispatchWatcher}); a failed batch is sent again by a user.
 */
@Service
@Transactional
public class SiBatchService {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "SI_DAILY_BATCH";

  /** Entity type of the batches (messages, audit). */
  public static final String ENTITY = "ServiceInvoiceBatch";

  private static final String PDF = "application/pdf";

  private static final int BODY_CAPACITY = 256;

  /** Service invoices to insurers of a date that are in no batch yet. */
  static final String BILLABLE =
      "select si.id from bkg_service_invoice si"
          + " join bkg_si_type t on t.code = si.type_code"
          + " where t.recipient = 'INSURER' and si.issue_date = ?"
          + " and not exists (select 1 from bkg_si_batch_item i where i.service_invoice_id = si.id)"
          + " order by si.company_id, si.recipient_code, si.id";

  private final SiBatchRepository batches;
  private final ServiceInvoiceRepository serviceInvoices;
  private final ServiceInvoiceDocument documents;
  private final PartyService parties;
  private final MessageService messages;
  private final ApplicationEventPublisher events;
  private final AuditTrailService audit;
  private final JdbcTemplate jdbc;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches batches
   * @param serviceInvoices service invoices
   * @param documents service invoice PDFs
   * @param parties insurer parties (billing address)
   * @param messages outbox
   * @param events event publisher
   * @param audit audit trail
   * @param jdbc JDBC (billable items)
   * @param clock clock
   */
  public SiBatchService(
      SiBatchRepository batches,
      ServiceInvoiceRepository serviceInvoices,
      ServiceInvoiceDocument documents,
      PartyService parties,
      MessageService messages,
      ApplicationEventPublisher events,
      AuditTrailService audit,
      JdbcTemplate jdbc,
      Clock clock) {
    this.batches = batches;
    this.serviceInvoices = serviceInvoices;
    this.documents = documents;
    this.parties = parties;
    this.messages = messages;
    this.events = events;
    this.audit = audit;
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /**
   * The e-mail of a batch was queued (delivered after the commit).
   *
   * @param batchId batch
   * @param messageId outbox message
   */
  public record SiBatchQueued(Long batchId, Long messageId) {}

  /**
   * Generates and sends the batches of a business date: one per company and insurer with service
   * invoices of the date not yet in a batch. An insurer that already has its batch for the date is
   * left as it is.
   *
   * @param date business date
   * @return batches generated
   */
  public List<SiBatch> generate(LocalDate date) {
    List<Long> ids = jdbc.queryForList(BILLABLE, Long.class, date);
    Map<String, List<ServiceInvoice>> byInsurer = new LinkedHashMap<>();
    for (ServiceInvoice si : serviceInvoices.findAllById(ids)) {
      byInsurer
          .computeIfAbsent(si.getCompanyId() + "|" + si.getRecipientCode(), k -> new ArrayList<>())
          .add(si);
    }
    List<SiBatch> generated = new ArrayList<>();
    for (List<ServiceInvoice> items : byInsurer.values()) {
      ServiceInvoice first = items.get(0);
      if (batches
          .findByCompanyIdAndInsurerCodeAndBatchDate(
              first.getCompanyId(), first.getRecipientCode(), date)
          .isPresent()) {
        continue;
      }
      SiBatch batch =
          batches.save(
              new SiBatch(
                  first.getCompanyId(),
                  first.getRecipientCode(),
                  first.getRecipientName(),
                  date,
                  items));
      audit.record(
          ENTITY,
          reference(batch),
          AuditAction.CREATE,
          "Generated with " + batch.getItemCount() + " service invoices");
      send(batch, items);
      generated.add(batch);
    }
    return generated;
  }

  /**
   * Sends a failed batch again.
   *
   * @param id batch
   * @return batch
   */
  public SiBatch resend(Long id) {
    SiBatch batch = get(id);
    batch.retry();
    send(batch, serviceInvoices.findAllById(batch.getServiceInvoiceIds()));
    audit.record(ENTITY, reference(batch), AuditAction.UPDATE, "Sent again");
    return batch;
  }

  /**
   * One batch.
   *
   * @param id batch
   * @return batch
   */
  @Transactional(readOnly = true)
  public SiBatch get(Long id) {
    return batches.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Batches of a company over a period, with the outcome of the e-mails delivered since by the
   * outbox recorded first.
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return batches, newest first
   */
  public List<SiBatch> list(Long companyId, LocalDate from, LocalDate to) {
    List<SiBatch> found =
        batches.findByCompanyIdAndBatchDateBetweenOrderByBatchDateDescInsurerNameAsc(
            companyId, from, to);
    found.forEach(this::recordOutcome);
    return found;
  }

  /**
   * The service invoices of a batch.
   *
   * @param id batch
   * @return service invoices
   */
  @Transactional(readOnly = true)
  public List<ServiceInvoice> items(Long id) {
    return serviceInvoices.findAllById(get(id).getServiceInvoiceIds());
  }

  /**
   * Records the outcome of the batch's e-mail once the outbox has delivered it or given up an
   * attempt.
   *
   * @param batch batch
   */
  void recordOutcome(SiBatch batch) {
    boolean waiting =
        batch.getStatus() == SiBatchStatus.GENERATED || batch.getStatus() == SiBatchStatus.RETRIED;
    if (!waiting || batch.getMessageId() == null) {
      return;
    }
    OutboundMessage message = messages.get(batch.getMessageId());
    if (message.getStatus() == MessageStatus.SENT) {
      batch.sent(clock.instant());
    } else if (message.getLastError() != null) {
      batch.failed(message.getLastError());
    }
  }

  private void send(SiBatch batch, List<ServiceInvoice> items) {
    String email = parties.getByCode(batch.getCompanyId(), batch.getInsurerCode()).getEmail();
    if (email == null || email.isBlank()) {
      batch.failed("No billing e-mail address for " + batch.getInsurerCode());
      return;
    }
    List<MessageFile> files =
        items.stream()
            .map(si -> new MessageFile(si.getSiNo() + ".pdf", PDF, documents.content(si)))
            .toList();
    Long messageId =
        messages
            .queueEmail(
                new OutboundEmail(
                    batch.getCompanyId(),
                    ServiceInvoiceDispatch.PURPOSE,
                    List.of(email.strip()),
                    null,
                    "Service invoices of " + batch.getBatchDate(),
                    body(batch, items),
                    files,
                    null,
                    new RecordLink(ENTITY, String.valueOf(batch.getId()), reference(batch))))
            .messageId();
    batch.queued(email.strip(), messageId);
    events.publishEvent(new SiBatchQueued(batch.getId(), messageId));
  }

  /**
   * Body of the batch e-mail: the list of the service invoices with their net amounts.
   *
   * @param batch batch
   * @param items service invoices
   * @return text
   */
  static String body(SiBatch batch, List<ServiceInvoice> items) {
    StringBuilder text =
        new StringBuilder(BODY_CAPACITY)
            .append("Dear ")
            .append(batch.getInsurerName())
            .append(",\n\nPlease find attached our ")
            .append(items.size())
            .append(" service invoices issued on ")
            .append(batch.getBatchDate())
            .append(":\n\n");
    for (ServiceInvoice si : items) {
      text.append("  ")
          .append(si.getSiNo())
          .append(si.getInvoiceNo() == null ? "" : " (invoice " + si.getInvoiceNo() + ")")
          .append("  ")
          .append(si.getCurrency())
          .append(' ')
          .append(si.getNetAmount().toPlainString())
          .append('\n');
    }
    return text.append("\nThank you.").toString();
  }

  private static String reference(SiBatch batch) {
    return batch.getInsurerCode() + " " + batch.getBatchDate();
  }
}
