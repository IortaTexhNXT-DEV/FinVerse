package com.iortatechnxt.brokerverse.migration.archive.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.AttachmentService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.archive.domain.AccessLog;
import com.iortatechnxt.brokerverse.migration.archive.domain.AccessLogRepository;
import com.iortatechnxt.brokerverse.migration.archive.domain.ArchiveRecord;
import com.iortatechnxt.brokerverse.migration.archive.domain.ArchiveRecordRepository;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationCodes;
import com.iortatechnxt.brokerverse.migration.common.service.MigrationParameters;
import com.iortatechnxt.brokerverse.storage.service.FileDownload;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The Legacy Inquiry (DATA_MIGRATION_DESIGN section 16): search, read-only view, document download
 * and Excel export of the legacy archive. Each access is written to the append-only access log with
 * the user, time, source address, criteria, record keys, result count and the reason given; a
 * reason is required when {@code MIG_LEGACY_ACCESS_REASON_REQUIRED} is true, the export is limited
 * to {@code MIG_ARCHIVE_EXPORT_MAX_ROWS} and a user exporting more than {@code
 * MIG_ACCESS_EXPORT_ALERT_ROWS} records in a day raises {@code MIG_LEGACY_ACCESS_UNUSUAL}.
 */
@Service
@Transactional
public class LegacyInquiryService {

  private static final int MAX_KEYS_LOGGED = 50;
  private static final Sort ORDER = Sort.by(Sort.Order.desc("documentDate"), Sort.Order.asc("id"));

