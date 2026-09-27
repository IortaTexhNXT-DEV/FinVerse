package com.iortatechnxt.brokerverse.party.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence for {@link Party}. */
public interface PartyRepository extends JpaRepository<Party, Long> {

  /**
   * Finds a party by code.
   *
   * @param companyId company
   * @param code party code
   * @return party if present
   */
  Optional<Party> findByCompanyIdAndCode(Long companyId, String code);

  /**
   * Checks whether a code is taken.
   *
   * @param companyId company
   * @param code party code
   * @return true when taken
   */
  boolean existsByCompanyIdAndCode(Long companyId, String code);

  /**
   * Lists parties of given types.
   *
   * @param companyId company
   * @param types types
   * @return parties ordered by name
   */
  List<Party> findByCompanyIdAndPartyTypeInOrderByName(Long companyId, Collection<PartyType> types);

  /**
   * Searches parties by code or name fragment, optionally restricted to types.
   *
   * @param companyId company
   * @param term search term (lower case)
   * @param types types to include
   * @return matches ordered by name
   */
  @Query(
      """
      select p from Party p
      where p.companyId = :companyId and p.partyType in :types
        and (lower(p.code) like concat(:term, '%') or lower(p.name) like concat('%', :term, '%'))
      order by p.name
      """)
  List<Party> search(
      @Param("companyId") Long companyId,
      @Param("term") String term,
      @Param("types") Collection<PartyType> types);
}
