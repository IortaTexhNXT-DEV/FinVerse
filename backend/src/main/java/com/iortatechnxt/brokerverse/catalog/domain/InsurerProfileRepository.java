package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Panel insurers. */
public interface InsurerProfileRepository extends JpaRepository<InsurerProfile, Long> {

  /**
   * An insurer by party code.
   *
   * @param companyId company
   * @param partyCode party code
   * @return insurer
   */
  Optional<InsurerProfile> findByCompanyIdAndPartyCode(Long companyId, String partyCode);

  /**
   * Insurers of a company by name.
   *
   * @param companyId company
   * @return insurers
   */
  List<InsurerProfile> findByCompanyIdOrderByNameAsc(Long companyId);
}
