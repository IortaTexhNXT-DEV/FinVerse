package com.iortatechnxt.brokerverse.submitted.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Nominated package rates (FR-SP-066). */
public interface SbmNominatedRateRepository extends JpaRepository<SbmNominatedRate, Long> {

  /**
   * The rates of a company.
   *
   * @param companyId company
   * @return rates by segment, classification and insurer
   */
  List<SbmNominatedRate> findByCompanyIdOrderBySegmentAscVehicleTypeAscInsurerCodeAscIdAsc(
      Long companyId);

  /**
   * The rates of a segment and insurer.
   *
   * @param companyId company
   * @param segment segment
   * @param insurerCode insurer
   * @return rates
   */
  List<SbmNominatedRate> findByCompanyIdAndSegmentAndInsurerCode(
      Long companyId, String segment, String insurerCode);

  /**
   * Rates in a record status (approval inbox).
   *
   * @param status record status
   * @return rates
   */
  List<SbmNominatedRate> findByRecordStatus(RecordStatus status);
}
