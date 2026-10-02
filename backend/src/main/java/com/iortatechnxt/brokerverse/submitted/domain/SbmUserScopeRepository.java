package com.iortatechnxt.brokerverse.submitted.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** User scopes. */
public interface SbmUserScopeRepository extends JpaRepository<SbmUserScope, Long> {

  /**
   * The scope of a user.
   *
   * @param companyId company
   * @param username user
   * @return scope
   */
  Optional<SbmUserScope> findByCompanyIdAndUsername(Long companyId, String username);

  /**
   * Scopes of a company.
   *
   * @param companyId company
   * @return scopes
   */
  List<SbmUserScope> findByCompanyIdOrderByUsernameAsc(Long companyId);
}
