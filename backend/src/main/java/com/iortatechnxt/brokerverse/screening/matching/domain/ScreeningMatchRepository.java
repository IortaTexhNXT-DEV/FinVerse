package com.iortatechnxt.brokerverse.screening.matching.domain;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Screening matches (SNSRP-301, FR-SS-031, 032). */
public interface ScreeningMatchRepository extends JpaRepository<ScreeningMatch, Long> {

  /**
   * Whether the pair was already matched under the configuration version (idempotence, FR-SS-030
   * R1).
   *
   * @param clientId client
   * @param entryId entry
   * @param entryVersion entry version
   * @param matchVersionId configuration version
   * @return true when recorded
   */
  boolean existsByClientIdAndEntryIdAndEntryVersionAndMatchVersionId(
      Long clientId, Long entryId, int entryVersion, Long matchVersionId);

  /**
   * The matches of a client in some statuses, oldest first.
   *
   * @param clientId client
   * @param statuses statuses
   * @return matches
   */
  List<ScreeningMatch> findByClientIdAndStatusInOrderByIdAsc(
      Long clientId, Collection<MatchStatus> statuses);

  /**
   * The matches of a client, newest first.
   *
   * @param clientId client
   * @return matches
   */
  List<ScreeningMatch> findByClientIdOrderByIdDesc(Long clientId);

  /**
   * The matches recorded by a run.
   *
   * @param runId run
   * @return matches
   */
  List<ScreeningMatch> findByRunIdOrderByIdAsc(Long runId);

  /**
   * Searches the matches of a company (Matches screen, FR-SS-032).
   *
   * @param companyId company
   * @param status status, {@code null} for all
   * @param listType list type, {@code null} for all
   * @param minScore lowest score, {@code null} for no bound
   * @param maxScore highest score, {@code null} for no bound
   * @param like lower-case pattern on the client name or code or the entry name, {@code %} for all
   * @param uncased true for matches not yet in a case only
   * @param pageable page
   * @return matches
   */
  @Query(
      "select m from ScreeningMatch m where m.companyId = :companyId"
          + " and (:status is null or m.status = :status)"
          + " and (:listType is null or m.listType = :listType)"
          + " and (:minScore is null or m.score >= :minScore)"
          + " and (:maxScore is null or m.score <= :maxScore)"
          + " and (lower(m.clientName) like :like or lower(m.clientCode) like :like"
          + " or lower(m.entryName) like :like)"
          + " and (:uncased = false or m.caseId is null)")
  Page<ScreeningMatch> search(
      @Param("companyId") Long companyId,
      @Param("status") MatchStatus status,
      @Param("listType") String listType,
      @Param("minScore") BigDecimal minScore,
      @Param("maxScore") BigDecimal maxScore,
      @Param("like") String like,
      @Param("uncased") boolean uncased,
      Pageable pageable);

  /**
   * Counts the matches of a company in a status not yet in a case (Screening Home tile).
   *
   * @param companyId company
   * @param status status
   * @return count
   */
  long countByCompanyIdAndStatusAndCaseIdIsNull(Long companyId, MatchStatus status);
}
