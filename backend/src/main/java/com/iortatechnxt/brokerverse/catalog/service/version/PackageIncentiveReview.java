package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteriaRepository;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flags the active incentive criteria of a package that expired or was retired for review (PMADD08:
 * alert {@code INCENTIVE_PRODUCT_INACTIVE}). Criteria are never deleted or changed automatically;
 * an Incentive Maintenance user deactivates or amends them.
 */
@Component
@Transactional
public class PackageIncentiveReview {

  /** Alert code (V815). */
  public static final String ALERT = "INCENTIVE_PRODUCT_INACTIVE";

  private final IncentiveCriteriaRepository criteria;
  private final AlertService alerts;
  private final Clock clock;

  /**
   * Creates the component.
   *
   * @param criteria incentive criteria
   * @param alerts alerts
   * @param clock clock
   */
  public PackageIncentiveReview(
      IncentiveCriteriaRepository criteria, AlertService alerts, Clock clock) {
    this.criteria = criteria;
    this.alerts = alerts;
    this.clock = clock;
  }

  /**
   * Raises one alert per active criterion that applies to a product which is no longer sold.
   *
   * @param productCode risk code
   * @param what "expired" or "retired"
   * @return criteria flagged
   */
  public List<IncentiveCriteria> productInactive(String productCode, String what) {
    LocalDate today = LocalDate.now(clock);
    List<IncentiveCriteria> affected =
        criteria.findByRecordStatus(RecordStatus.ACTIVE).stream()
            .filter(c -> c.getEffectiveTo() == null || !c.getEffectiveTo().isBefore(today))
            .filter(c -> c.getScopes().stream().anyMatch(s -> s.productCode().equals(productCode)))
            .toList();
    for (IncentiveCriteria c : affected) {
      alerts.raise(
          ALERT,
          new AlertFacts(
              c.getCompanyId(),
              null,
              "IncentiveCriteria",
              String.valueOf(c.getId()),
              "Incentive criterion "
                  + c.getCode()
                  + " applies to package "
                  + productCode
                  + ", which "
                  + what
                  + ": review or deactivate it",
              null,
              ALERT + ":" + c.getId() + ":" + productCode));
    }
    return affected;
  }
}
