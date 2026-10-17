package com.iortatechnxt.brokerverse.nonpackage.service;

import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalRequestRepository;
import com.iortatechnxt.brokerverse.nonpackage.domain.ProposalStatus;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the NB_PROPOSAL work case stage on the PRF (BRNB.022) for every transition, including the
 * generic ones of the workflow panel (TSU accept, returns, decline, void).
 */
@Component
public class ProposalStatusListener {

  private final ProposalRequestRepository proposals;

  /**
   * Creates the listener.
   *
   * @param proposals PRFs
   */
  public ProposalStatusListener(ProposalRequestRepository proposals) {
    this.proposals = proposals;
  }

  /**
   * Mirrors a stage change of a PRF's work case.
   *
   * @param event transition
   */
  @EventListener
  public void mirror(WorkCaseTransitioned event) {
    if (ProposalService.ENTITY.equals(event.entityType())) {
      proposals
          .findById(Long.valueOf(event.entityId()))
          .ifPresent(p -> p.markStatus(ProposalStatus.valueOf(event.toStage())));
    }
  }
}
