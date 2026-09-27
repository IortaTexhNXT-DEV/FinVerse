package com.iortatechnxt.brokerverse.collections.files.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Frequency;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Spec;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Status;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFileRepository;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publication of the Collections files (BRCLXN.024-029, 045): a registered report is generated,
 * archived as a GENERATE run with the time from which it may be downloaded and recorded with its
 * period and scope, once per report, period and scope. Users with {@code CLX_EXPORT} are told the
 * file is ready ({@code CLX_FILE_READY}); a failure is recorded and raises {@code
 * CLX_FILE_NOT_PUBLISHED}. Each file is generated in its own transaction.
 */
@Service
public class ScheduledFileService {

  /** Alert of a file that could not be generated. */
  public static final String NOT_PUBLISHED = "CLX_FILE_NOT_PUBLISHED";

  private static final String EXPORT_PERMISSION = "CLX_EXPORT";
  private static final Duration READY_WINDOW = Duration.ofDays(7);

  private final ScheduledFileRepository files;
  private final ReportService reports;
  private final NotificationService notifications;
  private final AlertService alerts;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param files published files
   * @param reports report generation
   * @param notifications notifications
   * @param alerts alerts
   * @param txManager transactions
   * @param clock clock
   */
  public ScheduledFileService(
      ScheduledFileRepository files,
      ReportService reports,
      NotificationService notifications,
      AlertService alerts,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.files = files;
    this.reports = reports;
    this.notifications = notifications;
    this.alerts = alerts;
    this.tx = new TransactionTemplate(txManager);
    this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  /**
   * Publishes a file unless it was already published for the period and scope; a failed earlier
   * attempt is retried.
   *
   * @param companyId company
   * @param spec report, period and scope
   * @param params report parameters
   * @param availableFrom when users may download it, null = at once
   * @return the file (published or failed)
   */
  public ScheduledFile publish(
      Long companyId, Spec spec, Map<String, String> params, Instant availableFrom) {
    String scope = spec.scope() == null ? "" : spec.scope();
    Optional<ScheduledFile> existing =
        files.findByCompanyIdAndReportCodeAndPeriodKeyAndScope(
            companyId, spec.reportCode(), spec.periodKey(), scope);
    if (existing.isPresent() && existing.get().getStatus() == Status.PUBLISHED) {
      return existing.get();
    }
    existing.ifPresent(failed -> tx.executeWithoutResult(s -> files.deleteById(failed.getId())));
    try {
      return required(tx.execute(s -> generate(companyId, spec, params, availableFrom)));
    } catch (RuntimeException ex) {
      String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
      alerts.raise(
          NOT_PUBLISHED,
          new AlertFacts(
              companyId,
              null,
              "CollectionFile",
              spec.reportCode() + ":" + spec.periodKey(),
              spec.reportCode() + " for " + spec.periodKey() + " was not published: " + message,
              null,
              NOT_PUBLISHED + ":" + companyId + ":" + spec.reportCode() + ":" + spec.periodKey()));
      return required(tx.execute(s -> files.save(new ScheduledFile(companyId, spec, message))));
    }
  }

  private ScheduledFile generate(
      Long companyId, Spec spec, Map<String, String> params, Instant availableFrom) {
    ExportFormat format = ExportFormat.XLSX;
    ReportRun run = reports.generate(spec.reportCode(), params, format, availableFrom);
    ScheduledFile saved =
        files.save(
            new ScheduledFile(companyId, spec, run.getId(), run.getRowCount(), availableFrom));
    if (spec.frequency() != Frequency.ON_REQUEST) {
      Instant from = availableFrom == null ? clock.instant() : availableFrom;
      notifications.notifyPermission(
          EXPORT_PERMISSION,
          new Notice(
              run.getTitle() + " " + spec.periodKey() + " is ready",
              "Available from " + from + (spec.scope() == null ? "" : " " + spec.scope()),
              "/collections/files",
              "CollectionFile",
              String.valueOf(saved.getId())),
          "CLX_FILE_READY");
    }
    return saved;
  }

  /**
   * Published files, newest first.
   *
   * @param companyId company
   * @param frequencies frequencies to list
   * @param pageable page
   * @return files
   */
  public Page<ScheduledFile> list(
      Long companyId, Collection<Frequency> frequencies, Pageable pageable) {
    Collection<Frequency> which = frequencies.isEmpty() ? List.of(Frequency.values()) : frequencies;
    return files.findByCompanyIdAndFrequencyInOrderByIdDesc(companyId, which, pageable);
  }

  /**
   * Counts the files published in the last week that are available now (home tile).
   *
   * @param companyId company
   * @return count
   */
  public long readyThisWeek(Long companyId) {
    Instant now = clock.instant();
    return files.countReady(companyId, Status.PUBLISHED, now.minus(READY_WINDOW), now);
  }

  private static ScheduledFile required(ScheduledFile file) {
    if (file == null) {
      throw new IllegalStateException("No file recorded");
    }
    return file;
  }
}
