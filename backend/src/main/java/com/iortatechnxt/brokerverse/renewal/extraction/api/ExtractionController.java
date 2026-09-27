package com.iortatechnxt.brokerverse.renewal.extraction.api;

import com.iortatechnxt.brokerverse.common.api.PageResponse;
import com.iortatechnxt.brokerverse.renewal.domain.ApprovalStatus;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.renewal.domain.RaSentRequest;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalExtractionRunRepository;
import com.iortatechnxt.brokerverse.renewal.extraction.service.ExtractionService;
import com.iortatechnxt.brokerverse.renewal.extraction.service.GoLiveService;
import com.iortatechnxt.brokerverse.renewal.extraction.service.RaAlreadySentService;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Extraction (FR-RN-001, 002, 070): the runs, a manual extraction of an expiry range, the go-live
 * take-over and the maker-checker corrections of the Renewal Advices already sent before go-live.
 */
@RestController
@RequestMapping("/api/v1/renewal/extraction")
public class ExtractionController {

  private static final String VIEW = "hasAuthority('RNW_VIEW')";
  private static final String EXTRACT = "hasAuthority('RNW_EXTRACT')";
  private static final String SETUP = "hasAuthority('RNW_SETUP')";
  private static final String RA_SEND = "hasAuthority('RNW_RA_SEND')";
  private static final int MAX_PAGE = 100;

  private final ExtractionService extraction;
  private final GoLiveService goLive;
  private final RaAlreadySentService raSent;
  private final RenewalExtractionRunRepository runs;
  private final LegacyPolicySource legacy;

  /**
   * Creates the controller.
   *
   * @param extraction extraction
   * @param goLive go-live take-over
   * @param raSent Renewal Advices already sent
   * @param runs extraction runs
   * @param legacy legacy policy source
   */
  public ExtractionController(
      ExtractionService extraction,
      GoLiveService goLive,
      RaAlreadySentService raSent,
      RenewalExtractionRunRepository runs,
      LegacyPolicySource legacy) {
    this.extraction = extraction;
    this.goLive = goLive;
    this.raSent = raSent;
    this.runs = runs;
    this.legacy = legacy;
  }

