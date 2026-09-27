package com.iortatechnxt.brokerverse.eb.service;

import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequest;
import com.iortatechnxt.brokerverse.eb.domain.EbFranchiseRequestRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChangeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.domain.EbSoaRepository;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the stage of the {@code EB_FRANCHISE}, {@code EB_MEMBER_CHANGE} and {@code EB_SOA} work
 * cases on their records (design 7.2), including the generic actions run from the workflow panel
 * (return and cancel of a member change).
 */
@Component
public class EbCaseMirror {

  private final EbFranchiseRequestRepository franchises;
  private final EbMemberChangeRepository changes;
  private final EbSoaRepository soas;
  private final Clock clock;

  /**
   * Creates the listener.
   *
   * @param franchises franchise requests
   * @param changes member changes
   * @param soas SOAs
   * @param clock clock
   */
  public EbCaseMirror(
      EbFranchiseRequestRepository franchises,
      EbMemberChangeRepository changes,
      EbSoaRepository soas,
      Clock clock) {
    this.franchises = franchises;
    this.changes = changes;
    this.soas = soas;
    this.clock = clock;
  }

  /**
   * Mirrors a stage change.
   *
   * @param event transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    String type = event.entityType();
    if (EbCodes.ENTITY_FRANCHISE.equals(type)) {
      franchises
          .findById(Long.valueOf(event.entityId()))
          .ifPresent(f -> f.mirror(EbFranchiseRequest.Status.valueOf(event.toStage())));
    } else if (EbCodes.ENTITY_MEMBER_CHANGE.equals(type)) {
      changes
          .findById(Long.valueOf(event.entityId()))
          .ifPresent(
              c -> c.mirror(EbMemberChange.Status.valueOf(event.toStage()), clock.instant()));
    } else if (EbCodes.ENTITY_SOA.equals(type)) {
      soas.findById(Long.valueOf(event.entityId()))
          .ifPresent(s -> s.mirror(EbSoa.Status.valueOf(event.toStage())));
    }
  }
}
