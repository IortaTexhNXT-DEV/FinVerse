package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequestRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the PM_PACKAGE_REQUEST work case stage on the request for every transition, including the
 * generic ones of the workflow panel (returns, void, not proceeded, request changes) and the
 * catalog system actions; opens negotiation round 1 when the TSU Head approves (BRPM.010) and
 * records a ManCom return (BRPM.015).
 */
@Component
public class PackageStatusListener {

  private final PackageRequestRepository requests;
  private final NegotiationService negotiation;
  private final RequirementsService requirements;
  private final TermsCodec codec;

  /**
   * Creates the listener.
   *
   * @param requests package requests
   * @param negotiation negotiation rounds
   * @param requirements ManCom decisions
   * @param codec terms JSON
   */
  public PackageStatusListener(
      PackageRequestRepository requests,
      NegotiationService negotiation,
      RequirementsService requirements,
      TermsCodec codec) {
    this.requests = requests;
    this.negotiation = negotiation;
    this.requirements = requirements;
    this.codec = codec;
  }

  /**
   * Mirrors a stage change of a package request's work case.
   *
   * @param event transition
   */
  @EventListener
  public void mirror(WorkCaseTransitioned event) {
    if (!PackageRequests.ENTITY.equals(event.entityType())) {
      return;
    }
    requests
        .findById(Long.valueOf(event.entityId()))
        .ifPresent(
            p -> {
              RequestStage stage = RequestStage.valueOf(event.toStage());
              p.markStatus(stage);
              follow(p, event, stage);
            });
  }

  private void follow(PackageRequest p, WorkCaseTransitioned event, RequestStage stage) {
    if (stage == RequestStage.NEGOTIATION
        && RequestStage.FOR_TSU_APPROVAL.name().equals(event.fromStage())) {
      PackageTerms terms = codec.terms(p.getRequestedTerms());
      negotiation.openFirstRound(p, terms.insurerCodes(), p.getRecommendation());
    } else if (stage == RequestStage.REQUIREMENTS_PREP
        && RequestStage.FOR_MANCOM.name().equals(event.fromStage())) {
      requirements.recordReturn(p, note(event));
    }
  }

  private static String note(WorkCaseTransitioned event) {
    String reason = event.reasonCode() == null ? "" : event.reasonCode();
    String comment = event.comment() == null ? "" : event.comment();
    String text = (reason + " " + comment).strip();
    return text.isEmpty() ? null : text;
  }
}
