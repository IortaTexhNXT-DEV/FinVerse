package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.remittance.domain.RemittanceDeduction.DeductionStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Remittance deductions (ACSL 2.9.2). */
public interface RemittanceDeductionRepository
    extends JpaRepository<RemittanceDeduction, Long>,
        JpaSpecificationExecutor<RemittanceDeduction> {

  /**
   * The deductions of an insurer and currency in a stage, oldest confirmation first (consumption
   * order).
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param currency currency
   * @param stage stage
   * @return deductions
   */
  List<RemittanceDeduction>
      findByCompanyIdAndInsurerCodeAndCurrencyAndStageOrderByConfirmedAtAscIdAsc(
          Long companyId, String insurerCode, String currency, DeductionStage stage);
}
