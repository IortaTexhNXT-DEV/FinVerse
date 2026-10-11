package com.iortatechnxt.brokerverse.acsl.api;

import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.AssignInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.CommentInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.CorrectionInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.LineInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.LinesInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslInputs.ProposalInput;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslViews.CorrectionSummary;
import com.iortatechnxt.brokerverse.acsl.api.dto.AcslViews.CorrectionView;
import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionStage;
import com.iortatechnxt.brokerverse.acsl.service.AcslQueryService;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionJournals;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionJournals.OriginalLine;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionPosting;
import com.iortatechnxt.brokerverse.acsl.service.CorrectionService;
import com.iortatechnxt.brokerverse.common.api.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Correction entries (ACSL 2.7.0-2.15.0, 2.9.1): the list, one correction with the original lines
 * of its invoice family (the correction editor), and the actions: raise, assign, propose a
 * wrong-account correction, save the lines, submit, endorse and approve (post). Returns and
 * cancellations are generic workflow actions.
 */
@RestController
@RequestMapping("/api/v1/acsl/corrections")
public class AcslCorrectionController {

  private static final String CORRECTION = "/{id}";

  private final AcslQueryService queries;
  private final CorrectionService corrections;
  private final CorrectionPosting posting;
  private final CorrectionJournals journals;

  /**
   * Creates the controller.
   *
   * @param queries reads
   * @param corrections preparation
   * @param posting approval and posting
   * @param journals original lines
   */
  public AcslCorrectionController(
      AcslQueryService queries,
      CorrectionService corrections,
      CorrectionPosting posting,
      CorrectionJournals journals) {
    this.queries = queries;
    this.corrections = corrections;
    this.posting = posting;
    this.journals = journals;
  }

  /**
   * Corrections by stage.
   *
   * @param companyId company
   * @param stage stage
   * @param q correction, invoice or description
   * @param page page
   * @param size size
   * @return corrections, newest first
   */
  @GetMapping
  @PreAuthorize(AcslAccess.VIEW)
  public PageResponse<CorrectionSummary> search(
      @RequestParam Long companyId,
      @RequestParam(required = false) CorrectionStage stage,
      @RequestParam(required = false) String q,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return PageResponse.of(
        queries.corrections(companyId, stage, q, AcslCaseController.page(page, size)),
        CorrectionSummary::from);
  }

  /**
   * Corrections per stage.
   *
   * @param companyId company
   * @return counts
   */
  @GetMapping("/counts")
  @PreAuthorize(AcslAccess.VIEW)
  public Map<CorrectionStage, Long> counts(@RequestParam Long companyId) {
    return queries.correctionCounts(companyId);
  }

  /**
   * One correction.
   *
   * @param id correction
   * @return correction
   */
  @GetMapping(CORRECTION)
  @PreAuthorize(AcslAccess.VIEW)
  public CorrectionView get(@PathVariable Long id) {
    return CorrectionView.from(corrections.get(id));
  }

  /**
   * The posted lines of the journals of the correction's invoice family and corrected journal (ACSL
   * 2.5.3, 2.9.1).
   *
   * @param id correction
   * @return lines
   */
  @GetMapping(CORRECTION + "/original-lines")
  @PreAuthorize(AcslAccess.VIEW)
  public List<OriginalLine> originalLines(@PathVariable Long id) {
    Correction c = corrections.get(id);
    return journals.familyLines(c.getCompanyId(), c.getInvoiceNo(), c.getOriginalBatchNo());
  }

  /**
   * Raises a correction (team leader, ACSL 2.7.0).
   *
   * @param companyId company
   * @param input kind, invoice, original journal and description
   * @return the correction
   */
  @PostMapping
  @PreAuthorize(AcslAccess.ASSIGN)
  public CorrectionView create(
      @RequestParam Long companyId, @Valid @RequestBody CorrectionInput input) {
    return CorrectionView.from(corrections.create(companyId, input.draft()));
  }

  /**
   * Assigns or re-assigns the preparation (ACSL 2.7.0, 2.8.0).
   *
   * @param id correction
   * @param input preparer and comment
   * @return the correction
   */
  @PostMapping(CORRECTION + "/assign")
  @PreAuthorize(AcslAccess.ASSIGN)
  public CorrectionView assign(@PathVariable Long id, @Valid @RequestBody AssignInput input) {
    return CorrectionView.from(corrections.assign(id, input.username().strip(), input.comment()));
  }

  /**
   * Proposes a wrong-account correction (ACSL 2.9.1).
   *
   * @param id correction
   * @param input original line and right account
   * @return the correction
   */
  @PostMapping(CORRECTION + "/propose")
  @PreAuthorize(AcslAccess.PROCESS)
  public CorrectionView propose(@PathVariable Long id, @Valid @RequestBody ProposalInput input) {
    return CorrectionView.from(corrections.proposeWrongAccount(id, input.proposal()));
  }

  /**
   * Saves the lines of a draft (ACSL 2.9.0).
   *
   * @param id correction
   * @param input lines
   * @return the correction
   */
  @PutMapping(CORRECTION + "/lines")
  @PreAuthorize(AcslAccess.PROCESS)
  public CorrectionView saveLines(@PathVariable Long id, @Valid @RequestBody LinesInput input) {
    return CorrectionView.from(
        corrections.saveLines(id, input.lines().stream().map(LineInput::values).toList()));
  }

  /**
   * Submits for review (ACSL 2.9.0).
   *
   * @param id correction
   * @param input comment
   * @return the correction
   */
  @PostMapping(CORRECTION + "/submit")
  @PreAuthorize(AcslAccess.PROCESS)
  public CorrectionView submit(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    return CorrectionView.from(corrections.submit(id, input.comment()));
  }

  /**
   * Endorses for approval (ACSL 2.10.0).
   *
   * @param id correction
   * @param input comment
   * @return the correction
   */
  @PostMapping(CORRECTION + "/endorse")
  @PreAuthorize(AcslAccess.REVIEW)
  public CorrectionView endorse(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    return CorrectionView.from(corrections.endorse(id, input.comment()));
  }

  /**
   * Approves and posts (ACSL 2.11.0, 2.15.0).
   *
   * @param id correction
   * @param input comment
   * @return the posted correction
   */
  @PostMapping(CORRECTION + "/approve")
  @PreAuthorize(AcslAccess.APPROVE)
  public CorrectionView approve(@PathVariable Long id, @Valid @RequestBody CommentInput input) {
    return CorrectionView.from(posting.approve(id, input.comment()));
  }
}
