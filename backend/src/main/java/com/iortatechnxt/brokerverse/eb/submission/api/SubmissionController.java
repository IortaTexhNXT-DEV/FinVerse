package com.iortatechnxt.brokerverse.eb.submission.api;

import com.iortatechnxt.brokerverse.eb.domain.EbSubmission;
import com.iortatechnxt.brokerverse.eb.service.EbParties;
import com.iortatechnxt.brokerverse.eb.submission.api.dto.SubmissionDtos.AcknowledgeRequest;
import com.iortatechnxt.brokerverse.eb.submission.api.dto.SubmissionDtos.SubmissionRequest;
import com.iortatechnxt.brokerverse.eb.submission.api.dto.SubmissionDtos.SubmissionResponse;
import com.iortatechnxt.brokerverse.eb.submission.service.SubmissionService;
import com.iortatechnxt.brokerverse.eb.submission.service.SubmissionService.ChecklistItem;
import com.iortatechnxt.brokerverse.eb.submission.service.SubmissionService.Scope;
import com.iortatechnxt.brokerverse.eb.submission.service.SubmissionService.SubmissionInput;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Submissions of documents to insurers (FR-EB-034). */
@RestController
@RequestMapping("/api/v1/eb")
@Transactional
public class SubmissionController {

  private static final String VIEW = "hasAuthority('EB_VIEW')";
  private static final String SEND = "hasAnyAuthority('EB_MARKET', 'EB_PROCESS')";

  private final SubmissionService submissions;
  private final EbParties parties;

  /**
   * Creates the controller.
   *
   * @param submissions submissions
   * @param parties insurer names
   */
  public SubmissionController(SubmissionService submissions, EbParties parties) {
    this.submissions = submissions;
    this.parties = parties;
  }

  /**
   * The submissions of a programme.
   *
   * @param id programme
   * @param companyId company
   * @return submissions, latest first
   */
  @GetMapping("/programmes/{id}/submissions")
  @PreAuthorize(VIEW)
  public List<SubmissionResponse> list(@PathVariable Long id, @RequestParam Long companyId) {
    return submissions.ofProgramme(companyId, id).stream().map(s -> map(companyId, s)).toList();
  }

  /**
   * The checklist of a submission.
   *
   * @param companyId company
   * @param programmeId programme
   * @param cycleId cycle
   * @param memberChangeId member change
   * @param processType process
   * @return checklist
   */
  @GetMapping("/submissions/checklist")
  @PreAuthorize(VIEW)
  public List<ChecklistItem> checklist(
      @RequestParam Long companyId,
      @RequestParam Long programmeId,
      @RequestParam(required = false) Long cycleId,
      @RequestParam(required = false) Long memberChangeId,
      @RequestParam String processType) {
    return submissions.checklist(companyId, new Scope(programmeId, cycleId, memberChangeId, processType));
  }

  /**
   * Submits documents to an insurer.
   *
   * @param companyId company
   * @param request scope, insurer, documents and remarks
   * @return the submission
   */
  @PostMapping("/submissions")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(SEND)
  public SubmissionResponse submit(@RequestParam Long companyId, @RequestBody SubmissionRequest request) {
    return map(
        companyId,
        submissions.submit(
            companyId,
            new Scope(request.programmeId(), request.cycleId(), request.memberChangeId(), request.processType()),
            new SubmissionInput(request.insurerCode(), request.attachmentIds(), request.remarks())));
  }

  /**
   * Records the insurer's acknowledgement.
   *
   * @param submissionId submission
   * @param companyId company
   * @param request date
   * @return the submission
   */
  @PostMapping("/submissions/{submissionId}/acknowledge")
  @PreAuthorize(SEND)
  public SubmissionResponse acknowledge(
      @PathVariable Long submissionId, @RequestParam Long companyId, @RequestBody AcknowledgeRequest request) {
    return map(companyId, submissions.acknowledge(companyId, submissionId, request.date()));
  }

  private SubmissionResponse map(Long companyId, EbSubmission s) {
    return SubmissionResponse.from(s, parties.insurerName(companyId, s.getInsurerCode()));
  }
}
