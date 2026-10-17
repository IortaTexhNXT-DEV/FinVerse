package com.iortatechnxt.brokerverse.collections.escalation.domain;

import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Stage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Escalation cases (BRCLXN.049/050). */
public interface EscalationRepository extends JpaRepository<Escalation, Long> {

  /**
   * An escalation with its invoices.
   *
   * @param id escalation
   * @return escalation
   */
  @EntityGraph(attributePaths = "items", type = EntityGraph.EntityGraphType.LOAD)
  Optional<Escalation> findWithItemsById(Long id);

  /**
   * Escalations of a company by stage, searched by number, account, client, assured or invoice.
   *
   * @param companyId company
   * @param stages stages
   * @param like lower-case pattern, {@code %} for all
   * @param pageable page
   * @return escalations
   */
  @Query(
      "select e from Escalation e where e.companyId = :companyId and e.status in :stages"
          + " and (lower(e.escalationNo) like :like or lower(e.arn) like :like"
          + " or lower(e.clientCode) like :like or lower(e.assuredName) like :like"
          + " or exists (select 1 from EscalationItem i where i.escalation = e"
          + " and lower(i.invoiceNo) like :like))")
  Page<Escalation> search(
      @Param("companyId") Long companyId,
      @Param("stages") Collection<Stage> stages,
      @Param("like") String like,
      Pageable pageable);

  /**
   * Whether an escalation exists with a key (automatic escalation idempotency).
   *
   * @param dedupKey key
   * @return true when raised already
   */
  boolean existsByDedupKey(String dedupKey);

  /**
   * Whether an invoice has an escalation of a rule in one of the stages.
   *
   * @param ruleCode rule
   * @param invoiceNo invoice
   * @param stages stages
   * @return true when one exists
   */
  @Query(
      "select count(e) > 0 from Escalation e join e.items i where e.ruleCode = :ruleCode"
          + " and i.invoiceNo = :invoiceNo and e.status in :stages")
  boolean existsForRule(
      @Param("ruleCode") String ruleCode,
      @Param("invoiceNo") String invoiceNo,
      @Param("stages") Collection<Stage> stages);

  /**
   * Escalations of an invoice, newest first.
   *
   * @param invoiceNo invoice
   * @return escalations
   */
  @Query(
      "select distinct e from Escalation e join e.items i where i.invoiceNo = :invoiceNo"
          + " order by e.id desc")
  List<Escalation> forInvoice(@Param("invoiceNo") String invoiceNo);

  /**
   * Ids of the escalations in the stages (auto-close, SLA check).
   *
   * @param stages stages
   * @return ids
   */
  @Query("select e.id from Escalation e where e.status in :stages order by e.id")
  List<Long> idsIn(@Param("stages") Collection<Stage> stages);

  /**
   * Escalations in the stages, with their invoices.
   *
   * @param stages stages
   * @return escalations
   */
  @EntityGraph(attributePaths = "items", type = EntityGraph.EntityGraphType.LOAD)
  List<Escalation> findByStatusIn(Collection<Stage> stages);
}
