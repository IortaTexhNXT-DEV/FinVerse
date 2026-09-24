package com.iortatechnxt.brokerverse.opsledger.api.dto;

import com.iortatechnxt.brokerverse.opsledger.service.OperationsHomeService.ExternalLink;
import com.iortatechnxt.brokerverse.opsledger.service.OperationsHomeService.SectionCounts;
import java.util.List;

/**
 * The Operations home (BRQID.003): the user's team sections with their counts and the links to the
 * integrated applications.
 *
 * @param sections sections in display order
 * @param links external application links
 */
public record OperationsHomeResponse(List<SectionCounts> sections, List<ExternalLink> links) {

  /** Defensive copies. */
  public OperationsHomeResponse {
    sections = List.copyOf(sections);
    links = List.copyOf(links);
  }
}
