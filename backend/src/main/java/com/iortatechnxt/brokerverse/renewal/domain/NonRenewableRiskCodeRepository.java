package com.iortatechnxt.brokerverse.renewal.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Non-renewable risk codes. */
public interface NonRenewableRiskCodeRepository extends JpaRepository<NonRenewableRiskCode, Long> {

  /**
   * Codes of a company.
   *
   * @param companyId company
   * @return codes
   */
  List<NonRenewableRiskCode> findByCompanyIdOrderByRiskCodeAscIdAsc(Long companyId);

  /**
   * Codes of a risk code.
   *
   * @param companyId company
   * @param riskCode risk code
   * @return codes
   */
  List<NonRenewableRiskCode> findByCompanyIdAndRiskCode(Long companyId, String riskCode);

  /**
   * Codes waiting for a checker.
   *
   * @param status status
   * @return codes
   */
  List<NonRenewableRiskCode> findByRecordStatus(RecordStatus status);
}
