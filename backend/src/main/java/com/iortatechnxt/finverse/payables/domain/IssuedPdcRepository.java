package com.iortatechnxt.finverse.payables.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link IssuedPdc}. */
public interface IssuedPdcRepository extends JpaRepository<IssuedPdc, Long> {

  /**
   * Lists the register filtered by status and cheque date.
   *
   * @param companyId company
   * @param statuses statuses
   * @param from cheque date from
   * @param to cheque date to
   * @return cheques by cheque date
   */
  @Query(
      """
      select p from IssuedPdc p
      where p.companyId = :companyId and p.status in :statuses
        and p.chequeDate between :from and :to
      order by p.chequeDate, p.chequeNo
      """)
  List<IssuedPdc> search(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<IssuedPdcStatus> statuses,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /**
   * Issued cheques whose date has been reached.
   *
   * @param status status (ISSUED)
   * @param asOf date
   * @return cheques to mark due
   */
  List<IssuedPdc> findByStatusAndChequeDateLessThanEqual(IssuedPdcStatus status, LocalDate asOf);

  /**
   * The live cheque of a voucher.
   *
   * @param voucherId voucher
   * @param statuses outstanding statuses
   * @return cheque
   */
  Optional<IssuedPdc> findFirstByVoucherIdAndStatusIn(
      Long voucherId, Collection<IssuedPdcStatus> statuses);

  /**
   * All cheques of a voucher, oldest first.
   *
   * @param voucherId voucher
   * @return cheques
   */
  List<IssuedPdc> findByVoucherIdOrderById(Long voucherId);
}
