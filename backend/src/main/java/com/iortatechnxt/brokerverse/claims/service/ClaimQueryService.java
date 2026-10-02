package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimRepository;
import com.iortatechnxt.brokerverse.claims.domain.MovementLine;
import com.iortatechnxt.brokerverse.claims.domain.MovementLineRepository;
import com.iortatechnxt.brokerverse.claims.domain.MovementTotal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read side of the claim movement ledger for screens, reports and the kernel views: movement
 * history of a claim and per-claim estimate / paid figures over any period (bulk aggregate query).
 */
@Service
@Transactional(readOnly = true)
public class ClaimQueryService {

  /** Earliest movement date used for "as at" figures. */
  public static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);

  private final ClaimRepository claims;
  private final MovementLineRepository lines;

  /**
   * Creates the service.
   *
   * @param claims claim repository
   * @param lines movement ledger
   */
  public ClaimQueryService(ClaimRepository claims, MovementLineRepository lines) {
    this.claims = claims;
    this.lines = lines;
  }

  /**
   * Movement history of a claim.
   *
   * @param claimId claim
   * @return lines in date order
   */
  public List<MovementLine> movements(Long claimId) {
    return lines.findByClaimIdOrderByMovementDateAscIdAsc(claimId);
  }

  /**
   * Movement lines of a company in a period, with their claim.
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return lines in date order
   */
  public List<MovementLine> movements(Long companyId, LocalDate from, LocalDate to) {
    return lines.findByCompanyIdAndMovementDateBetweenOrderByMovementDateAscIdAsc(
        companyId, from, to);
  }

  /**
   * Movement totals per claim over a period.
   *
   * @param companyId company
   * @param from first movement date
   * @param to last movement date
   * @return totals grouped by claim id
   */
  public Map<Long, List<MovementTotal>> totals(Long companyId, LocalDate from, LocalDate to) {
    return lines.totals(companyId, from, to).stream()
        .collect(Collectors.groupingBy(MovementTotal::claimId));
  }

  /**
   * Figures of every claim over a period in base currency (reports).
   *
   * @param companyId company
   * @param from first movement date
   * @param to last movement date
   * @return figures by claim id (claims without movements are absent)
   */
  public Map<Long, ClaimFigures> baseFigures(Long companyId, LocalDate from, LocalDate to) {
    return totals(companyId, from, to).entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> ClaimFigures.of(e.getValue(), true)));
  }

  /**
   * Claims of a company.
   *
   * @param companyId company
   * @return claims ordered by number
   */
  public List<Claim> claims(Long companyId) {
    return claims.findByCompanyIdOrderByClaimNo(companyId);
  }

  /**
   * Finds a claim by number.
   *
   * @param companyId company
   * @param claimNo claim number
   * @return claim
   */
  public Optional<Claim> findByNumber(Long companyId, String claimNo) {
    return claims.findByCompanyIdAndClaimNo(companyId, claimNo);
  }
}
