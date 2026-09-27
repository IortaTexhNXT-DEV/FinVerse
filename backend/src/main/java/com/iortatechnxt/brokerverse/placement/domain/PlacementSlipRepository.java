package com.iortatechnxt.brokerverse.placement.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Placement slips. */
public interface PlacementSlipRepository extends JpaRepository<PlacementSlip, Long> {

  /**
   * Slips of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return slips
   */
  Page<PlacementSlip> findByCompanyIdOrderByIdDesc(Long companyId, Pageable pageable);

  /**
   * Slips of a company in a status, newest first.
   *
   * @param companyId company
   * @param status status
   * @param pageable page
   * @return slips
   */
  Page<PlacementSlip> findByCompanyIdAndStatusOrderByIdDesc(
      Long companyId, SlipStatus status, Pageable pageable);

  /**
   * Every version of the slips that cover an account, newest first.
   *
   * @param arn Account Reference Number
   * @return slips
   */
  @Query(
      "select distinct s from PlacementSlip s join s.accounts a where a.arn = :arn"
          + " order by s.id desc")
  List<PlacementSlip> findByArn(@Param("arn") String arn);
}
