package com.iortatechnxt.brokerverse.configpromo.api.dto;

import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueGroup;
import java.util.List;

/**
 * The configuration catalogue of this environment.
 *
 * @param groups groups in screen order
 * @param datasets datasets in load order
 * @param excluded reasons of the tables never promoted, with their number of tables
 */
public record CatalogueResponse(
    List<CatalogueGroup> groups, List<DatasetResponse> datasets, List<ExcludedResponse> excluded) {

  /**
   * A dataset.
   *
   * @param code code
   * @param name name
   * @param group group
   * @param module owner module
   * @param key natural key fields
   * @param dependsOn datasets loaded first
   * @param collection whether it is the collection of a parent (replaced with it)
   * @param optional not selected by default
   * @param users user records
   * @param environmentFields fields every environment keeps
   * @param environmentRows whether some items belong to the environment
   * @param items items in this environment
   */
  public record DatasetResponse(
      String code,
      String name,
      String group,
      String module,
      List<String> key,
      List<String> dependsOn,
      boolean collection,
      boolean optional,
      boolean users,
      List<String> environmentFields,
      boolean environmentRows,
      long items) {}

  /**
   * A reason why tables are never promoted.
   *
   * @param reason reason code
   * @param text reason in words
   * @param tables number of tables
   */
  public record ExcludedResponse(String reason, String text, long tables) {}
}
