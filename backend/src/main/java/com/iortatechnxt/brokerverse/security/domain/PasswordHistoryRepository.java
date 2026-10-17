package com.iortatechnxt.brokerverse.security.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link PasswordHistory}. */
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

  /**
   * Previous passwords of a user, newest first.
   *
   * @param username user
   * @return up to 24 history rows
   */
  List<PasswordHistory> findTop24ByUsernameIgnoreCaseOrderByChangedAtDesc(String username);
}