  /**
   * Extraction runs, newest first.
   *
   * @param companyId company
   * @param page page
   * @param size size
   * @return runs
   */
  @GetMapping("/runs")
  @PreAuthorize(VIEW)
  public PageResponse<RunView> runs(
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        runs.findByCompanyIdOrderByStartedAtDesc(
            companyId, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE))),
        RunView::of);
  }

  /**
   * Whether the legacy policy source is connected.
   *
   * @return connection state
   */
  @GetMapping("/source")
  @PreAuthorize(VIEW)
  public SourceView source() {
    return new SourceView(legacy.connected());
  }

  /**
   * Extracts the expiries of a range now (FR-RN-001).
   *
   * @param request range
   * @return the run
   */
  @PostMapping("/runs")
  @PreAuthorize(EXTRACT)
  public RunView extract(@Valid @RequestBody RangeRequest request) {
    return RunView.of(
        extraction.extractRange(request.companyId(), request.expiryFrom(), request.expiryTo()));
  }

  /**
   * Takes over the expiries of the go-live window from the legacy system (FR-RN-070).
   *
   * @param request go-live date
   * @return the run
   */
  @PostMapping("/go-live")
  @PreAuthorize(SETUP)
  public RunView takeOver(@Valid @RequestBody GoLiveRequest request) {
    return RunView.of(goLive.takeOver(request.companyId(), request.goLive()));
  }

  /**
   * Corrections of rejected rows of the Renewal Advices already sent.
   *
   * @param companyId company
   * @param status status (default PENDING)
   * @return corrections
   */
  @GetMapping("/ra-sent-corrections")
  @PreAuthorize(RA_SEND)
  public List<CorrectionView> corrections(
      @RequestParam Long companyId, @RequestParam(defaultValue = "PENDING") ApprovalStatus status) {
    return raSent.corrections(companyId, status).stream().map(CorrectionView::of).toList();
  }

  /**
   * Prepares a correction (maker).
   *
   * @param request corrected row
   * @return the correction
   */
  @PostMapping("/ra-sent-corrections")
  @PreAuthorize(RA_SEND)
  public CorrectionView prepare(@Valid @RequestBody CorrectionRequest request) {
    return CorrectionView.of(
        raSent.prepare(
            request.companyId(),
            new RaSentRequest.Row(
                request.legacyRef(),
                request.raDate(),
                request.raRef(),
                request.channel(),
                request.recipient()),
            new RaSentRequest.Source(request.jobNo(), request.rowNo()),
            request.correction()));
  }

  /**
   * Decides a correction (checker).
   *
   * @param id correction
   * @param request decision
   * @return the correction
   */
  @PostMapping("/ra-sent-corrections/{id}/decision")
  @PreAuthorize(RA_SEND)
  public CorrectionView decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
    return CorrectionView.of(raSent.decide(id, request.approve(), request.remarks()));
  }

  /**
   * An expiry range.
   *
   * @param companyId company
   * @param expiryFrom first expiry
   * @param expiryTo last expiry
   */
  public record RangeRequest(
      @NotNull Long companyId, @NotNull LocalDate expiryFrom, @NotNull LocalDate expiryTo) {}

  /**
   * The go-live take-over.
   *
   * @param companyId company
   * @param goLive go-live date
   */
  public record GoLiveRequest(@NotNull Long companyId, @NotNull LocalDate goLive) {}

  /**
   * A corrected row.
   *
   * @param companyId company
   * @param legacyRef legacy reference
   * @param raDate date the RA was sent
   * @param raRef RA reference
   * @param channel channel
   * @param recipient recipient
   * @param jobNo upload job, may be null
   * @param rowNo upload row, may be null
   * @param correction what was corrected
   */
  public record CorrectionRequest(
      @NotNull Long companyId,
      @NotBlank String legacyRef,
      @NotNull LocalDate raDate,
      String raRef,
      String channel,
      String recipient,
      String jobNo,
      Integer rowNo,
      @NotBlank String correction) {}

  /**
   * A decision.
   *
   * @param approve approve or reject
   * @param remarks remarks
   */
  public record DecisionRequest(boolean approve, String remarks) {}

  /**
   * Connection of the legacy policy source.
   *
   * @param connected connected
   */
  public record SourceView(boolean connected) {}

  /**
   * An extraction run.
   *
   * @param runNo run number
   * @param trigger trigger
   * @param expiryFrom first expiry
   * @param expiryTo last expiry
   * @param requestedBy user or job
   * @param counts read, new, existing, skipped and urgent
   * @param status status
   * @param message failure or summary
   * @param startedAt start
   * @param endedAt end
   */
  public record RunView(
      String runNo,
      String trigger,
      LocalDate expiryFrom,
      LocalDate expiryTo,
      String requestedBy,
      ExtractionRun.Counts counts,
      String status,
      String message,
      Instant startedAt,
      Instant endedAt) {

    static RunView of(ExtractionRun r) {
      return new RunView(
          r.getRunNo(),
          r.getTrigger().name(),
          r.getExpiryFrom(),
          r.getExpiryTo(),
          r.getRequestedBy(),
          new ExtractionRun.Counts(
              r.getReadCount(),
              r.getNewCount(),
              r.getExistingCount(),
              r.getSkippedCount(),
              r.getUrgentCount()),
          r.getStatus().name(),
          r.getMessage(),
          r.getStartedAt(),
          r.getEndedAt());
    }
  }

  /**
   * A correction.
   *
   * @param id id
   * @param legacyRef legacy reference
   * @param raDate RA date
   * @param raRef RA reference
   * @param jobNo upload job
   * @param rowNo upload row
   * @param correction correction
   * @param status status
   * @param preparedBy maker
   * @param decidedBy checker
   * @param decidedAt decision time
   * @param decisionRemarks remarks
   */
  public record CorrectionView(
      Long id,
      String legacyRef,
      LocalDate raDate,
      String raRef,
      String jobNo,
      Integer rowNo,
      String correction,
      String status,
      String preparedBy,
      String decidedBy,
      Instant decidedAt,
      String decisionRemarks) {

    static CorrectionView of(RaSentRequest r) {
      return new CorrectionView(
          r.getId(),
          r.getLegacyRef(),
          r.getRaDate(),
          r.getRaRef(),
          r.getJobNo(),
          r.getRowNo(),
          r.getCorrection(),
          r.getStatus().name(),
          r.getCreatedBy(),
          r.getDecidedBy(),
          r.getDecidedAt(),
          r.getDecisionRemarks());
    }
  }
}
