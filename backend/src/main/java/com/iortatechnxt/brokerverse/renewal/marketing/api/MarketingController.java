package com.iortatechnxt.brokerverse.renewal.marketing.api;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalRemark;
import com.iortatechnxt.brokerverse.renewal.marketing.service.NbPathService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.OverrideService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RemarkService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalAssignmentService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalAssignmentService.Officer;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalDispositionService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.ReviewService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Marketing actions on renewals (FR-RN-030, 043-048, 050, 051): assignment, disposition, push,
 * remarks, re-opening, the New Business path, the Team Leader's return and post, and the overrides.
 * Actions on several renewals answer with the done and the refused ones.
 */
@RestController
@RequestMapping("/api/v1/renewal")
public class MarketingController {

  private final RenewalAssignmentService assignments;
  private final RenewalDispositionService dispositions;
  private final ReviewService review;
  private final OverrideService overrides;
  private final RemarkService remarks;
  private final NbPathService nbPath;

  /**
   * Creates the controller.
   *
   * @param assignments assignment
   * @param dispositions disposition
   * @param review Team Leader review
   * @param overrides overrides
   * @param remarks remarks
   * @param nbPath New Business path
   */
  public MarketingController(
      RenewalAssignmentService assignments,
      RenewalDispositionService dispositions,
      ReviewService review,
      OverrideService overrides,
      RemarkService remarks,
      NbPathService nbPath) {
    this.assignments = assignments;
    this.dispositions = dispositions;
    this.review = review;
    this.overrides = overrides;
    this.remarks = remarks;
    this.nbPath = nbPath;
  }

  /**
   * The officers the user may assign to.
   *
   * @param companyId company
   * @return officers
   */
  @GetMapping("/officers")
  @PreAuthorize("hasAuthority('RNW_ASSIGN')")
  public List<Officer> officers(@RequestParam Long companyId) {
    return assignments.officers(companyId);
  }

  /**
   * Assigns or re-assigns renewals to an AO.
   *
   * @param request selection, officer and reason
   * @return outcome
   */
  @PostMapping("/candidates/assign")
  @PreAuthorize("hasAuthority('RNW_ASSIGN')")
  public BatchOutcome assign(@Valid @RequestBody AssignRequest request) {
    return assignments.assign(
        request.companyId(), request.renewalRefs(), request.ao(), request.reasonCode());
  }

  /**
   * Saves the disposition of a renewal.
   *
   * @param companyId company
   * @param ref renewal
   * @param input disposition
   * @return stage after the save
   */
  @PostMapping("/candidates/{ref}/disposition")
  @PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_REVIEW')")
  public StageView dispose(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @RequestBody RenewalDispositionService.Input input) {
    return StageView.of(dispositions.save(companyId, ref, input).getStage().name());
  }

  /**
   * Pushes dispositioned renewals to the Team Leader.
   *
   * @param request selection
   * @return outcome
   */
  @PostMapping("/candidates/push")
  @PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_REVIEW')")
  public BatchOutcome push(@Valid @RequestBody Selection request) {
    return dispositions.push(request.companyId(), request.renewalRefs());
  }

  /**
   * Adds a remark.
   *
   * @param companyId company
   * @param ref renewal
   * @param request text
   * @return remark
   */
  @PostMapping("/candidates/{ref}/remarks")
  @PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_REVIEW','RNW_PROCESS','RNW_FOLLOWUP')")
  public RemarkView addRemark(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody TextRequest request) {
    return RemarkView.of(remarks.add(companyId, ref, request.text()));
  }

  /**
   * The remarks of a renewal.
   *
   * @param companyId company
   * @param ref renewal
   * @return remarks, newest first
   */
  @GetMapping("/candidates/{ref}/remarks")
  @PreAuthorize("hasAuthority('RNW_VIEW')")
  public List<RemarkView> remarks(@RequestParam Long companyId, @PathVariable String ref) {
    return remarks.of(companyId, ref).stream().map(RemarkView::of).toList();
  }

  /**
   * Re-opens a renewal tagged Not for Renewal.
   *
   * @param companyId company
   * @param ref renewal
   * @param request reason
   * @return stage after the re-opening
   */
  @PostMapping("/candidates/{ref}/reopen")
  @PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_PROCESS_ASSIGN')")
  public StageView reopen(
      @RequestParam Long companyId, @PathVariable String ref, @RequestBody TextRequest request) {
    return StageView.of(dispositions.reopen(companyId, ref, request.text()).getStage().name());
  }

