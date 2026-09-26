package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.domain.ReportRun.RunFile;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import com.iortatechnxt.brokerverse.screening.cases.service.CaseTimeline;
import com.iortatechnxt.brokerverse.screening.config.service.ActiveConfig;
import com.iortatechnxt.brokerverse.screening.config.service.StrLayout;
import com.iortatechnxt.brokerverse.screening.str.domain.StrExtraction;
import com.iortatechnxt.brokerverse.screening.str.domain.StrExtractionRepository;
import com.iortatechnxt.brokerverse.screening.str.domain.StrRepository;
import com.iortatechnxt.brokerverse.screening.str.domain.StrStatus;
import com.iortatechnxt.brokerverse.screening.str.domain.StrTransaction.Line;
import com.iortatechnxt.brokerverse.screening.str.domain.SuspiciousTransactionReport;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extracts the AML Committee-approved STRs (SNSRP-706; FR-SS-071): only STRs with an APPROVE_STR
 * decision in the period (Philippine days of the committee decision) and not yet extracted are
 * written with the STR layout in force and saved through {@link StrFileSink}; the extraction
 * (batch, period, count, file hash, user, time) is recorded and audited and the STRs become
 * EXTRACTED. A re-extraction of STRs already extracted needs a reason and is recorded as such.
 */
@Service
@Transactional
public class StrExtractionService {

  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

