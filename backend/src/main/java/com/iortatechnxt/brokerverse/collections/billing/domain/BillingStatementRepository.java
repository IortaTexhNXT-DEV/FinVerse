package com.iortatechnxt.brokerverse.collections.billing.domain;

import com.iortatechnxt.brokerverse.collections.billing.domain.BillingStatement.StatementStatus;
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

/** Statements of account (BRCLXN.058/060). */
public interface BillingStatementRepository extends JpaRepository<BillingStatement, Long> {

  /**
   * A statement with its lines.
   *
   * @param id statement
   * @return statement
   */
  @EntityGraph(attributePaths = "lines", type = EntityGraph.EntityGraphType.LOAD)
  Optional<BillingStatement> findWithLinesById(Long id);

  /**
   * Statements of a company by status and due date, searched by SOA, account, client or assured.
   *
   * @param companyId company
   * @param statuses statuses
   * @param from first due date
   * @param to last due date
   * @param like lower-case pattern, {@code %} for all
   * @param pageable page
   * @return statements
   */
  @Query(
      "select s from BillingStatement s where s.companyId = :companyId and s.status in :statuses"
          + " and s.dueDate between :from and :to"
          + " and (lower(s.soaNo) like :like or lower(s.arn) like :like"
          + " or lower(s.clientCode) like :like or lower(s.assuredName) like :like)")
  Page<BillingStatement> search(
      @Param("companyId") Long companyId,
      @Param("statuses") Collection<StatementStatus> statuses,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to,
      @Param("like") String like,
      Pageable pageable);

  /**
   * The live statement of a plan's cycle.
   *
   * @param planId plan
   * @param cycleSeq cycle
   * @param status statuses to exclude (CANCELLED)
   * @return statement
   */
  Optional<BillingStatement> findFirstByPlanIdAndCycleSeqAndStatusNot(
      Long planId, int cycleSeq, StatementStatus status);

  /**
   * Statements of a plan, by cycle.
   *
   * @param planId plan
   * @return statements
   */
  List<BillingStatement> findByPlanIdOrderByCycleSeqAscIdAsc(Long planId);

  /**
   * Statements of an account, newest first.
   *
   * @param arn account
   * @return statements
   */
  List<BillingStatement> findByArnOrderByIdDesc(String arn);
}
