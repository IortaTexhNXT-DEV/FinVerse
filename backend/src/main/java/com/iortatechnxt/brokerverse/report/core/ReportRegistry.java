package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Collects every {@link ReportDefinition} bean into the report catalogue. */
@Component
public class ReportRegistry {

  private final Map<String, ReportDefinition> byCode = new LinkedHashMap<>();

  /**
   * Creates the registry.
   *
   * @param definitions all report beans
   */
  public ReportRegistry(List<ReportDefinition> definitions) {
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
    return d;
  }

  /**
   * Lists all report metadata.
   *
   * @return catalogue
   */
  public List<ReportMetadata> catalogue() {
    return byCode.values().stream().map(ReportDefinition::metadata).toList();
  }
}
