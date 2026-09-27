package com.iortatechnxt.brokerverse.eb.comparative.api;

import com.iortatechnxt.brokerverse.eb.comparative.api.dto.ComparativeDtos.CommentRequest;
import com.iortatechnxt.brokerverse.eb.comparative.api.dto.ComparativeDtos.CommentView;
import com.iortatechnxt.brokerverse.eb.comparative.api.dto.ComparativeDtos.ComparativeSummary;
import com.iortatechnxt.brokerverse.eb.comparative.api.dto.ComparativeDtos.ComparativeView;
import com.iortatechnxt.brokerverse.eb.comparative.api.dto.ComparativeDtos.DecisionView;
import com.iortatechnxt.brokerverse.eb.comparative.api.dto.ComparativeDtos.LineView;
import com.iortatechnxt.brokerverse.eb.comparative.api.dto.ComparativeDtos.RecommendationRequest;
import com.iortatechnxt.brokerverse.eb.comparative.api.dto.ComparativeDtos.RemarksRequest;
import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativeApproval;
import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativeExport;
import com.iortatechnxt.brokerverse.eb.comparative.service.ComparativePresenter;
import com.iortatechnxt.brokerverse.eb.comparative.service.EbComparativeService;
import com.iortatechnxt.brokerverse.eb.domain.EbComparative;
import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.service.EbRecords;
import java.util.List;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The comparative of a cycle (FR-EB-041 to 043): build, recommend, submit, sign off, threshold
 * approval, return, present, comments and export.
 */
