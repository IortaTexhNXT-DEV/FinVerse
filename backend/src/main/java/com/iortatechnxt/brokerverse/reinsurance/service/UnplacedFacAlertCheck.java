package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.domain.ExceptionCode;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacement;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacPlacementRepository;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily check {@code RI_FAC_UNPLACED}: facultative requirements still provisional or pending
 * approval more than the exception code's threshold days (default 30) after the policy was ceded;
 * the risk is exposed beyond treaty capacity until placed.
 */
@Component
public class UnplacedFacAlertCheck implements AlertCheck {

  /** Exception code. */
  static final String CODE = "RI_FAC_UNPLACED";

  private static final int DEFAULT_DAYS = 30;

  private final FacPlacementRepository placements;
  private final AlertService alerts;

  /**
   * Creates the check.
   *
   * @param placements facultative placements
   * @param alerts exception code thresholds
   */
  public UnplacedFacAlertCheck(FacPlacementRepository placements, AlertService alerts) {
    this.placements = placements;
    this.alerts = alerts;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AlertSignal> evaluate(LocalDate asOf) {
    Optional<ExceptionCode> code = alerts.activeCode(CODE);
    if (code.isEmpty()) {
      return List.of();
    }
    Integer threshold = code.get().getThresholdDays();
    int days = threshold == null ? DEFAULT_DAYS : threshold;
    return placements
        .findByStatusIn(EnumSet.of(FacStatus.PROVISIONAL, FacStatus.PENDING_APPROVAL))
        .stream()
        .filter(f -> ChronoUnit.DAYS.between(f.getCession().getRiDate(), asOf) > days)
        .map(f -> new AlertSignal(CODE, facts(f, asOf)))
        .toList();
  }

  private static AlertFacts facts(FacPlacement f, LocalDate asOf) {
    long age = ChronoUnit.DAYS.between(f.getCession().getRiDate(), asOf);
    return new AlertFacts(
        f.getCompanyId(),
        f.getBranchId(),
        "FacPlacement",
        f.getPlacementNo(),
        "Facultative placement "
            + f.getPlacementNo()
            + " ("
            + f.getCession().getPolicyNo()
            + ") is "
            + f.getStatus()
            + " after "
            + age
            + " days",
        f.getCession().toBase(f.getFacPremium()),
        CODE + ":" + f.getPlacementNo());
  }
}
