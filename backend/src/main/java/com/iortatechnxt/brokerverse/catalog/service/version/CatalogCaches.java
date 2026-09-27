package com.iortatechnxt.brokerverse.catalog.service.version;

import com.iortatechnxt.brokerverse.cache.service.CacheSpec;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache of the product and package version look-ups ({@link ProductVersionQueryService}), cleared
 * by any product or version change. Served outside read-write transactions only: the catalog has
 * many write paths, so a transaction that changes a product always reads the database.
 */
@Configuration(proxyBeanMethods = false)
public class CatalogCaches {

  /**
   * Version views, keyed {@code current:<product>:<date>}, {@code inForce:<product>:<date>}, {@code
   * version:<product>:<no>} and {@code versions:<product>}.
   */
  public static final String PRODUCT_VERSIONS = "catalog-product-versions";

  /**
   * Declares the cache.
   *
   * @return spec (1 hour; any product or version change clears it)
   */
  @Bean
  public CacheSpec catalogProductVersionsCache() {
    return CacheSpec.of(
            PRODUCT_VERSIONS, Duration.ofHours(1), ProductVersion.class, RiskProduct.class)
        .servedOnlyOutsideWriteTransactions();
  }
}
