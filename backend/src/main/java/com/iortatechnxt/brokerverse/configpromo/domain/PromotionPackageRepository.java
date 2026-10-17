package com.iortatechnxt.brokerverse.configpromo.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Configuration packages. */
public interface PromotionPackageRepository extends JpaRepository<PromotionPackage, Long> {

  /**
   * Packages of a kind, newest first.
   *
   * @param kind kind
   * @param pageable page
   * @return packages
   */
  Page<PromotionPackage> findByKindOrderByIdDesc(PackageKind kind, Pageable pageable);

  /**
   * Every package, newest first.
   *
   * @param pageable page
   * @return packages
   */
  Page<PromotionPackage> findAllByOrderByIdDesc(Pageable pageable);
}
