package com.iortatechnxt.brokerverse.configpromo.domain;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Configuration imports. */
public interface PromotionImportRepository extends JpaRepository<PromotionImport, Long> {

  /**
   * Every import, newest first.
   *
   * @param pageable page
   * @return imports
   */
  Page<PromotionImport> findAllByOrderByIdDesc(Pageable pageable);

  /**
   * Imports in a status, oldest first.
   *
   * @param status status
   * @return imports
   */
  List<PromotionImport> findByStatusOrderByIdAsc(ImportStatus status);

  /**
   * Imports of a package.
   *
   * @param packageId package
   * @return imports
   */
  List<PromotionImport> findByPackageIdOrderByIdDesc(Long packageId);
}
