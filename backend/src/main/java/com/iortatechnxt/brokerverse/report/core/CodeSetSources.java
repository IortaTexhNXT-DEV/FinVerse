package com.iortatechnxt.brokerverse.report.core;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.report.core.CodeSetSource.CodeOption;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every {@link CodeSetSource} bean, by key. */
@Service
@Transactional(readOnly = true)
public class CodeSetSources {

  private final Map<String, CodeSetSource> sources;

  /**
   * Creates the registry.
   *
   * @param sources the sources of the business modules
   * @param groups groups of sources of the business modules
   */
  public CodeSetSources(
      ObjectProvider<CodeSetSource> sources, ObjectProvider<CodeSetSourceGroup> groups) {
    this.sources =
        Stream.concat(
                sources.orderedStream(), groups.orderedStream().flatMap(g -> g.sources().stream()))
            .collect(Collectors.toMap(CodeSetSource::source, Function.identity()));
  }

  /**
   * The options of a source.
   *
   * @param source key
   * @param companyId company
   * @return options
   */
  public List<CodeOption> options(String source, Long companyId) {
    CodeSetSource found = sources.get(source);
    if (found == null) {
      throw new ResourceNotFoundException("Code list", source);
    }
    return found.options(companyId);
  }
}
