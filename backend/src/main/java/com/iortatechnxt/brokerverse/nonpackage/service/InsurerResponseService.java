package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponseHistory;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponseHistoryRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponseRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseTerms;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insurer responses to the quotation slip (BRNB.009/010): TSU keys in each insurer's terms and
 * attaches its response document (every change is kept in the version history), flags the
 * recommended insurer, and closes the request for terms when all responses are in or when it
 * decides not to wait (terms_complete). The comparative table is compiled from the responses.
 */
@Service
@Transactional
public class InsurerResponseService {

  /** Document type of an insurer's response. */
  public static final String RESPONSE_DOCUMENT = "INSURER_RESPONSE";

  private static final String RESPONSE = "Insurer response";

  private final ProposalService proposals;
  private final InsurerResponseRepository responses;
  private final InsurerResponseHistoryRepository history;
  private final DocumentService documents;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param proposals PRF reads
   * @param responses responses
   * @param history response history
   * @param documents documents of the PRF
   * @param workflow workflow engine
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public InsurerResponseService(
      ProposalService proposals,
      InsurerResponseRepository responses,
      InsurerResponseHistoryRepository history,
      DocumentService documents,
      WorkflowService workflow,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.proposals = proposals;
    this.responses = responses;
    this.history = history;
    this.documents = documents;
    this.workflow = workflow;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Responses of a PRF.
   *
   * @param id PRF
   * @return responses in the order the insurers were approached
   */
  @Transactional(readOnly = true)
  public List<InsurerResponse> responses(Long id) {
    return responses.findByProposalIdOrderById(proposals.get(id).getId());
  }

  /**
   * Version history of the responses of a PRF, newest first.
   *
   * @param id PRF
   * @return snapshots
   */
  @Transactional(readOnly = true)
  public List<InsurerResponseHistory> history(Long id) {
    List<Long> ids = responses(id).stream().map(InsurerResponse::getId).toList();
    return ids.isEmpty() ? List.of() : history.findByResponseIdInOrderByIdDesc(ids);
  }

  /**
   * The comparative table (BRNB.010).
   *
   * @param id PRF
   * @return table
   */
  @Transactional(readOnly = true)
  public ComparativeTable comparative(Long id) {
    return ComparativeTable.of(responses(id));
  }

  /**
   * Records an insurer's terms (TSU).
   *
   * @param id PRF
   * @param responseId response
   * @param terms terms
   * @return the response
   */
  public InsurerResponse record(Long id, Long responseId, ResponseTerms terms) {
    InsurerResponse response = open(id, responseId);
    if (terms.status() == ResponseStatus.RECEIVED && terms.premium() == null) {
      throw new BusinessRuleException(
          "RESPONSE_PREMIUM_REQUIRED", "Enter the premium quoted by " + response.getInsurerName());
    }
    response.record(terms, clock.instant());
    snapshot(response, "Terms of " + response.getInsurerName() + ": " + terms.status());
    return response;
  }

  /**
   * Attaches an insurer's response document to the PRF and links it to the response.
   *
   * @param id PRF
   * @param responseId response
   * @param file uploaded file
   * @return the response
   */
  public InsurerResponse attach(Long id, Long responseId, UploadedFile file) {
    InsurerResponse response = open(id, responseId);
    ProposalRequest p = proposals.get(id);
    Attachment saved =
        documents
            .upload(
                new AttachmentTarget(ProposalService.ENTITY, String.valueOf(id)),
                List.of(file),
                new UploadOptions(
                    RESPONSE_DOCUMENT, true, p.getPrfNo(), "Terms of " + response.getInsurerName()))
            .get(0);
    response.attachDocument(saved.getId());
    snapshot(response, "Response document of " + response.getInsurerName());
    return response;
  }

  /**
   * Flags one received response as recommended; the others lose the flag.
   *
   * @param id PRF
   * @param responseId response
   * @return the response
   */
  public InsurerResponse recommend(Long id, Long responseId) {
    InsurerResponse chosen = open(id, responseId);
    if (chosen.getStatus() != ResponseStatus.RECEIVED) {
      throw new BusinessRuleException(
          "RESPONSE_NOT_RECEIVED", "Only received terms can be recommended");
    }
    for (InsurerResponse r : responses.findByProposalIdOrderById(id)) {
      boolean flag = r.getId().equals(chosen.getId());
      if (r.isRecommended() != flag) {
        r.recommend(flag);
        snapshot(r, (flag ? "Recommended " : "Not recommended ") + r.getInsurerName());
      }
    }
    return chosen;
  }

  /**
   * Closes the request for terms (BRNB.009): all responses in, or the user decides to proceed with
   * the terms received so far.
   *
   * @param id PRF
   * @param closePending proceed although some insurers have not answered
   * @param comment comment
   * @return the PRF, now TERMS_RECEIVED
   */
  public ProposalRequest termsComplete(Long id, boolean closePending, String comment) {
    ProposalRequest p = proposals.get(id);
    List<InsurerResponse> all = responses.findByProposalIdOrderById(id);
    if (all.stream().noneMatch(r -> r.getStatus() == ResponseStatus.RECEIVED)) {
      throw new BusinessRuleException("TERMS_NONE", "No insurer terms have been received");
    }
    long pending = all.stream().filter(r -> r.getStatus() == ResponseStatus.PENDING).count();
    if (pending > 0 && !closePending) {
      throw new BusinessRuleException(
          "TERMS_PENDING", pending + " insurer(s) have not answered: close the request to proceed");
    }
    if (pending > 0) {
      p.closeTerms();
    }
    workflow.transition(
        ProposalService.ENTITY,
        String.valueOf(id),
        "terms_complete",
        TransitionNote.comment(pending > 0 ? "Closed with " + pending + " pending" : comment));
    return p;
  }

  private InsurerResponse open(Long id, Long responseId) {
    ProposalRequest p = proposals.get(id);
    if (!ProposalStatus.RESPONSES_OPEN.contains(p.getStatus())) {
      throw new BusinessRuleException(
          "RESPONSES_CLOSED", "Insurer terms are keyed in after the quotation slip is sent");
    }
    InsurerResponse response =
        responses
            .findById(responseId)
            .orElseThrow(() -> new ResourceNotFoundException(RESPONSE, responseId));
    if (!response.getProposalId().equals(id)) {
      throw new ResourceNotFoundException(RESPONSE, responseId);
    }
    return response;
  }

  private void snapshot(InsurerResponse response, String summary) {
    history.save(new InsurerResponseHistory(response, currentUser.username(), clock.instant()));
    ProposalRequest p = proposals.get(response.getProposalId());
    audit.record(ProposalService.ENTITY, p.getPrfNo(), AuditAction.UPDATE, summary);
  }
}
