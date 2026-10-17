package com.iortatechnxt.brokerverse.security.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link PasswordResetToken}. */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

  /**
   * The link of a token hash.
   *
   * @param tokenHash SHA-256 (hex) of the token
   * @return link
   */
  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  /**
   * The unused links of a user (withdrawn when a newer one is issued).
   *
   * @param username user
   * @return links
   */
  List<PasswordResetToken> findByUsernameIgnoreCaseAndUsedAtIsNull(String username);
}
