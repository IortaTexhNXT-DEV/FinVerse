package com.iortatechnxt.brokerverse.system.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for {@link ModuleProfile}. */
public interface ModuleProfileRepository extends JpaRepository<ModuleProfile, Long> {

  /**
   * Finds a profile by code.
   *
   * @param code profile code
   * @return profile if present
   */
  Optional<ModuleProfile> findByCode(String code);

  /**
   * All profiles by name.
   *
   * @return profiles
   */
  List<ModuleProfile> findAllByOrderByNameAsc();
}
