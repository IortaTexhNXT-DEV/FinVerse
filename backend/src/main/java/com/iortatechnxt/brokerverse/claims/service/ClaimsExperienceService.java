package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimRepository;
import com.iortatechnxt.brokerverse.claims.domain.MovementTotal;
import com.iortatechnxt.brokerverse.insurance.ClaimMovement;
import com.iortatechnxt.brokerverse.insurance.ClaimsExperienceView;
import com.iortatechnxt.brokerverse.insurance.OutstandingClaim;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the insurance kernel port {@link ClaimsExperienceView} for actuarial reserving
 * and reinsurance: outstanding reserves (payment estimate − paid, loss and expense, company share)
 * as at a date, and the kernel movements of a period (see {@link KernelMovements}). Only approved,
 * posted movements exist in the ledger, so nothing pending is ever returned.
 */
@Service
@Transactional(readOnly = true)
public class ClaimsExperienceService implements ClaimsExperienceView {

  private final ClaimQueryService query;
  private final ClaimRepository claims;

  /**
   * Creates the view.
   *
   * @param query movement ledger queries
   * @param claims claim repository
   */
  public ClaimsExperienceService(ClaimQueryService query, ClaimRepository claims) {
    this.query = query;
    this.claims = claims;
  }

  @Override
  public List<OutstandingClaim> outstanding(Long companyId, LocalDate asOf) {
    Map<Long, List<MovementTotal>> totals =
        query.totals(companyId, ClaimQueryService.EARLIEST, asOf);
    return claims.findAllById(totals.keySet()).stream()
        .map(c -> outstanding(c, totals.get(c.getId())))
        .filter(o -> o.outstanding().signum() != 0)
        .sorted(Comparator.comparing(OutstandingClaim::claimNo))
        .toList();
  }

  @Override
  public List<ClaimMovement> movements(Long companyId, LocalDate from, LocalDate to) {
    return query.movements(companyId, from, to).stream()
        .flatMap(line -> KernelMovements.of(line).stream())
        .toList();
  }

  private static OutstandingClaim outstanding(Claim c, List<MovementTotal> totals) {
    BigDecimal amount = ClaimFigures.of(totals, false).paymentOutstanding(true);
    BigDecimal base = ClaimFigures.of(totals, true).paymentOutstanding(true);
    return new OutstandingClaim(
        c.getCompanyId(),
        c.getBranchId(),
        c.getId(),
        c.getClaimNo(),
        c.getPolicy().getPolicyId(),
        c.getPolicy().getBusinessLine(),
        c.getLoss().getLossDate(),
        c.getLoss().getReportedDate(),
        c.getCurrency(),
        amount,
        base);
  }
}
