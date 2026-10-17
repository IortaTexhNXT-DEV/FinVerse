package com.iortatechnxt.brokerverse.acsl.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** GL-SL control account configurations (ACSL 2.13.2). */
public interface GlSlControlRepository extends JpaRepository<GlSlControl, Long> {

  /**
   * Configurations of a company.
   *
   * @param companyId company
   * @return configurations by account
   */
  List<GlSlControl> findByCompanyIdOrderByAccountCode(Long companyId);

  /**
   * The configuration of an account.
   *
   * @param companyId company
   * @param accountCode account
   * @return configuration
   */
  Optional<GlSlControl> findByCompanyIdAndAccountCode(Long companyId, String accountCode);
}
