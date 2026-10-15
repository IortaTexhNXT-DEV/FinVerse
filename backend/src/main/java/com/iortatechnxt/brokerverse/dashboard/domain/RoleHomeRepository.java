package com.iortatechnxt.brokerverse.dashboard.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Home-page widget sets of the roles. */
public interface RoleHomeRepository extends JpaRepository<RoleHome, Long> {

  /**
   * Finds the set of a role.
   *
   * @param roleCode role code
   * @return the set, empty when the role shows the delivered widgets
   */
  Optional<RoleHome> findByRoleCode(String roleCode);

  /**
   * Finds the sets of some roles.
   *
   * @param roleCodes role codes
   * @return sets
   */
  List<RoleHome> findByRoleCodeIn(Iterable<String> roleCodes);
}
