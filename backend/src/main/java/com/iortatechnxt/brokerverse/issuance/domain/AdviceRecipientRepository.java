package com.iortatechnxt.brokerverse.issuance.domain;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurance Advice recipients of the mortgagee banks (FR-NB-107). */
public interface AdviceRecipientRepository extends JpaRepository<AdviceRecipient, Long> {

  /**
   * The set-ups of a company.
   *
   * @param companyId company
   * @return set-ups by bank and segment
   */
  List<AdviceRecipient> findByCompanyIdOrderByMortgageeBankAscMarketSegmentAscIdAsc(Long companyId);

  /**
   * The set-ups of a mortgagee bank.
   *
   * @param companyId company
   * @param mortgageeBank mortgagee bank
   * @return set-ups
   */
  List<AdviceRecipient> findByCompanyIdAndMortgageeBank(Long companyId, String mortgageeBank);

  /**
   * Set-ups in a record status (approval inbox).
   *
   * @param status record status
   * @return set-ups
   */
  List<AdviceRecipient> findByRecordStatus(RecordStatus status);
}