  /**
   * Returns renewals to the AO.
   *
   * @param request selection, reason and remarks
   * @return outcome
   */
  @PostMapping("/candidates/return")
  @PreAuthorize("hasAuthority('RNW_REVIEW')")
  public BatchOutcome returnToAo(@Valid @RequestBody ReturnRequest request) {
    return review.returnToAo(
        request.companyId(), request.renewalRefs(), request.reasonCode(), request.remarks());
  }

  /**
   * Posts renewals.
   *
   * @param request selection
   * @return outcome
   */
  @PostMapping("/candidates/post")
  @PreAuthorize("hasAuthority('RNW_REVIEW')")
  public BatchOutcome post(@Valid @RequestBody Selection request) {
    return review.post(request.companyId(), request.renewalRefs());
  }

  /**
   * Overrides renewals.
   *
   * @param request selection and override
   * @return outcome
   */
  @PostMapping("/candidates/override")
  @PreAuthorize("hasAuthority('RNW_OVERRIDE')")
  public BatchOutcome override(@Valid @RequestBody OverrideRequest request) {
    return overrides.override(
        request.companyId(),
        request.renewalRefs(),
        new OverrideService.Request(
            request.kind(), request.target(), request.reasonCode(), request.remarks()));
  }

  /**
   * Starts the New Business path.
   *
   * @param companyId company
   * @param ref renewal
   * @param kind QUOTATION or PROPOSAL, default from the disposition
   * @return the quotation or PRF number
   */
  @PostMapping("/candidates/{ref}/nb-path")
  @PreAuthorize("hasAnyAuthority('RNW_DISPOSE','RNW_REVIEW')")
  public NbPathView startNbPath(
      @RequestParam Long companyId,
      @PathVariable String ref,
      @RequestParam(required = false) NbPathService.Kind kind) {
    return new NbPathView(nbPath.start(companyId, ref, kind));
  }

  /**
   * A selection of renewals.
   *
   * @param companyId company
   * @param renewalRefs renewals
   */
  public record Selection(@NotNull Long companyId, @NotEmpty List<String> renewalRefs) {}

  /**
   * An assignment.
   *
   * @param companyId company
   * @param renewalRefs renewals
   * @param ao account officer
   * @param reasonCode reason of a re-assignment
   */
  public record AssignRequest(
      @NotNull Long companyId, @NotEmpty List<String> renewalRefs, String ao, String reasonCode) {}

  /**
   * A return to the AO.
   *
   * @param companyId company
   * @param renewalRefs renewals
   * @param reasonCode reason
   * @param remarks remarks
   */
  public record ReturnRequest(
      @NotNull Long companyId,
      @NotEmpty List<String> renewalRefs,
      String reasonCode,
      String remarks) {}

  /**
   * An override.
   *
   * @param companyId company
   * @param renewalRefs renewals
   * @param kind kind
   * @param target check, bucket or disposition
   * @param reasonCode reason
   * @param remarks remarks
   */
  public record OverrideRequest(
      @NotNull Long companyId,
      @NotEmpty List<String> renewalRefs,
      com.iortatechnxt.brokerverse.renewal.domain.OverrideKind kind,
      String target,
      String reasonCode,
      String remarks) {}

  /**
   * A text.
   *
   * @param text text
   */
  public record TextRequest(String text) {}

  /**
   * Stage of a renewal after an action.
   *
   * @param stage stage
   */
  public record StageView(String stage) {
    static StageView of(String stage) {
      return new StageView(stage);
    }
  }

  /**
   * The quotation or PRF of the New Business path.
   *
   * @param reference number
   */
  public record NbPathView(String reference) {}

  /**
   * A remark.
   *
   * @param text text
   * @param stage stage when written
   * @param by user
   * @param at time
   */
  public record RemarkView(String text, String stage, String by, Instant at) {
    static RemarkView of(RenewalRemark r) {
      return new RemarkView(r.getText(), r.getStage().name(), r.getCreatedBy(), r.getCreatedAt());
    }
  }
}
