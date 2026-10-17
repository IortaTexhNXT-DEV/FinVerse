package com.iortatechnxt.brokerverse.security.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link UserDataScopeGrant}. */
public interface UserDataScopeGrantRepository extends JpaRepository<UserDataScopeGrant, Long> {

  /**
   * The grants of a user.
   *
   * @param userId user id
   * @return grants
   */
  List<UserDataScopeGrant> findByUserIdOrderByCompanyIdAscBranchIdAsc(Long userId);
}
