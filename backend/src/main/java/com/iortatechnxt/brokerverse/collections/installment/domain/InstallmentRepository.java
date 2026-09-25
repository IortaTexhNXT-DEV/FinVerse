package com.iortatechnxt.brokerverse.collections.installment.domain;

import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.InstallmentStatus;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Installments (BRCLXN.053), read across plans for the work lists and escalation. */
public interface InstallmentRepository extends JpaRepository<Installment, Long> {

  /**
   * Installments of live plans due up to a date and not paid, oldest due first (installments due
   * and overdue work list).
   *
   * @param companyId company
   * @param statuses installment statuses
   * @param until last due date
   * @param pageable page
   * @return installments with their plan
   */
  @Query(
      value =
          "select i from Installment i join fetch i.plan p where p.companyId = :companyId"
              + " and p.status = :planStatus and i.status in :statuses and i.dueDate <= :until"
              + " order by i.dueDate, i.id",
      countQuery =
          "select count(i) from Installment i join i.plan p where p.companyId = :companyId"
              + " and p.status = :planStatus and i.status in :statuses and i.dueDate <= :until")
  Page<Installment> due(
      @Param("companyId") Long companyId,
      @Param("planStatus") PlanStatus planStatus,
      @Param("statuses") Collection<InstallmentStatus> statuses,
      @Param("until") LocalDate until,
      Pageable pageable);

  /**
   * Installments of live plans in a status that bill a booked invoice (escalation basis
   * INSTALLMENT_OVERDUE_DAYS).
   *
   * @param companyId company
   * @param planStatus ACTIVE
   * @param status OVERDUE
   * @return installments with their plan
   */
  @Query(
      "select i from Installment i join fetch i.plan p where p.companyId = :companyId"
          + " and p.status = :planStatus and i.status = :status and i.invoiceNo is not null")
  List<Installment> inStatus(
      @Param("companyId") Long companyId,
      @Param("planStatus") PlanStatus planStatus,
      @Param("status") InstallmentStatus status);
}
