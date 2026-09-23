package com.iortatechnxt.finverse.security.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link Role}. */
public interface RoleRepository extends JpaRepository<Role, Long> {

  /**
   * Finds a role by code.
   *
   * @param code role code
   * @return role if present
   */
  Optional<Role> findByCode(String code);

  /**
   * Finds roles by codes.
   *
   * @param codes role codes
   * @return matching roles
   */
  List<Role> findByCodeIn(Iterable<String> codes);
}
