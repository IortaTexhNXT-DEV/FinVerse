package com.iortatechnxt.brokerverse.frbs.domain;

import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Service-fee runs (FRBS 2.10.0). */
public interface ServiceFeeRunRepository extends JpaRepository<ServiceFeeRun, Long> {

  /**
   * A run by number.
   *
   * @param runNo run number
   * @return run
   */
  Optional<ServiceFeeRun> findByRunNo(String runNo);

  /**
   * Searches runs.
   *
   * @param companyId company
   * @param stage stage, null for all
   * @param q run number contains (lower case pattern), null for all
   * @param pageable page
   * @return runs
   */
  @Query(
      "select r from ServiceFeeRun r where r.companyId = :companyId"
          + " and (:stage is null or r.stage = :stage)"
          + " and (:q is null or lower(r.runNo) like :q)")
  Page<ServiceFeeRun> search(
      @Param("companyId") Long companyId,
      @Param("stage") RunStage stage,
      @Param("q") String q,
      Pageable pageable);

  /**
   * Runs per stage.
   *
   * @param companyId company
   * @return stage and count pairs
   */
  @Query(
      "select r.stage, count(r) from ServiceFeeRun r where r.companyId = :companyId"
          + " group by r.stage")
  List<Object[]> countByStage(@Param("companyId") Long companyId);
}
