package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.system.service.ProductModules;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Collects every {@link ReportDefinition} bean into the report catalogue. The reports of a
 * switched-off product module are not listed and cannot be run ({@link ProductModules}).
 */
@Component
public class ReportRegistry {

  private final Map<String, ReportDefinition> byCode = new LinkedHashMap<>();
  private final ProductModules modules;

  /**
   * Creates the registry.
   *
   * @param definitions all report beans
   * @param modules product module switches
   */
  public ReportRegistry(List<ReportDefinition> definitions, ProductModules modules) {
    this.modules = modules;
    definitions.stream()
        .sorted(Comparator.comparing(d -> d.metadata().code()))
        .forEach(
            d -> {
              if (byCode.putIfAbsent(d.metadata().code(), d) != null) {
                throw new IllegalStateException("Duplicate report code " + d.metadata().code());
              }
            });
  }

  /**
   * Finds a report.
   *
   * @param code report code
   * @return definition
   */
  public ReportDefinition get(String code) {
    ReportDefinition d = byCode.get(code);
    if (d == null) {
      throw new ResourceNotFoundException("Report", code);
    }
    modules.requireClassOn(ClassUtils.getUserClass(d));
    return d;
  }

  /**
   * Lists the metadata of the reports of the modules in use.
   *
   * @return catalogue
   */
  public List<ReportMetadata> catalogue() {
    return byCode.values().stream()
        .filter(d -> modules.isClassOn(ClassUtils.getUserClass(d)))
        .map(ReportDefinition::metadata)
        .toList();
  }
}
