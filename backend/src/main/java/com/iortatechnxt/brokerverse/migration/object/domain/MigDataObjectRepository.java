package com.iortatechnxt.brokerverse.migration.object.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data objects of the migration register. */
public interface MigDataObjectRepository extends JpaRepository<MigDataObject, Long> {

  /**
   * An object by code.
   *
   * @param code object code
   * @return object
   */
  Optional<MigDataObject> findByCode(String code);

  /**
   * The register in load order.
   *
   * @return objects
   */
  List<MigDataObject> findAllByOrderByLoadOrderAscCodeAsc();
}
