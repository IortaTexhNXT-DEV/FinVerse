package com.iortatechnxt.finverse.payables.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link PettyCashDisbursement}. */
public interface PettyCashDisbursementRepository
    extends JpaRepository<PettyCashDisbursement, Long> {

  /**
   * Lists vouchers of a fund in a date range.
   *
   * @param fundId fund
   * @param from from date
   * @param to to date
   * @return vouchers newest first
   */
  @Query(
      """
      select d from PettyCashDisbursement d
      where d.fundId = :fundId and d.disbursementDate between :from and :to
      order by d.disbursementDate desc, d.id desc
      """)
  List<PettyCashDisbursement> search(
      @Param("fundId") Long fundId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  /**
   * Approved vouchers of a fund not yet on a reimbursement claim.
   *
   * @param fundId fund
   * @return vouchers oldest first
   */
  @Query(
      """
      select d from PettyCashDisbursement d
      where d.fundId = :fundId and d.reimbursementId is null
        and d.status = com.iortatechnxt.finverse.payables.domain.PettyCashStatus.APPROVED
      order by d.disbursementDate, d.id
      """)
  List<PettyCashDisbursement> findUnclaimed(@Param("fundId") Long fundId);

  /**
   * Vouchers by id.
   *
   * @param ids ids
   * @return vouchers
   */
  List<PettyCashDisbursement> findByIdIn(Collection<Long> ids);

  /**
   * Vouchers of a claim.
   *
   * @param reimbursementId claim
   * @return vouchers
   */
  List<PettyCashDisbursement> findByReimbursementIdOrderByDisbursementDate(Long reimbursementId);

  /**
   * Disbursement vouchers in one status across companies (approval inbox).
   *
   * @param status status
   * @return vouchers, oldest first
   */
  List<PettyCashDisbursement> findByStatusOrderById(PettyCashStatus status);
}
