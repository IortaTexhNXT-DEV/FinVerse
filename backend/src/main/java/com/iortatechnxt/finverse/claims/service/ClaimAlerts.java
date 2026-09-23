package com.iortatechnxt.finverse.claims.service;

import com.iortatechnxt.finverse.alert.domain.AlertFacts;
import com.iortatechnxt.finverse.alert.domain.ExceptionCode;
import com.iortatechnxt.finverse.alert.service.AlertService;
import com.iortatechnxt.finverse.claims.domain.Claim;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Event-time exception rules of the claims module (codes seeded in V200, thresholds tuned by
 * administrators on Administration → Exception Codes):
 *
 * <ul>
 *   <li>{@value #LATE_NOTIFICATION}: a claim reported more than the threshold days after the loss;
 *   <li>{@value #LARGE_RESERVE}: an approved reserve (company share, base currency) at or above the
 *       threshold amount.
 * </ul>
 *
 * One live alert per claim and code (dedup key {@code <code>:<claim id>}).
 */
@Component
public class ClaimAlerts {

  /** Exception code of late claim notifications. */
  public static final String LATE_NOTIFICATION = "LATE_CLAIM_NOTIFICATION";

  /** Exception code of large claim reserves. */
  public static final String LARGE_RESERVE = "LARGE_CLAIM_RESERVE";

  private final AlertService alerts;

  /**
   * Creates the rules.
   *
   * @param alerts alert service
   */
  public ClaimAlerts(AlertService alerts) {
    this.alerts = alerts;
  }

  /**
   * Raises a late-notification alert when the reporting lag exceeds the threshold days.
   *
   * @param claim newly registered claim
   */
  public void checkNotification(Claim claim) {
    long lag =
        ChronoUnit.DAYS.between(claim.getLoss().getLossDate(), claim.getLoss().getReportedDate());
    alerts
        .activeCode(LATE_NOTIFICATION)
        .map(ExceptionCode::getThresholdDays)
        .filter(Objects::nonNull)
        .filter(days -> lag > days)
        .ifPresent(
            days ->
                raise(
                    LATE_NOTIFICATION,
                    claim,
                    "Claim "
                        + claim.getClaimNo()
                        + " reported "
                        + lag
                        + " days after the loss (threshold "
                        + days
                        + ")",
                    null));
  }

  /**
   * Raises a large-reserve alert when the approved reserve reaches the threshold amount.
   *
   * @param claim claim
   * @param baseEstimate company share of the payment estimate in base currency
   */
  public void checkReserve(Claim claim, BigDecimal baseEstimate) {
    alerts
        .activeCode(LARGE_RESERVE)
        .map(ExceptionCode::getThresholdAmount)
        .filter(Objects::nonNull)
        .filter(threshold -> baseEstimate.compareTo(threshold) >= 0)
        .ifPresent(
            threshold ->
                raise(
                    LARGE_RESERVE,
                    claim,
                    "Claim "
                        + claim.getClaimNo()
                        + " reserve "
                        + baseEstimate
                        + " reaches the threshold "
                        + threshold,
                    baseEstimate));
  }

  private void raise(String code, Claim claim, String message, BigDecimal amount) {
    alerts.raise(
        code,
        new AlertFacts(
            claim.getCompanyId(),
            claim.getBranchId(),
            ClaimSupport.CLAIM_ENTITY,
            claim.getClaimNo(),
            message,
            amount,
            code + ":" + claim.getId()));
  }
}
