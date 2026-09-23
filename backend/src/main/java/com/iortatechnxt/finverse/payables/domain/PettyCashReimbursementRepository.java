package com.iortatechnxt.finverse.payables.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link PettyCashReimbursement}. */
public interface PettyCashReimbursementRepository
    extends JpaRepository<PettyCashReimbursement, Long> {

  /**
   * Claims of a fund.
   *
   * @param fundId fund
   * @return claims newest first
   */
  List<PettyCashReimbursement> findByFundIdOrderByClaimDateDescIdDesc(Long fundId);

  /**
   * Reimbursement claims in one status across companies (approval inbox).
   *
   * @param status status
   * @return claims, oldest first
   */
  List<PettyCashReimbursement> findByStatusOrderById(PettyCashStatus status);
}
