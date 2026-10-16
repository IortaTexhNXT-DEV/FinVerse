package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Contacts of the insurers. */
public interface InsurerContactRepository extends JpaRepository<InsurerContact, Long> {

  /**
   * Contacts of an insurer.
   *
   * @param insurerId insurer profile
   * @return contacts by role and name
   */
  List<InsurerContact> findByInsurerIdOrderByRoleAscNameAsc(Long insurerId);
}
