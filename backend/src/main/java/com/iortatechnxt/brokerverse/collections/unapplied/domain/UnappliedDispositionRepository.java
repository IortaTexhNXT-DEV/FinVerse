package com.iortatechnxt.brokerverse.collections.unapplied.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Collector dispositions of unapplied payments (BRCLXN.031/033, 040). */
public interface UnappliedDispositionRepository extends JpaRepository<UnappliedDisposition, Long> {

  /**
   * Dispositions of an item, newest first.
   *
   * @param companyId company
   * @param unappliedRef item reference
   * @return dispositions
   */
  List<UnappliedDisposition> findByCompanyIdAndUnappliedRefOrderByIdDesc(
      Long companyId, String unappliedRef);

  /**
   * The latest disposition of each of some items.
   *
   * @param companyId company
   * @param refs item references
   * @return one disposition per item that has one
   */
  @Query(
      "select d from UnappliedDisposition d where d.companyId = :companyId"
          + " and d.unappliedRef in :refs and d.id = (select max(x.id) from UnappliedDisposition x"
          + " where x.companyId = d.companyId and x.unappliedRef = d.unappliedRef)")
  List<UnappliedDisposition> latest(
      @Param("companyId") Long companyId, @Param("refs") Collection<String> refs);
}
