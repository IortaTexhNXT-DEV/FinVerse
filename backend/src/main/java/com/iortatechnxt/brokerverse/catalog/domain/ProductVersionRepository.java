package com.iortatechnxt.brokerverse.catalog.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Package product versions (BRPM.006/007). */
public interface ProductVersionRepository extends JpaRepository<ProductVersion, Long> {

  /**
   * One version of a product.
   *
   * @param productCode risk code
   * @param versionNo version number
   * @return version
   */
  Optional<ProductVersion> findByProductCodeAndVersionNo(String productCode, int versionNo);

  /**
   * Every version of a product, newest first.
   *
   * @param productCode risk code
   * @return versions
   */
  List<ProductVersion> findByProductCodeOrderByVersionNoDesc(String productCode);

  /**
   * Whether a product has any version.
   *
   * @param productCode risk code
   * @return true when versioned
   */
  boolean existsByProductCode(String productCode);

  /**
   * Versions in some statuses, oldest submission first.
   *
   * @param statuses statuses
   * @return versions
   */
  List<ProductVersion> findByStatusInOrderBySubmittedAtAscIdAsc(
      Collection<ProductVersionStatus> statuses);
}
