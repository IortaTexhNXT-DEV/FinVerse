package com.iortatechnxt.brokerverse.reinsurance.service;

import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.insurance.ClaimsExperienceView;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiClaimMovement;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiClaimMovementRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reinsurers' shares of claims: the processed movements for the recoveries screen, and a catch-up
 * that replays the posted claim movements of a period through {@link ClaimRecoveryListener}
 * (idempotent), for movements posted before a treaty existed or while reinsurance was not deployed.
 * The claims module is reached only through the kernel port {@link ClaimsExperienceView}, injected
 * optionally.
 */
@Service
@Transactional
public class ClaimRecoveryService {

  private final RiClaimMovementRepository movements;
  private final ClaimRecoveryListener listener;
  private final ObjectProvider<ClaimsExperienceView> claims;

  /**
   * Creates the service.
   *
   * @param movements processed movements
   * @param listener claim movement listener
   * @param claims claims data, when the claims module is deployed
   */
  public ClaimRecoveryService(
      RiClaimMovementRepository movements,
      ClaimRecoveryListener listener,
      ObjectProvider<ClaimsExperienceView> claims) {
    this.movements = movements;
    this.listener = listener;
    this.claims = claims;
  }

  /**
   * Processed movements of a period with their shares.
   *
   * @param companyId company
   * @param from first movement date
   * @param to last movement date
   * @return movements ordered by date
   */
  @Transactional(readOnly = true)
  public List<RiClaimMovement> movements(Long companyId, LocalDate from, LocalDate to) {
    return movements.findByCompanyIdAndMovementDateBetweenOrderByMovementDateAscIdAsc(
        companyId, from, to);
  }

  /**
   * Replays the claim movements of a period not yet processed.
   *
   * @param companyId company
   * @param from first movement date
   * @param to last movement date
   * @return movements processed now (0 when the claims module is not deployed)
   */
  public int catchUp(Long companyId, LocalDate from, LocalDate to) {
    ClaimsExperienceView view = claims.getIfAvailable();
    if (view == null) {
      return 0;
    }
    int processed = 0;
    for (ClaimMovement m : view.movements(companyId, from, to)) {
      if (!movements.existsByCompanyIdAndReference(companyId, m.reference())) {
        listener.onClaimMovement(m);
        processed++;
      }
    }
    return processed;
  }
}
