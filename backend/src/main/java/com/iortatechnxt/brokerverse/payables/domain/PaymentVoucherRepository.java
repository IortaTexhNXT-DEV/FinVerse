package com.iortatechnxt.brokerverse.payables.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link PaymentVoucher}. */
public interface PaymentVoucherRepository extends JpaRepository<PaymentVoucher, Long> {

  /**
   * Loads a voucher with its allocations.
   *
   * @param id id
   * @return voucher
   */
  @EntityGraph(
      type = EntityGraph.EntityGraphType.LOAD,
      attributePaths = {"allocations"})
  Optional<PaymentVoucher> findWithAllocationsById(Long id);

  /**
   * Searches vouchers.
   *
   * @param companyId company
   * @param status status or null
   * @param partyCode party or null
   * @param from voucher date from
   * @param to voucher date to
   * @param pageable page
   * @return page of vouchers
   */
  @Query(
      """
      select v from PaymentVoucher v
      where v.companyId = :companyId
        and (:status is null or v.status = :status)
        and (:partyCode is null or v.partyCode = :partyCode)
        and v.voucherDate between :from and :to
      """)
  Page<PaymentVoucher> search(
      @Param("companyId") Long companyId,
      @Param("status") VoucherStatus status,
      @Param("partyCode") String partyCode,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      Pageable pageable);

  /**
   * Lists approved vouchers of a bank account and modes in a date range (payment notification).
   *
   * @param bankAccountId bank account
   * @param modes payment modes
   * @param from from date
   * @param to to date
   * @return vouchers by date and number
   */
  @Query(
      """
      select v from PaymentVoucher v
      where v.bankAccountId = :bankAccountId and v.paymentMode in :modes
        and v.status = com.iortatechnxt.brokerverse.payables.domain.VoucherStatus.APPROVED
        and v.voucherDate between :from and :to
      order by v.voucherDate, v.voucherNo
      """)
  List<PaymentVoucher> findApproved(
      @Param("bankAccountId") Long bankAccountId,
      @Param("modes") Collection<PaymentMode> modes,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /**
   * Sums the amounts of live vouchers (draft, pending) that already reserve an open item, so the
   * same payable is not put on two vouchers.
   *
   * @param openItemId open item
   * @param excludeVoucherId voucher being edited (or -1)
   * @return reserved amount
   */
  @Query(
      """
      select coalesce(sum(a.amount), 0) from VoucherAllocation a join a.voucher v
      where a.openItemId = :openItemId and v.id <> :excludeVoucherId
        and v.status in (com.iortatechnxt.brokerverse.payables.domain.VoucherStatus.DRAFT,
                         com.iortatechnxt.brokerverse.payables.domain.VoucherStatus.PENDING_APPROVAL)
      """)
  BigDecimal reservedAmount(
      @Param("openItemId") Long openItemId, @Param("excludeVoucherId") Long excludeVoucherId);

  /**
   * Vouchers in one status across companies (approval inbox).
   *
   * @param status status
   * @return vouchers, oldest first
   */
  List<PaymentVoucher> findByStatusOrderById(VoucherStatus status);
}
