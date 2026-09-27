package com.iortatechnxt.brokerverse.reinsurance.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Aggregate queries on cession lines (statements of account, bordereaux). */
public interface CessionLineRepository extends JpaRepository<CessionLine, Long> {

  /**
   * Base-currency premium and commission ceded to a treaty participant in a period.
   *
   * @param treatyId treaty
   * @param partyId participant
   * @param from first RI accounting date
   * @param to last RI accounting date
   * @return premium (first) and commission (second)
   */
  @Query(
      "select new com.iortatechnxt.brokerverse.reinsurance.domain.AmountPair(sum(l.basePremium),"
          + " sum(l.baseCommission)) from CessionLine l where l.treatyId = :treatyId"
          + " and l.partyId = :partyId and l.cession.riDate between :from and :to")
  AmountPair premiumAndCommission(
      @Param("treatyId") Long treatyId,
      @Param("partyId") Long partyId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /**
   * Accounting references of the cessions to a treaty participant in a period.
   *
   * @param treatyId treaty
   * @param partyId participant
   * @param from first RI accounting date
   * @param to last RI accounting date
   * @return source references of the open items
   */
  @Query(
      "select distinct l.postingRef from CessionLine l where l.treatyId = :treatyId"
          + " and l.partyId = :partyId and l.postingRef is not null"
          + " and l.cession.riDate between :from and :to")
  List<String> postingRefs(
      @Param("treatyId") Long treatyId,
      @Param("partyId") Long partyId,
      @Param("from") LocalDate from,
      @Param("to") LocalDate to);

  /**
   * Lines of a treaty in a period, with their cession (treaty bordereau).
   *
   * @param treatyId treaty
   * @param from first RI accounting date
   * @param to last RI accounting date
   * @return lines
   */
  @Query(
      "select l from CessionLine l join fetch l.cession c where l.treatyId = :treatyId"
          + " and c.riDate between :from and :to order by c.riDate, c.cessionNo, l.riskLineNo")
  List<CessionLine> ofTreaty(
      @Param("treatyId") Long treatyId, @Param("from") LocalDate from, @Param("to") LocalDate to);

  /**
   * Facultative lines ceded to reinsurers (endorsements after placement) in a period.
   *
   * @param companyId company
   * @param from first RI accounting date
   * @param to last RI accounting date
   * @return lines with their cession
   */
  @Query(
      "select l from CessionLine l join fetch l.cession c where c.companyId = :companyId"
          + " and l.layer = com.iortatechnxt.brokerverse.reinsurance.domain.RiLayer.FAC"
          + " and l.partyId is not null and c.riDate between :from and :to")
  List<CessionLine> cededFac(
      @Param("companyId") Long companyId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
