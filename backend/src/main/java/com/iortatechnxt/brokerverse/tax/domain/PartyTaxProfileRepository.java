package com.iortatechnxt.brokerverse.tax.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Party tax profiles. */
public interface PartyTaxProfileRepository extends JpaRepository<PartyTaxProfile, Long> {

  /**
   * Profiles of a company.
   *
   * @param companyId company
   * @return profiles ordered by party code
   */
  List<PartyTaxProfile> findByCompanyIdOrderByPartyCode(Long companyId);

  /**
   * Profile of a party.
   *
   * @param companyId company
   * @param partyCode party code
   * @return profile
   */
  Optional<PartyTaxProfile> findByCompanyIdAndPartyCode(Long companyId, String partyCode);

  /**
   * Whether a party has a profile.
   *
   * @param companyId company
   * @param partyId party id
   * @return true when present
   */
  boolean existsByCompanyIdAndPartyId(Long companyId, Long partyId);
}
