package com.iortatechnxt.brokerverse.security.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** The authenticator apps of the users. */
public interface UserMfaRepository extends JpaRepository<UserMfa, Long> {

  /**
   * The enrolment of a user in a status.
   *
   * @param username user
   * @param status PENDING or ACTIVE
   * @return enrolment
   */
  Optional<UserMfa> findByUsernameIgnoreCaseAndStatus(String username, String status);

  /**
   * Every enrolment of a user.
   *
   * @param username user
   * @return enrolments
   */
  List<UserMfa> findByUsernameIgnoreCase(String username);

  /**
   * Every enrolment in a status.
   *
   * @param status PENDING or ACTIVE
   * @return enrolments
   */
  List<UserMfa> findByStatus(String status);

  /**
   * Removes every enrolment of a user.
   *
   * @param username user
   * @return rows removed
   */
  @Modifying
  @Query("delete from UserMfa m where lower(m.username) = lower(?1)")
  int deleteAllOf(String username);
}
