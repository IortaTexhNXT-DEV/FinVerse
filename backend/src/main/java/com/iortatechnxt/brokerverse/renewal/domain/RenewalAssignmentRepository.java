package com.iortatechnxt.brokerverse.renewal.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Assignments. */
public interface RenewalAssignmentRepository extends JpaRepository<RenewalAssignment, Long> {

  /**
   * Assignments of a candidate, newest first.
   *
   * @param candidateId candidate
   * @return assignments
   */
  List<RenewalAssignment> findByCandidateIdOrderByIdDesc(Long candidateId);

  /**
   * Whether a candidate was ever assigned to a user in a role.
   *
   * @param candidateId candidate
   * @param username user
   * @param role role
   * @return true when assigned at least once
   */
  boolean existsByCandidateIdAndUsernameAndRole(
      Long candidateId, String username, RenewalAssignment.Role role);
}
