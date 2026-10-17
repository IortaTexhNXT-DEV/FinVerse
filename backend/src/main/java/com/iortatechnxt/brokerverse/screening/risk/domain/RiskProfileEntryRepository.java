package com.iortatechnxt.brokerverse.screening.risk.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Client risk-profile history (SNSRP-302, 304; insert-only). */
public interface RiskProfileEntryRepository extends JpaRepository<RiskProfileEntry, Long> {

  /**
   * The history of a client, newest first.
   *
   * @param clientId client
   * @return entries
   */
  List<RiskProfileEntry> findByClientIdOrderByEffectiveAtDescIdDesc(Long clientId);

  /**
   * The latest change of a client.
   *
   * @param clientId client
   * @return the latest entry
   */
  Optional<RiskProfileEntry> findFirstByClientIdOrderByEffectiveAtDescIdDesc(Long clientId);
}
