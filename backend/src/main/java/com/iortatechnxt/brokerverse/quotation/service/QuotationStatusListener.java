package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationRepository;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors the NB_QUOTATION work case stage on the quotation (BRNB.022) for every transition,
 * including the generic ones run from the workflow panel (return, decline, void).
 */
@Component
public class QuotationStatusListener {

  private final QuotationRepository quotations;

  /**
   * Creates the listener.
   *
   * @param quotations quotations
   */
  public QuotationStatusListener(QuotationRepository quotations) {
    this.quotations = quotations;
  }

  /**
   * Mirrors a stage change of a quotation's work case.
   *
   * @param event transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!QuotationService.ENTITY.equals(event.entityType())) {
      return;
    }
    Quotation quotation =
        quotations
            .findById(Long.valueOf(event.entityId()))
            .orElseThrow(() -> new IllegalStateException("No quotation " + event.entityId()));
    quotation.markStatus(QuotationStatus.valueOf(event.toStage()));
  }
}