  private final ArchiveRecordRepository records;
  private final AccessLogRepository log;
  private final AttachmentService attachments;
  private final MigrationParameters parameters;
  private final AlertService alerts;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param records archive records
   * @param log access log
   * @param attachments legacy documents
   * @param parameters migration parameters
   * @param alerts alerts
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators
  public LegacyInquiryService(
      ArchiveRecordRepository records,
      AccessLogRepository log,
      AttachmentService attachments,
      MigrationParameters parameters,
      AlertService alerts,
      CurrentUser currentUser,
      Clock clock) {
    this.records = records;
    this.log = log;
    this.attachments = attachments;
    this.parameters = parameters;
    this.alerts = alerts;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Searches the archive.
   *
   * @param companyId company
   * @param criteria criteria
   * @param reason reason of the access
   * @param page page
   * @return records, newest first
   */
  public Page<ArchiveRecord> search(
      Long companyId, ArchiveCriteria criteria, AccessLog.Reason reason, Pageable page) {
    requireReason(reason);
    requireCriteria(criteria);
    Page<ArchiveRecord> found =
        records.findAll(
            ArchiveSpecifications.matching(companyId, criteria),
            PageRequest.of(page.getPageNumber(), page.getPageSize(), ORDER));
    write(companyId, "SEARCH", criteria.describe(), keys(found.getContent()), found, reason);
    return found;
  }

  private void write(
      Long companyId,
      String action,
      String criteria,
      String keys,
      Page<ArchiveRecord> found,
      AccessLog.Reason reason) {
    write(
        companyId,
        action,
        new AccessLog.What(criteria, keys, (int) found.getTotalElements()),
        reason);
  }

  /**
   * An archive record with its documents; the view is logged.
   *
   * @param id record
   * @param reason reason of the access
   * @return the record and its documents
   */
  public RecordView view(Long id, AccessLog.Reason reason) {
    requireReason(reason);
    ArchiveRecord record = record(id);
    write(record.getCompanyId(), "VIEW", new AccessLog.What(null, key(record), 1), reason);
    return new RecordView(record, documents(record));
  }

  /**
   * A legacy document of a record; the download is logged.
   *
   * @param id record
   * @param attachmentId document
   * @param reason reason of the access
   * @return the file
   */
  public FileDownload download(Long id, Long attachmentId, AccessLog.Reason reason) {
    requireReason(reason);
    ArchiveRecord record = record(id);
    Attachment document =
        documents(record).stream()
            .filter(a -> a.getId().equals(attachmentId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Legacy document", attachmentId));
    write(
        record.getCompanyId(),
        "DOWNLOAD",
        new AccessLog.What(document.getFileName(), key(record), 1),
        reason);
    return attachments.downloadable(document.getId());
  }

  /**
   * The records of an export (at most {@code MIG_ARCHIVE_EXPORT_MAX_ROWS}); the export is logged
   * and a day's exports above the alert limit raise the unusual-access alert.
   *
   * @param companyId company
   * @param criteria criteria
   * @param reason reason of the access
   * @return the records
   */
  public List<ArchiveRecord> export(
      Long companyId, ArchiveCriteria criteria, AccessLog.Reason reason) {
    requireReason(reason);
    requireCriteria(criteria);
    int max = parameters.archiveExportMaxRows();
    Page<ArchiveRecord> found =
        records.findAll(
            ArchiveSpecifications.matching(companyId, criteria), PageRequest.of(0, max, ORDER));
    if (found.getTotalElements() > max) {
      throw new BusinessRuleException(
          "MIG_EXPORT_TOO_LARGE",
          found.getTotalElements()
              + " records match; narrow the search to at most "
              + max
              + " records to export");
    }
    write(companyId, "EXPORT", criteria.describe(), keys(found.getContent()), found, reason);
    checkUnusual(companyId);
    return found.getContent();
  }

  private void checkUnusual(Long companyId) {
    String user = currentUser.username();
    Instant dayStart = BusinessClock.today(clock).atStartOfDay(BusinessClock.zone()).toInstant();
    long exported = log.exportedSince(user, dayStart);
    if (exported > parameters.accessExportAlertRows()) {
      alerts.raise(
          MigrationCodes.ALERT_ACCESS_UNUSUAL,
          new AlertFacts(
              companyId,
              null,
              "MigAccessLog",
              user,
              "User "
                  + user
                  + " exported "
                  + exported
                  + " legacy archive records today (limit "
                  + parameters.accessExportAlertRows()
                  + ")",
              BigDecimal.valueOf(exported),
              MigrationCodes.ALERT_ACCESS_UNUSUAL + ":" + user + ":" + BusinessClock.today(clock)));
    }
  }

  /**
   * The access log (Compliance).
   *
   * @param companyId company
   * @param filter user, action and period
   * @param page page
   * @return entries, newest first
   */
  @Transactional(readOnly = true)
  public Page<AccessLog> accessLog(Long companyId, AccessLogFilter filter, Pageable page) {
    return log.findAll(
        ArchiveSpecifications.log(companyId, filter),
        PageRequest.of(
            page.getPageNumber(),
            page.getPageSize(),
            Sort.by(Sort.Order.desc("accessedAt"), Sort.Order.desc("id"))));
  }

  /**
   * The settings the inquiry screen shows: whether a reason is asked, the export limit and the
   * addresses of the read-only legacy applications.
   *
   * @return settings
   */
  @Transactional(readOnly = true)
  public Settings settings() {
    return new Settings(
        parameters.accessReasonRequired(),
        parameters.archiveExportMaxRows(),
        List.of("EBIX", "QPS", "ISYS", "CMS").stream()
            .filter(s -> !parameters.legacyLink(s).isEmpty())
            .map(s -> new LegacyLink(s, parameters.legacyLink(s)))
            .toList());
  }

  private ArchiveRecord record(Long id) {
    return records
        .findById(id)
        .filter(r -> !r.isRolledBack())
        .orElseThrow(() -> new ResourceNotFoundException(MigrationCodes.ENTITY_ARCHIVE, id));
  }

  private List<Attachment> documents(ArchiveRecord record) {
    return attachments.list(
        new AttachmentTarget(MigrationCodes.ENTITY_ARCHIVE, String.valueOf(record.getId())));
  }

  private void requireReason(AccessLog.Reason reason) {
    boolean blank = reason == null || reason.code() == null || reason.code().isBlank();
    if (parameters.accessReasonRequired() && blank) {
      throw new BusinessRuleException(
          "MIG_ACCESS_REASON_REQUIRED", "Give the reason for opening the legacy archive");
    }
  }

  private static void requireCriteria(ArchiveCriteria criteria) {
    if (criteria.empty()) {
      throw new BusinessRuleException(
          "MIG_CRITERIA_REQUIRED",
          "Give a client, policy, invoice, receipt or claim, a record type or a date range");
    }
  }

  private void write(Long companyId, String action, AccessLog.What what, AccessLog.Reason reason) {
    AccessLog.Reason given = reason == null ? new AccessLog.Reason(null, null) : reason;
    log.save(
        new AccessLog(
            companyId,
            new AccessLog.Who(currentUser.username(), clock.instant(), sourceAddress()),
            action,
            what,
            given));
  }

  private static String keys(List<ArchiveRecord> found) {
    return found.stream()
        .limit(MAX_KEYS_LOGGED)
        .map(LegacyInquiryService::key)
        .collect(Collectors.joining(","));
  }

  private static String key(ArchiveRecord r) {
    return r.getSourceSystem() + ":" + r.getRecordType() + ":" + r.getLegacyKey();
  }

  private static String sourceAddress() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet
        ? servlet.getRequest().getRemoteAddr()
        : null;
  }

  /**
   * An archive record with its documents.
   *
   * @param record record
   * @param documents legacy documents, oldest first
   */
  public record RecordView(ArchiveRecord record, List<Attachment> documents) {

    /** Defensive copy. */
    public RecordView {
      documents = List.copyOf(documents);
    }
  }

  /**
   * The settings of the inquiry screen.
   *
   * @param reasonRequired a reason is asked before the archive is opened
   * @param exportMaxRows largest export
   * @param links read-only legacy applications
   */
  public record Settings(boolean reasonRequired, int exportMaxRows, List<LegacyLink> links) {

    /** Defensive copy. */
    public Settings {
      links = List.copyOf(links);
    }
  }

  /**
   * The address of a read-only legacy application.
   *
   * @param system legacy system
   * @param url address
   */
  public record LegacyLink(String system, String url) {}
}
