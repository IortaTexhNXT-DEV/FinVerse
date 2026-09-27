package com.iortatechnxt.brokerverse.migration.load.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** The loaders of the data objects, by object code. */
@Component
public class LoaderRegistry {

  private final Map<String, MigrationLoader> loaders;

  /**
   * Creates the registry.
   *
   * @param loaders every loader bean
   */
  public LoaderRegistry(List<MigrationLoader> loaders) {
    this.loaders =
        loaders.stream()
            .collect(Collectors.toMap(MigrationLoader::objectCode, Function.identity()));
  }

  /**
   * The loader of an object.
   *
   * @param objectCode object
   * @return loader
   */
  public Optional<MigrationLoader> find(String objectCode) {
    return Optional.ofNullable(loaders.get(objectCode));
  }

  /**
   * The loader of an object, refused when none is built.
   *
   * @param objectCode object
   * @return loader
   */
  public MigrationLoader require(String objectCode) {
    return find(objectCode)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "MIG_NO_LOADER", "Object " + objectCode + " has no loader in this release"));
  }

  /**
   * Objects with a loader.
   *
   * @return object codes
   */
  public Set<String> objects() {
    return loaders.keySet();
  }
}
