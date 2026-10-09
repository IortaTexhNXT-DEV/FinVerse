package com.iortatechnxt.brokerverse.identity.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Directory details of the users. */
public interface DirectoryProfileRepository extends JpaRepository<DirectoryProfile, Long> {

  /**
   * The profile of a user.
   *
   * @param username user
   * @return profile
   */
  Optional<DirectoryProfile> findByUsernameIgnoreCase(String username);
}
