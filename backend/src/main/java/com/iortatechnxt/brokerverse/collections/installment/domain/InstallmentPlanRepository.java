package com.iortatechnxt.brokerverse.collections.installment.domain;

import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanSource;
import com.iortatechnxt.brokerverse.collections.installment.domain.PlanEnums.PlanStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Installment plans (BRCLXN.053/058). */
public interface InstallmentPlanRepository extends JpaRepository<InstallmentPlan, Long> {

  /**
   * A plan with its installments.
   *
   * @param id plan
   * @return plan
   */
  @EntityGraph(attributePaths = "installments", type = EntityGraph.EntityGraphType.LOAD)
  Optional<InstallmentPlan> findWithInstallmentsById(Long id);

  /**
   * Plans of a company by status, searched by plan, account, invoice, client or assured.
   *
   * @param companyId company
   * @param statuses statuses
   * @param like lower-case pattern, {@code %} for all
   * @param pageable page
   * @return plans
   */
  @Query(
      "select p from InstallmentPlan p where p.companyId = :companyId and p.status in :statuses"
          + " and (lower(p.planNo) like :like or lower(p.arn) like :like"
          + " or lower(coalesce(p.invoiceNo, '')) like :like or lower(p.clientCode) like :like"
          + " or lower(p.assuredName) like :like)")
  Page<InstallmentPlan> search(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<PlanStatus> statuses,
      @Param("like") String like,
      Pageable pageable);

  /**
   * The live plan of an invoice.
   *
   * @param companyId company
   * @param invoiceNo invoice
   * @param status ACTIVE
   * @return plan
   */
  Optional<InstallmentPlan> findFirstByCompanyIdAndInvoiceNoAndStatus(
      Long companyId, String invoiceNo, PlanStatus status);

  /**
   * The live policy-year plan of an account.
   *
   * @param companyId company
   * @param arn account
   * @param source POLICY_YEARS
   * @param status ACTIVE
   * @return plan
   */
  Optional<InstallmentPlan> findFirstByCompanyIdAndArnAndSourceAndStatus(
      Long companyId, String arn, PlanSource source, PlanStatus status);

  /**
   * Plans of an account, newest first.
   *
   * @param arn account
   * @return plans
   */
  List<InstallmentPlan> findByArnOrderByIdDesc(String arn);

  /**
   * Ids of the plans in a status (daily refresh).
   *
   * @param status status
   * @return ids
   */
  @Query("select p.id from InstallmentPlan p where p.status = :status order by p.id")
  List<Long> idsByStatus(@Param("status") PlanStatus status);
}
