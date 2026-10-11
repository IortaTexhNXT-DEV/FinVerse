package com.iortatechnxt.brokerverse.booking.api;

import com.iortatechnxt.brokerverse.booking.domain.IncentiveEvaluation;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveStatus;
import com.iortatechnxt.brokerverse.booking.domain.IncentiveTrigger;
import com.iortatechnxt.brokerverse.booking.service.IncentiveEvaluationService;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The incentive evaluations of a booked transaction (FR-NB-118, FR-RN-087 audit): trigger, criteria
 * matched, endorsements considered, result and reason. The indicator has no change endpoint: it is
 * set by the rules only.
 */
@RestController
@RequestMapping("/api/v1/booking/invoices")
public class IncentiveHistoryController {

  private final IncentiveEvaluationService incentives;

  /**
   * Creates the controller.
   *
   * @param incentives incentive evaluation
   */
  public IncentiveHistoryController(IncentiveEvaluationService incentives) {
    this.incentives = incentives;
  }

  /**
   * The evaluations of the transaction of an invoice, oldest first.
   *
   * @param invoiceNo an invoice of the transaction
   * @return evaluations
   */
  @GetMapping("/{invoiceNo}/incentive-history")
  @PreAuthorize(BookingAccess.VIEW)
  public List<EvaluationView> history(@PathVariable String invoiceNo) {
    return incentives.history(invoiceNo).stream().map(EvaluationView::of).toList();
  }

  /**
   * An evaluation.
   *
   * @param trigger booking, full payment, endorsement or cancellation
   * @param result indicator
   * @param resultLabel indicator as shown
   * @param criteria criteria codes matched
   * @param endorsements endorsement invoices considered
   * @param reason reason
   * @param evaluatedAt time
   * @param evaluatedBy user or SYSTEM
   */
  public record EvaluationView(
      IncentiveTrigger trigger,
      IncentiveStatus result,
      String resultLabel,
      String criteria,
      String endorsements,
      String reason,
      Instant evaluatedAt,
      String evaluatedBy) {

    /**
     * Maps an evaluation.
     *
     * @param e evaluation
     * @return view
     */
    public static EvaluationView of(IncentiveEvaluation e) {
      return new EvaluationView(
          e.getTrigger(),
          e.getResult(),
          e.getResult().label(),
          e.getCriteria(),
          e.getEndorsements(),
          e.getReason(),
          e.getEvaluatedAt(),
          e.getEvaluatedBy());
    }
  }
}
