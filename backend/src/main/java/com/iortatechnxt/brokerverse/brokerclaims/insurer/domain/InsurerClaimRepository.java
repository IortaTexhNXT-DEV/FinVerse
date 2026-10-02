package com.iortatechnxt.brokerverse.brokerclaims.insurer.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Insurer claim lines (BRCLM.023/043). */
public interface InsurerClaimRepository extends JpaRepository<InsurerClaim, Long> {

  /**
   * The insurer lines of a claim in the order they were added.
   *
   * @param claimId claim
   * @return lines
   */
  List<InsurerClaim> findByClaimIdOrderByIdAsc(Long claimId);

  /**
   * The insurer lines of several claims (lists and feeds).
   *
   * @param claimIds claims
   * @return lines
   */
  List<InsurerClaim> findByClaimIdInOrderByIdAsc(Collection<Long> claimIds);

  /**
   * One line of a claim.
   *
   * @param id line
   * @param claimId claim
   * @return the line when it belongs to the claim
   */
  Optional<InsurerClaim> findByIdAndClaimId(Long id, Long claimId);

  /**
   * Lines of any claim of the company carrying an insurer claim number (043 AC5 search, the
   * cross-claim duplicate warning and the bulk uploads).
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param number insurer claim number (any case)
   * @return lines
   */
  @Query(
      "select l from InsurerClaim l where l.companyId = :companyId and l.insurerCode = :insurerCode"
          + " and lower(l.insurerClaimNo) = lower(:number) order by l.id")
  List<InsurerClaim> findNumbered(
      @Param("companyId") Long companyId,
      @Param("insurerCode") String insurerCode,
      @Param("number") String number);

  /**
   * Claims whose insurer claim numbers contain a text (worklist and cover search, 043 AC5).
   *
   * @param companyId company
   * @param text text, lower case with wildcards
   * @return claim ids
   */
  @Query(
      "select distinct l.claimId from InsurerClaim l where l.companyId = :companyId"
          + " and lower(l.insurerClaimNo) like :text")
  List<Long> claimIdsByNumber(@Param("companyId") Long companyId, @Param("text") String text);
}
