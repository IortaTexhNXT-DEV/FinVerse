package com.iortatechnxt.brokerverse.eb.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Members of the roster versions. */
public interface EbMemberRepository extends JpaRepository<EbMember, Long> {

  /**
   * Members of a version.
   *
   * @param rosterVersionId version
   * @return members by employee number
   */
  List<EbMember> findByRosterVersionIdOrderByEmployeeNoAsc(Long rosterVersionId);

  /**
   * A member of a version by employee number (case-insensitive).
   *
   * @param rosterVersionId version
   * @param employeeNo employee number
   * @return member
   */
  Optional<EbMember> findByRosterVersionIdAndEmployeeNoIgnoreCase(
      Long rosterVersionId, String employeeNo);

  /**
   * Members of a version matching a text (employee number or name).
   *
   * @param rosterVersionId version
   * @param text text, lower case, null for all
   * @param pageable page
   * @return members
   */
  @Query(
      "select m from EbMember m where m.rosterVersionId = :version and (:text is null"
          + " or lower(m.employeeNo) like concat('%', :text, '%')"
          + " or lower(m.lastName) like concat('%', :text, '%')"
          + " or lower(m.firstName) like concat('%', :text, '%'))")
  Page<EbMember> search(
      @Param("version") Long rosterVersionId, @Param("text") String text, Pageable pageable);

  /**
   * Active members of a version.
   *
   * @param rosterVersionId version
   * @param status ACTIVE
   * @return count
   */
  long countByRosterVersionIdAndStatus(Long rosterVersionId, EbMember.Status status);
}
