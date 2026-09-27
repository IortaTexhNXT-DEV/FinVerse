package com.iortatechnxt.brokerverse.migration.legacy.service;

import com.iortatechnxt.brokerverse.migration.legacy.domain.PackageMapRow.PackageMapEntry;
import com.iortatechnxt.brokerverse.migration.legacy.service.port.PackageMapSink;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Default adapters of the legacy seams (section 23: seam only until the owners connect them). */
@Configuration(proxyBeanMethods = false)
public class LegacyPorts {

  /**
   * Package map kept in the migration tables until the Renewal package map is connected.
   *
   * @return sink
   */
  @Bean
  @ConditionalOnMissingBean(PackageMapSink.class)
  public PackageMapSink migrationOnlyPackageMap() {
    return new PackageMapSink() {
      @Override
      public boolean connected() {
        return false;
      }

      @Override
      public void add(Long companyId, PackageMapEntry entry) {
        // kept in mig_package_map; served by MigratedPackageMap
      }

      @Override
      public void remove(Long companyId, PackageMapEntry entry) {
        // the rolled-back row of mig_package_map is enough
      }
    };
  }
}
