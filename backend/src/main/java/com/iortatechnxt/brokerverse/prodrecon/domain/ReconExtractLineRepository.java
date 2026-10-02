package com.iortatechnxt.brokerverse.prodrecon.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Lines of the production register extracts. */
public interface ReconExtractLineRepository extends JpaRepository<ReconExtractLine, Long> {

  /**
   * Lines of an extract in order.
   *
   * @param extractId extract
   * @param pageable page
   * @return lines
   */
  Page<ReconExtractLine> findByExtractIdOrderByLineNoAsc(Long extractId, Pageable pageable);

  /**
   * Every line of an extract in order.
   *
   * @param extractId extract
   * @return lines
   */
  List<ReconExtractLine> findByExtractIdOrderByLineNoAsc(Long extractId);

  /**
   * Invoice numbers already sent in a cycle's extracts.
   *
   * @param cycleId cycle
   * @return invoice numbers
   */
  @Query(
      "select l.invoiceNo from ReconExtractLine l, ReconExtract e"
          + " where e.id = l.extractId and e.cycleId = :cycleId")
  List<String> invoicesOfCycle(@Param("cycleId") Long cycleId);
}
