package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NoticeDelivery;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequest;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequestRepository;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalService;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Tells of the decision on a proposal of a quotation request (BDOI FRS FRPM.009.02): the TSU
 * officer who generated it learns that it was approved or returned (with the remarks), and the
 * requestor that the proposal is ready.
 */
@Component
public class ProposalDecisionNotices {

  /** Event of the decision. */
  public static final String EVENT = "PM_PROPOSAL_DECIDED";

  private static final String FOR_APPROVAL = "PS_FOR_APPROVAL";

  private final ProposalRequestRepository proposals;
  private final NoticeDelivery delivery;

  /**
   * Creates the listener.
   *
   * @param proposals quotation requests
   * @param delivery notices
   */
  public ProposalDecisionNotices(ProposalRequestRepository proposals, NoticeDelivery delivery) {
    this.proposals = proposals;
    this.delivery = delivery;
  }

  /**
   * Follows the decisions on proposals.
   *
   * @param event transition
   */
  @EventListener
  public void onTransition(WorkCaseTransitioned event) {
    if (ProposalService.ENTITY.equals(event.entityType())
        && FOR_APPROVAL.equals(event.fromStage())) {
      proposals.findById(Long.valueOf(event.entityId())).ifPresent(p -> tell(p, event));
    }
  }

  private void tell(ProposalRequest p, WorkCaseTransitioned event) {
    boolean approved = "approve_ps".equals(event.action());
    String reference = p.getArn() == null ? p.getPrfNo() : p.getArn();
    String outcome = approved ? "approved" : "returned";
    if (p.getPsSubmittedBy() != null) {
      delivery.toUser(
          p.getPsSubmittedBy(),
          notice(
              p,
              reference + ": proposal " + outcome,
              event.comment() == null ? p.getClientName() : event.comment()),
          EVENT,
          false);
    }
    if (approved) {
      delivery.toUser(
          p.getCreatedBy(),
          notice(
              p,
              reference + ": Proposal Ready",
              "The proposal for " + p.getClientName() + " is ready to be sent to the client."),
          EVENT,
          false);
    }
  }

  private static Notice notice(ProposalRequest p, String title, String body) {
    return new Notice(
        title, body, "/proposals/" + p.getId(), ProposalService.ENTITY, String.valueOf(p.getId()));
  }
}
