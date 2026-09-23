package com.iortatechnxt.finverse.investment.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link InvestmentHolding}. */
public interface InvestmentHoldingRepository extends JpaRepository<InvestmentHolding, Long> {

  /**
   * Holdings in a status (month-end run candidates).
   *
   * @param companyId company
   * @param status status
   * @return holdings by holding number
   */
  List<InvestmentHolding> findByCompanyIdAndStatusOrderByHoldingNo(
      Long companyId, HoldingStatus status);

  /**
   * Finds a holding by security code (first match).
   *
   * @param companyId company
   * @param securityCode security code
   * @return holding if present
   */
  Optional<InvestmentHolding> findFirstByCompanyIdAndSecurityCode(
      Long companyId, String securityCode);

  /**
   * Searches holdings.
   *
   * @param companyId company
   * @param statuses statuses to include
   * @param portfolioId portfolio filter (null = all)
   * @param term lower-case fragment of holding no., security code or description ("" = all)
   * @return holdings by holding number
   */
  @Query(
      """
      select h from InvestmentHolding h
      where h.companyId = :companyId and h.status in :statuses
        and (:portfolioId is null or h.portfolio.id = :portfolioId)
        and (lower(h.holdingNo) like concat('%', :term, '%')
             or lower(coalesce(h.securityCode, '')) like concat('%', :term, '%')
             or lower(h.description) like concat('%', :term, '%'))
      order by h.holdingNo
      """)
  List<InvestmentHolding> search(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<HoldingStatus> statuses,
      @Param("portfolioId") Long portfolioId,
      @Param("term") String term);
}
