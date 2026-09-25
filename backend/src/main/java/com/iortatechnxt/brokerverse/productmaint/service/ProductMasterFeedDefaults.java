package com.iortatechnxt.brokerverse.productmaint.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default {@link ProductMasterFeed} (BRPM.022 seam, PQ16): logs each product master change and
 * sends nothing. An integration module replaces it by declaring its own {@code ProductMasterFeed}
 * bean.
 */
@Configuration(proxyBeanMethods = false)
public class ProductMasterFeedDefaults {

  private static final Logger LOG = LoggerFactory.getLogger(ProductMasterFeedDefaults.class);

  /**
   * The logging feed, registered only when no integration adapter exists.
   *
   * @return feed
   */
  @Bean
  @ConditionalOnMissingBean(ProductMasterFeed.class)
  public ProductMasterFeed loggingProductMasterFeed() {
    return change ->
        LOG.info(
            "Product master change {} {} v{} from {} (no synchronisation target configured, PQ16)",
            change.kind(),
            change.productCode(),
            change.versionNo(),
            change.effectiveDate());
  }
}
