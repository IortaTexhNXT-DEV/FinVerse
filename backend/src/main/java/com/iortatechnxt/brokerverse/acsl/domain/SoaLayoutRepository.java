package com.iortatechnxt.brokerverse.acsl.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Insurer SOA layouts (ACSL 2.4.0, AQ21). */
public interface SoaLayoutRepository extends JpaRepository<SoaLayout, Long> {

  /**
   * The active layout of an insurer (or of the standard template {@code *}).
   *
   * @param insurerCode insurer
   * @return layout
   */
  Optional<SoaLayout> findByInsurerCodeAndActiveTrue(String insurerCode);

  /**
   * Every layout, standard first.
   *
   * @return layouts
   */
  List<SoaLayout> findAllByOrderByInsurerCodeAsc();
}
