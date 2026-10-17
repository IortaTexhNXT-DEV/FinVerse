package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Renewal letters. */
public interface RenewalLetterRepository extends JpaRepository<RenewalLetter, Long> {

  /**
   * Letters of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return letters
   */
  List<RenewalLetter> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * A letter by number.
   *
   * @param companyId company
   * @param letterNo letter number
   * @return letter
   */
  Optional<RenewalLetter> findByCompanyIdAndLetterNo(Long companyId, String letterNo);

  /**
   * Letters of a company in a type and status.
   *
   * @param companyId company
   * @param type type
   * @param statuses statuses
   * @return letters, newest first
   */
  List<RenewalLetter> findByCompanyIdAndTypeAndStatusInOrderByIdDesc(
      Long companyId, LetterType type, Collection<LetterStatus> statuses);

  /**
   * Letters in a status (delivery refresh).
   *
   * @param status status
   * @return letters
   */
  List<RenewalLetter> findByStatus(LetterStatus status);
}
