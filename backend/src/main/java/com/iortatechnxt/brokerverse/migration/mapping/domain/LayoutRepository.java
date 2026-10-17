package com.iortatechnxt.brokerverse.migration.mapping.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Layout versions. */
public interface LayoutRepository extends JpaRepository<Layout, Long> {

  /**
   * A layout version in a status (FROZEN is the version in force).
   *
   * @param code layout code
   * @param status status
   * @return versions, newest first
   */
  List<Layout> findByCodeAndStatusOrderByVersionNoDesc(String code, Layout.Status status);

  /**
   * The layout versions of an object.
   *
   * @param objectCode object
   * @return versions
   */
  List<Layout> findByObjectCodeOrderByCodeAscVersionNoDesc(String objectCode);

  /**
   * Every layout version.
   *
   * @return versions
   */
  List<Layout> findAllByOrderByObjectCodeAscCodeAscVersionNoDesc();

  /**
   * A version of a layout.
   *
   * @param code layout
   * @param versionNo version
   * @return layout
   */
  Optional<Layout> findByCodeAndVersionNo(String code, int versionNo);
}
