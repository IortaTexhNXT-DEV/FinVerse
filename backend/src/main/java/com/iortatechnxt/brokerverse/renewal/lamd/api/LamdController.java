package com.iortatechnxt.brokerverse.renewal.lamd.api;

import com.iortatechnxt.brokerverse.renewal.domain.LamdLine;
import com.iortatechnxt.brokerverse.renewal.domain.LamdReport;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.lamd.service.LamdService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** LAMD loan reports and their match results (FR-RN-025); the upload is the bulk handler. */
@RestController
@RequestMapping("/api/v1/renewal/lamd-reports")
@PreAuthorize("hasAnyAuthority('RNW_LAMD_UPLOAD','RNW_PROCESS','RNW_REVIEW')")
public class LamdController {

  private final LamdService lamd;
  private final RenewalCandidateRepository candidates;

  /**
   * Creates the controller.
   *
   * @param lamd LAMD reports
   * @param candidates renewals (references)
   */
  public LamdController(LamdService lamd, RenewalCandidateRepository candidates) {
    this.lamd = lamd;
    this.candidates = candidates;
  }

  /**
   * The reports of a company.
   *
   * @param companyId company
   * @return reports, newest first
   */
  @GetMapping
  public List<ReportView> reports(@RequestParam Long companyId) {
    return lamd.reports(companyId).stream().map(ReportView::of).toList();
  }

  /**
   * The lines of a report.
   *
   * @param companyId company
   * @param reportNo report number
   * @return lines
   */
  @GetMapping("/{reportNo}/lines")
  @Transactional(readOnly = true)
  public List<LineView> lines(@RequestParam Long companyId, @PathVariable String reportNo) {
    return lamd.lines(companyId, reportNo).stream()
        .map(
            l ->
                LineView.of(
                    l,
                    l.getCandidateId() == null
                        ? null
                        : candidates
                            .findById(l.getCandidateId())
                            .map(RenewalCandidate::getRenewalRef)
                            .orElse(null)))
        .toList();
  }

  /**
   * A report.
   *
   * @param reportNo number
   * @param type PAID_OFF or RMU
   * @param period month
   * @param jobNo upload job
   * @param lines lines
   * @param matched matched lines
   * @param by uploader
   * @param at upload time
   */
  public record ReportView(
      String reportNo,
      String type,
      String period,
      String jobNo,
      int lines,
      int matched,
      String by,
      Instant at) {

    static ReportView of(LamdReport r) {
      return new ReportView(
          r.getReportNo(),
          r.getReportType().name(),
          r.getPeriod(),
          r.getJobNo(),
          r.getLineCount(),
          r.getMatchedCount(),
          r.getCreatedBy(),
          r.getCreatedAt());
    }
  }

  /**
   * A line.
   *
   * @param rowNo row
   * @param pnNo PN
   * @param status loan status
   * @param statusDate status date
   * @param borrower borrower
   * @param match match outcome
   * @param renewalRef matched renewal
   * @param routing routing applied
   * @param message message
   */
  public record LineView(
      int rowNo,
      String pnNo,
      String status,
      LocalDate statusDate,
      String borrower,
      String match,
      String renewalRef,
      String routing,
      String message) {

    static LineView of(LamdLine l, String ref) {
      return new LineView(
          l.getRowNo(),
          l.getPnNo(),
          l.getLoanStatus().name(),
          l.getStatusDate(),
          l.getBorrower(),
          l.getMatchOutcome(),
          ref,
          l.getRouting(),
          l.getMessage());
    }
  }
}
