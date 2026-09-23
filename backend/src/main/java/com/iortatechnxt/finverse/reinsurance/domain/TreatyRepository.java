package com.iortatechnxt.finverse.reinsurance.domain;

import com.iortatechnxt.finverse.common.domain.RecordStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Treaties. */
public interface TreatyRepository extends JpaRepository<Treaty, Long> {

  /**
   * Treaties of a company, latest underwriting year first.
   *
   * @param companyId company
   * @return treaties
   */
  List<Treaty> findByCompanyIdOrderByUwYearDescBusinessLineAscCodeAsc(Long companyId);

  /**
   * Whether a code is taken.
   *
   * @param companyId company
   * @param code code
   * @return true when taken
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);

  /**
   * Finds a treaty by code.
   *
   * @param companyId company
   * @param code code
   * @return treaty
   */
  Optional<Treaty> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Treaties of a programme (line of business and underwriting year) in a status.
   *
   * @param companyId company
   * @param businessLine line of business
   * @param uwYear underwriting year
   * @param status record status
   * @return treaties
   */
  List<Treaty> findByCompanyIdAndBusinessLineAndUwYearAndRecordStatus(
      Long companyId, String businessLine, int uwYear, RecordStatus status);

  /**
   * Treaties of every company in a status (approval inbox).
   *
   * @param status record status
   * @return treaties
   */
  List<Treaty> findByRecordStatus(RecordStatus status);

  /**
   * Treaties of a company in a status.
   *
   * @param companyId company
   * @param status record status
   * @return treaties
   */
  List<Treaty> findByCompanyIdAndRecordStatusOrderByCode(Long companyId, RecordStatus status);
}