  private final StrRepository strs;
  private final StrExtractionRepository extractions;
  private final StrService strService;
  private final ScreeningCaseRepository cases;
  private final ActiveConfig config;
  private final StrFileSink sink;
  private final ReportArchiveService archive;
  private final DocumentNumberService numbers;
  private final CaseTimeline timeline;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param strs STRs
   * @param extractions extractions
   * @param strService STR values
   * @param cases cases
   * @param config active configuration (STR layout)
   * @param sink file destination
   * @param archive report archive (download)
   * @param numbers batch numbers
   * @param timeline case timeline
   * @param currentUser current user
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the extraction
  public StrExtractionService(
      StrRepository strs,
      StrExtractionRepository extractions,
      StrService strService,
      ScreeningCaseRepository cases,
      ActiveConfig config,
      StrFileSink sink,
      ReportArchiveService archive,
      DocumentNumberService numbers,
      CaseTimeline timeline,
      CurrentUser currentUser,
      AuditTrailService audit,
      Clock clock) {
    this.strs = strs;
    this.extractions = extractions;
    this.strService = strService;
    this.cases = cases;
    this.config = config;
    this.sink = sink;
    this.archive = archive;
    this.numbers = numbers;
    this.timeline = timeline;
    this.currentUser = currentUser;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The STRs an extraction of a period would hold.
   *
   * @param companyId company
   * @param from period start
   * @param to period end
   * @param reason re-extraction reason, blank for a first extraction
   * @return the STRs
   */
  @Transactional(readOnly = true)
  public List<SuspiciousTransactionReport> preview(
      Long companyId, LocalDate from, LocalDate to, String reason) {
    requirePeriod(from, to);
    Set<StrStatus> statuses =
        blank(reason)
            ? EnumSet.of(StrStatus.APPROVED)
            : EnumSet.of(StrStatus.APPROVED, StrStatus.EXTRACTED);
    return strs.decidedIn(
        companyId,
        statuses,
        from.atStartOfDay(MANILA).toInstant(),
        to.plusDays(1).atStartOfDay(MANILA).toInstant());
  }

  /**
   * Extracts the STRs of a period.
   *
   * @param companyId company
   * @param from period start
   * @param to period end
   * @param reason re-extraction reason, blank for a first extraction
   * @return the extraction
   */
  public StrExtraction extract(Long companyId, LocalDate from, LocalDate to, String reason) {
    List<SuspiciousTransactionReport> selected = preview(companyId, from, to, reason);
    if (selected.isEmpty()) {
      throw new BusinessRuleException(
          "SCR_NOTHING_TO_EXTRACT", "No committee-approved STR to extract");
    }
    LocalDate today = LocalDate.now(clock);
    StrLayout layout =
        config
            .strLayout(companyId, today)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "SCR_NO_ACTIVE_CONFIG", "No STR layout is in force; ask Compliance"));
    byte[] content = StrFileWriter.write(layout, selected.stream().map(this::row).toList());
    String batchNo = numbers.next("STRX-" + today.getYear());
    String fileName = batchNo + "." + StrFileWriter.extension(layout);
    String text = blank(reason) ? null : reason.strip();
    Set<Long> ids =
        new LinkedHashSet<>(selected.stream().map(SuspiciousTransactionReport::getId).toList());
    Instant now = clock.instant();
    StrExtraction extraction =
        extractions.save(
            new StrExtraction(
                batchNo,
                companyId,
                new StrExtraction.Batch(
                    from, to, layout.version().id(), fileName, sha256(content), text, ids),
                currentUser.username(),
                now));
    StrFileSink.Delivered delivered =
        sink.deliver(
            new StrFileSink.StrFile(
                fileName,
                "text/csv",
                content,
                List.of(batchNo, "Period " + from + " to " + to, selected.size() + " STR(s)"),
                selected.size()));
    extraction.archived(delivered.reportRunId());
    markExtracted(selected, extraction, text, now);
    audit.record(
        "ScreeningStrExtraction",
        batchNo,
        AuditAction.EXPORT,
        auditText(text, selected.size(), extraction, delivered.location()));
    return extraction;
  }

  private void markExtracted(
      List<SuspiciousTransactionReport> selected,
      StrExtraction extraction,
      String reason,
      Instant now) {
    for (SuspiciousTransactionReport str : selected) {
      str.extracted(extraction.getId(), now);
      cases
          .findById(str.getCaseId())
          .ifPresent(
              c ->
                  timeline.record(
                      c,
                      CaseEventType.STR_EXTRACTED,
                      EventFacts.change(null, extraction.getBatchNo(), null, reason)));
    }
  }

  private static String auditText(
      String reason, int count, StrExtraction extraction, String location) {
    return (reason == null ? "Extracted " : "Re-extracted (" + reason + ") ")
        + count
        + " STR(s) of "
        + extraction.getPeriodFrom()
        + " to "
        + extraction.getPeriodTo()
        + " to "
        + extraction.getFileName()
        + ", SHA-256 "
        + extraction.getSha256()
        + " ("
        + location
        + ")";
  }

  private Map<String, String> row(SuspiciousTransactionReport str) {
    Map<String, String> row = new LinkedHashMap<>(strService.values(str.getId()));
    row.put("STR_NO", str.getStrNo());
    row.put("SUBJECT_CODE", str.getSubjectCode());
    row.putIfAbsent("SUBJECT_NAME", str.getSubjectName());
    row.put("REASON_CODE", String.join(";", str.reasons()));
    List<Line> lines = strService.transactions(str.getId());
    row.putIfAbsent(
        "TOTAL_AMOUNT",
        lines.stream().map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add).toPlainString());
    cases
        .findById(str.getCaseId())
        .map(ScreeningCase::getCaseNo)
        .ifPresent(no -> row.put("CASE_NO", no));
    row.put(
        "COMMITTEE_DATE",
        str.getCommitteeDecidedAt() == null
            ? ""
            : LocalDate.ofInstant(str.getCommitteeDecidedAt(), MANILA).toString());
    return row;
  }

  /**
   * The extractions of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return extractions
   */
  @Transactional(readOnly = true)
  public Page<StrExtraction> extractions(Long companyId, Pageable pageable) {
    return extractions.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * The file of an extraction (from the report archive).
   *
   * @param id the extraction
   * @return the file
   */
  public RunFile file(Long id) {
    StrExtraction extraction =
        extractions
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("STR extraction", id));
    if (extraction.getReportRunId() == null) {
      throw new BusinessRuleException(
          "SCR_STR_FILE_ELSEWHERE",
          "The file of " + extraction.getBatchNo() + " was saved outside BIBS");
    }
    return archive.file(extraction.getReportRunId());
  }

  private static void requirePeriod(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new BusinessRuleException("SCR_PERIOD_REQUIRED", "Enter the period");
    }
    if (to.isBefore(from)) {
      throw new BusinessRuleException(
          "SCR_PERIOD_INVALID", "The end date must be on or after the start date");
    }
  }

  private static String sha256(byte[] content) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 unavailable", ex);
    }
  }

  private static boolean blank(String text) {
    return text == null || text.isBlank();
  }
}
