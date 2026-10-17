package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer renewable lists. */
public interface InsurerRenewableRiskRepository extends JpaRepository<InsurerRenewableRisk, Long> {

  /**
   * The rows of a company.
   *
   * @param companyId company
   * @return rows by insurer and risk code
   */
  List<InsurerRenewableRisk> findByCompanyIdOrderByInsurerCodeAscRiskCodeAscIdAsc(Long companyId);

  /**
   * The rows of an insurer.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @return rows
   */
  List<InsurerRenewableRisk> findByCompanyIdAndInsurerCode(Long companyId, String insurerCode);

  /**
   * The rows in a maker-checker status.
   *
   * @param status status
   * @return rows
   */
  List<InsurerRenewableRisk> findByRecordStatus(RecordStatus status);
}
