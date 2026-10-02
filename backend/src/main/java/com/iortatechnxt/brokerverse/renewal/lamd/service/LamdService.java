package com.iortatechnxt.brokerverse.renewal.lamd.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.CheckTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.LamdLine;
import com.iortatechnxt.brokerverse.renewal.domain.LamdLineRepository;
import com.iortatechnxt.brokerverse.renewal.domain.LamdReport;
import com.iortatechnxt.brokerverse.renewal.domain.LamdReportRepository;
import com.iortatechnxt.brokerverse.renewal.domain.LamdStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.rules.service.ReevaluationService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import java.time.Clock;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LAMD loan reports (FR-RN-025): each line is matched by PN to the open renewals of the company; a
 * matched renewal is evaluated again, and its LAMD check routes it (Not for Renewal or transfer to
 * the RMU unit). Unmatched and ambiguous lines are kept for the RNW-LAMD-MATCH report.
 */
@Service
@Transactional
public class LamdService {

  private static final Set<RenewalStage> MATCHABLE =
      EnumSet.complementOf(
          EnumSet.of(RenewalStage.RENEWED, RenewalStage.CLOSED, RenewalStage.EXTRACTED));

  private final LamdReportRepository reports;
  private final LamdLineRepository lines;
  private final RenewalCandidateRepository candidates;
  private final ReevaluationService reevaluation;
  private final RenewalNotices notices;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param reports reports
   * @param lines lines
   * @param candidates renewals
   * @param reevaluation checks
   * @param notices notifications
   * @param numbers document numbers
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public LamdService(
      LamdReportRepository reports,
      LamdLineRepository lines,
      RenewalCandidateRepository candidates,
      ReevaluationService reevaluation,
      RenewalNotices notices,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.reports = reports;
    this.lines = lines;
    this.candidates = candidates;
    this.reevaluation = reevaluation;
    this.notices = notices;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records and routes one line of a report (the report is created with its first line).
   *
   * @param companyId company
   * @param jobNo upload job
   * @param header report type and period
   * @param rowNo row of the file
   * @param loan loan
   * @return the line
   */
  public LamdLine line(Long companyId, String jobNo, Header header, int rowNo, LamdLine.Loan loan) {
    LamdReport report =
        reports
            .findByJobNo(jobNo)
            .orElseGet(
                () ->
                    reports.save(
                        new LamdReport(
                            companyId,
                            numbers.next("LMD-" + BusinessClock.today(clock).getYear()),
                            header.type(),
                            header.period(),
                            jobNo)));
    List<RenewalCandidate> matches = match(companyId, loan.pnNo());
    String outcome =
        matches.isEmpty()
            ? LamdLine.UNMATCHED
            : matches.size() > 1 ? LamdLine.AMBIGUOUS : LamdLine.MATCHED;
    RenewalCandidate c = matches.size() == 1 ? matches.get(0) : null;
    LamdLine line =
        lines.save(
            new LamdLine(report.getId(), rowNo, loan, c == null ? null : c.getId(), outcome));
    report.count(c != null);
    if (c == null) {
      line.routed(
          null,
          matches.isEmpty()
              ? "No open renewal has PN " + loan.pnNo()
              : "PN " + loan.pnNo() + " matches several renewals");
      return line;
    }
    RenewalStage before = c.getStage();
    reevaluation.reevaluate(c, CheckTrigger.UPLOAD);
    String routing = routing(c, before);
    line.routed(
        c.getStage() == before ? null : c.getStage().name(), c.getRenewalRef() + ": " + routing);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "LAMD "
            + label(loan.status())
            + " for PN "
            + loan.pnNo()
            + " ("
            + report.getReportNo()
            + ")");
    if (c.getOwnerUnit() != null) {
      notices.teamLeaders(
          companyId,
          c.getOwnerUnit(),
          RenewalCodes.EVENT_ASSIGNED,
          c,
          new RenewalNotices.Text(c.getRenewalRef() + ": LAMD " + label(loan.status()), routing));
    }
    return line;
  }

  /**
   * The reports of a company, newest first.
   *
   * @param companyId company
   * @return reports
   */
  @Transactional(readOnly = true)
  public List<LamdReport> reports(Long companyId) {
    return reports.findByCompanyIdOrderByIdDesc(companyId);
  }

  /**
   * The lines of a report.
   *
   * @param companyId company
   * @param reportNo report number
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<LamdLine> lines(Long companyId, String reportNo) {
    LamdReport report =
        reports.findByCompanyIdOrderByIdDesc(companyId).stream()
            .filter(r -> r.getReportNo().equals(reportNo))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("LAMD report", reportNo));
    return lines.findByReportIdOrderByRowNoAsc(report.getId());
  }

  private List<RenewalCandidate> match(Long companyId, String pn) {
    String key = pn.strip();
    return candidates.findOpenByPn(MATCHABLE, "%" + key + "%").stream()
        .filter(c -> Objects.equals(c.getCompanyId(), companyId))
        .filter(c -> c.getSnapshot().pnNos() != null)
        .filter(
            c ->
                Arrays.stream(c.getSnapshot().pnNos().split(","))
                    .map(String::strip)
                    .anyMatch(key::equals))
        .toList();
  }

  private static String routing(RenewalCandidate c, RenewalStage before) {
    if (c.getStage() == before) {
      return "Checks run again; stage " + c.getStage().label();
    }
    return "Moved from " + before.label() + " to " + c.getStage().label();
  }

  private static String label(LamdStatus status) {
    return status == LamdStatus.PAID_OFF ? "loan fully paid" : "loan under RMU";
  }

  /**
   * Header of a report.
   *
   * @param type PAID_OFF or RMU
   * @param period month yyyy-MM
   */
  public record Header(LamdStatus type, String period) {}
}
