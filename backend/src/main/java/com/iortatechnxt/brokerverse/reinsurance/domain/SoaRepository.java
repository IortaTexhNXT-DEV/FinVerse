package com.iortatechnxt.brokerverse.reinsurance.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Statements of account. */
public interface SoaRepository extends JpaRepository<Soa, Long> {

  /**
   * Statements of a company, latest first.
   *
   * @param companyId company
   * @return statements
   */
  List<Soa> findByCompanyIdOrderBySoaYearDescQuarterDescSoaNoAsc(Long companyId);

  /**
   * Statement of a participant for a quarter.
   *
   * @param treatyId treaty
   * @param partyId participant
   * @param year year
   * @param quarter quarter
   * @return statement
   */
  Optional<Soa> findByTreatyIdAndPartyIdAndSoaYearAndQuarter(
      Long treatyId, Long partyId, int year, int quarter);

  /**
   * Statements of a participant (reserve balances).
   *
   * @param treatyId treaty
   * @param partyId participant
   * @return statements
   */
  List<Soa> findByTreatyIdAndPartyId(Long treatyId, Long partyId);

  /**
   * Statements of every company in a status (approval inbox).
   *
   * @param status status
   * @return statements
   */
  List<Soa> findByStatus(SoaStatus status);
}
