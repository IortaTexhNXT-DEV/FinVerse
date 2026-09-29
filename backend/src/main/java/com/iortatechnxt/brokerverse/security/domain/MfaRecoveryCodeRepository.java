package com.iortatechnxt.brokerverse.security.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Recovery codes of the second factor. */
public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, Long> {

  /**
   * An unused code of a user.
   *
   * @param username user
   * @param codeHash SHA-256 of the code
   * @return code
   */
  Optional<MfaRecoveryCode> findFirstByUsernameIgnoreCaseAndCodeHashAndUsedAtIsNull(
      String username, String codeHash);

  /**
   * Unused codes of a user.
   *
   * @param username user
   * @return count
   */
  long countByUsernameIgnoreCaseAndUsedAtIsNull(String username);

  /**
   * Removes every code of a user.
   *
   * @param username user
   * @return rows removed
   */
  @Modifying
  @Query("delete from MfaRecoveryCode c where lower(c.username) = lower(?1)")
  int deleteAllOf(String username);
}
