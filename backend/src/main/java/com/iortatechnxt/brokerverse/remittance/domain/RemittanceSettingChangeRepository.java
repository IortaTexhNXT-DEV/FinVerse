package com.iortatechnxt.brokerverse.remittance.domain;

import com.iortatechnxt.brokerverse.remittance.domain.RemittanceSettingChange.Status;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Changes of the Remittance Settings. */
public interface RemittanceSettingChangeRepository
    extends JpaRepository<RemittanceSettingChange, Long> {

  /**
   * The settings in force.
   *
   * @param companyId company
   * @param status APPROVED
   * @return latest approved change
   */
  Optional<RemittanceSettingChange> findFirstByCompanyIdAndStatusOrderByIdDesc(
      Long companyId, Status status);

  /**
   * Changes of a company, newest first.
   *
   * @param companyId company
   * @return changes
   */
  List<RemittanceSettingChange> findTop50ByCompanyIdOrderByIdDesc(Long companyId);

  /**
   * Whether a change waits for approval.
   *
   * @param companyId company
   * @param status PENDING
   * @return true when one waits
   */
  boolean existsByCompanyIdAndStatus(Long companyId, Status status);
}
