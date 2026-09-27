package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.DispositionStatus;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTagRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.DispositionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptActionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.UnappliedRepository;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the work case stages of the cashiering workflows on their records, including the generic
 * actions of the workflow panel (return, withdraw, cancel): {@code OPS_RECEIPT_ACTION} on the
 * receipt action, {@code OPS_DISPOSITION} on the unapplied item and its current disposition, and
 * {@code OPS_CWT_2307} on the 2307 tag.
 */
@Component
public class CashieringStageListener {

  private static final Set<DispositionStatus> MIRRORED =
      EnumSet.of(
          DispositionStatus.MONITORING,
          DispositionStatus.FOR_APPROVAL,
          DispositionStatus.IN_PROCESS,
          DispositionStatus.COMPLETED,
          DispositionStatus.FOR_REVERSAL);

  private final ReceiptActionRepository actions;
  private final UnappliedRepository items;
  private final DispositionRepository dispositions;
  private final CwtTagRepository tags;

  /**
   * Creates the listener.
   *
   * @param actions receipt actions
   * @param items unapplied items
   * @param dispositions dispositions
   * @param tags 2307 tags
   */
  public CashieringStageListener(
      ReceiptActionRepository actions,
      UnappliedRepository items,
      DispositionRepository dispositions,
      CwtTagRepository tags) {
    this.actions = actions;
    this.items = items;
    this.dispositions = dispositions;
    this.tags = tags;
  }

  /**
   * Mirrors a stage change.
   *
   * @param event transition
   */
  @EventListener
  public void mirror(WorkCaseTransitioned event) {
    Long id = parse(event.entityId());
    if (id == null) {
      return;
    }
    switch (event.entityType()) {
      case ReceiptActionService.ENTITY ->
          actions.findById(id).ifPresent(a -> a.markStage(event.toStage()));
      case UnappliedService.ENTITY -> mirrorUnapplied(id, event.toStage());
      case CwtService.ENTITY -> tags.findById(id).ifPresent(t -> t.markStage(event.toStage()));
      default -> {
        // other modules' cases
      }
    }
  }

  private void mirrorUnapplied(Long id, String stage) {
    items.findById(id).ifPresent(i -> i.markStage(stage));
    DispositionStatus status = statusOf(stage);
    if (status != null) {
      dispositions
          .findFirstByUnappliedIdAndStatusInOrderByIdDesc(id, MIRRORED)
          .ifPresent(d -> d.markStatus(status));
    }
  }

  private static DispositionStatus statusOf(String stage) {
    for (DispositionStatus s : MIRRORED) {
      if (s.name().equals(stage)) {
        return s;
      }
    }
    return null;
  }

  private static Long parse(String id) {
    try {
      return Long.valueOf(id);
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
