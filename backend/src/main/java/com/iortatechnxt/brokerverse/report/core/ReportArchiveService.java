package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.report.domain.ReportRun;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFacts;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.report.domain.ReportRunFile;
import com.iortatechnxt.brokerverse.report.domain.ReportRunFileRepository;
import com.iortatechnxt.brokerverse.report.domain.ReportRunRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Archive of generated reports (CSHID.017/018): every on-screen run and export of an archived
 * report is kept with its parameters, creator and time, exports with their file. Users see the runs
 * of the reports they may view; only users allowed to export a report download its files.
 */
@Service
@Transactional
public class ReportArchiveService {

  private static final String ENTITY = "ReportRun";

  private final ReportRunRepository runs;
  private final ReportRunFileRepository files;
  private final ReportRegistry registry;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param runs archived runs
   * @param files files of archived exports
   * @param registry report catalogue
   * @param audit audit trail
   */
  public ReportArchiveService(
      ReportRunRepository runs,
      ReportRunFileRepository files,
      ReportRegistry registry,
      AuditTrailService audit) {
    this.runs = runs;
    this.files = files;
    this.registry = registry;
    this.audit = audit;
  }

  /**
   * Archives an on-screen run of an archived report.
   *
   * @param metadata report
   * @param echo parameter echo
   * @param result result
   */
  public void viewed(ReportMetadata metadata, List<String> echo, ReportResult result) {
    if (metadata.archived()) {
      runs.save(ReportRun.viewed(facts(metadata, echo, result)));
    }
  }

  /**
   * Archives an export of an archived report with its file.
   *
   * @param metadata report
   * @param echo parameter echo
   * @param result result
   * @param file exported file
   */
  public void exported(
      ReportMetadata metadata, List<String> echo, ReportResult result, RunFile file) {
    if (metadata.archived()) {
      ReportRun run = runs.save(ReportRun.exported(facts(metadata, echo, result), file));
      files.save(new ReportRunFile(run.getId(), file.content()));
    }
  }

  /**
   * Archived runs of the reports the user may view, newest first.
   *
   * @param code one report, null for all
   * @param pageable page
   * @return runs
   */
  @Transactional(readOnly = true)
  public Page<ReportRun> runs(String code, Pageable pageable) {
    List<String> codes =
        registry.catalogue().stream()
            .filter(ReportMetadata::archived)
            .filter(ReportAccess::mayView)
            .map(ReportMetadata::code)
            .filter(c -> code == null || code.isBlank() || c.equals(code))
            .toList();
    return runs.findByReportCodeInOrderByIdDesc(codes.isEmpty() ? List.of("-") : codes, pageable);
  }

  /**
   * The file of an archived export; only for users allowed to export the report (CSHID.018).
   *
   * @param id run
   * @return file name, type and content
   */
  public RunFile file(Long id) {
    ReportRun run = runs.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    ReportMetadata metadata = registry.get(run.getReportCode()).metadata();
    if (!ReportAccess.mayExport(metadata)) {
      throw new AccessDeniedException("Not permitted to download report " + metadata.code());
    }
    ReportRunFile file =
        files.findById(id).orElseThrow(() -> new ResourceNotFoundException("Report file", id));
    audit.record(
        ENTITY, id, AuditAction.EXPORT, "Downloaded archived " + run.getFileName() + " again");
    return new RunFile(run.getFormat(), run.getFileName(), run.getContentType(), file.getContent());
  }

  private static RunFacts facts(ReportMetadata m, List<String> echo, ReportResult result) {
    return new RunFacts(
        m.code(), m.title(), m.category().name(), String.join("; ", echo), result.rows().size());
  }
}