@RestController
@RequestMapping("/api/v1/eb")
@Transactional
public class ComparativeController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String MARKET = "hasAuthority('EB_MARKET')";
  private static final String SIGN = "hasAuthority('EB_COMPARATIVE_APPROVE')";
  private static final String APPROVERS =
      "hasAnyAuthority('EB_COMPARATIVE_APPROVE', 'EB_THRESHOLD_APPROVE')";
  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final EbComparativeService comparatives;
  private final ComparativeApproval approval;
  private final ComparativePresenter presenter;
  private final ComparativeExport export;
  private final EbRecords records;

  /**
   * Creates the controller.
   *
   * @param comparatives comparatives
   * @param approval sign-off and threshold approval
   * @param presenter presentation to the client
   * @param export PDF and Excel
   * @param records programme and cycle
   */
  public ComparativeController(
      EbComparativeService comparatives,
      ComparativeApproval approval,
      ComparativePresenter presenter,
      ComparativeExport export,
      EbRecords records) {
    this.comparatives = comparatives;
    this.approval = approval;
    this.presenter = presenter;
    this.export = export;
    this.records = records;
  }

  /**
   * The comparatives of a programme.
   *
   * @param id programme
   * @param companyId company
   * @return comparatives, latest first
   */
  @GetMapping("/programmes/{id}/comparatives")
  @PreAuthorize(VIEW)
  public List<ComparativeSummary> list(@PathVariable Long id, @RequestParam Long companyId) {
    return comparatives.ofProgramme(companyId, id).stream().map(ComparativeSummary::from).toList();
  }

  /**
   * Builds a new comparative version.
   *
   * @param cycleId cycle
   * @param companyId company
   * @return the comparative page
   */
  @PostMapping("/cycles/{cycleId}/comparatives")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(MARKET)
  public ComparativeView build(@PathVariable Long cycleId, @RequestParam Long companyId) {
    return view(comparatives.build(companyId, cycleId));
  }

  /**
   * The comparative page.
   *
   * @param id comparative
   * @param companyId company
   * @return page
   */
  @GetMapping("/comparatives/{id}")
  @PreAuthorize(VIEW)
  public ComparativeView get(@PathVariable Long id, @RequestParam Long companyId) {
    return view(comparatives.require(companyId, id));
  }

  /**
   * Marks the recommendation.
   *
   * @param id comparative
   * @param companyId company
   * @param request proposal per line and summary
   * @return page
   */
  @PutMapping("/comparatives/{id}/recommendation")
  @PreAuthorize(MARKET)
  public ComparativeView recommend(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestBody RecommendationRequest request) {
    Map<String, Long> recommendation =
        request.recommendation() == null ? Map.of() : request.recommendation();
    return view(comparatives.recommend(companyId, id, recommendation, request.summary()));
  }

  /**
   * Submits for sign-off.
   *
   * @param id comparative
   * @param companyId company
   * @return page
   */
  @PostMapping("/comparatives/{id}/submit")
  @PreAuthorize(MARKET)
  public ComparativeView submit(@PathVariable Long id, @RequestParam Long companyId) {
    return view(comparatives.submit(companyId, id));
  }

  /**
   * Signs off.
   *
   * @param id comparative
   * @param companyId company
   * @param request remarks
   * @return page
   */
  @PostMapping("/comparatives/{id}/sign-off")
  @PreAuthorize(SIGN)
  public ComparativeView signOff(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestBody(required = false) RemarksRequest request) {
    return view(approval.signOff(companyId, id, remarks(request)));
  }

  /**
   * Approves above the threshold (the rule's approver permission is checked by the service).
   *
   * @param id comparative
   * @param companyId company
   * @param request remarks
   * @return page
   */
  @PostMapping("/comparatives/{id}/threshold-approve")
  @PreAuthorize(VIEW)
  public ComparativeView approveThreshold(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestBody(required = false) RemarksRequest request) {
    return view(approval.approveThreshold(companyId, id, remarks(request)));
  }

  /**
   * Returns to the AO.
   *
   * @param id comparative
   * @param companyId company
   * @param request reason
   * @return page
   */
  @PostMapping("/comparatives/{id}/return")
  @PreAuthorize(APPROVERS)
  public ComparativeView returnToAo(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody RemarksRequest request) {
    return view(approval.returnToAo(companyId, id, request.remarks()));
  }

  /**
   * Presents to the client.
   *
   * @param id comparative
   * @param companyId company
   * @return page
   */
  @PostMapping("/comparatives/{id}/present")
  @PreAuthorize(MARKET)
  public ComparativeView present(@PathVariable Long id, @RequestParam Long companyId) {
    return view(presenter.present(companyId, id));
  }

  /**
   * Adds a comment.
   *
   * @param id comparative
   * @param companyId company
   * @param request text, client flag and reply
   * @return the comment
   */
  @PostMapping("/comparatives/{id}/comments")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(VIEW)
  public CommentView comment(
      @PathVariable Long id, @RequestParam Long companyId, @RequestBody CommentRequest request) {
    return CommentView.from(
        comparatives.comment(companyId, id, request.client(), request.text(), request.replyTo()));
  }

  /**
   * Exports the comparative.
   *
   * @param id comparative
   * @param companyId company
   * @param format pdf or xlsx
   * @return file
   */
  @GetMapping("/comparatives/{id}/export")
  @PreAuthorize(VIEW)
  public ResponseEntity<byte[]> export(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @RequestParam(defaultValue = "pdf") String format) {
    EbComparative c = comparatives.require(companyId, id);
    boolean excel = "xlsx".equals(format) || "XLSX".equals(format);
    byte[] body = excel ? export.xlsx(c) : export.pdf(c);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(c.getComparativeNo() + (excel ? ".xlsx" : ".pdf"))
                .build()
                .toString())
        .contentType(excel ? MediaType.parseMediaType(XLSX) : MediaType.APPLICATION_PDF)
        .body(body);
  }

  private static String remarks(RemarksRequest request) {
    return request == null ? null : request.remarks();
  }

  private ComparativeView view(EbComparative c) {
    EbProgramme programme = records.programme(c.getCompanyId(), c.getProgrammeId());
    EbCycle cycle = records.cycle(c.getCompanyId(), c.getCycleId());
    return new ComparativeView(
        ComparativeSummary.from(c),
        programme.getId(),
        programme.getProgrammeNo(),
        programme.getName(),
        programme.getClientName(),
        programme.getAccountOfficer(),
        cycle.getCycleNo(),
        cycle.getStage().name(),
        c.getSummary(),
        c.getApproverPermission(),
        c.getAttachmentId(),
        c.getLines().stream().map(LineView::from).toList(),
        comparatives.matrix(c),
        approval.decisions(c).stream().map(DecisionView::from).toList(),
        comparatives.comments(c).stream().map(CommentView::from).toList());
  }
}
