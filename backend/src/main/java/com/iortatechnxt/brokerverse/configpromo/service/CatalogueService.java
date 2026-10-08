package com.iortatechnxt.brokerverse.configpromo.service;

import com.iortatechnxt.brokerverse.configpromo.catalogue.CatalogueDataset;
import com.iortatechnxt.brokerverse.configpromo.catalogue.ConfigCatalogue;
import com.iortatechnxt.brokerverse.configpromo.engine.CatalogueModel;
import com.iortatechnxt.brokerverse.configpromo.engine.DatasetReader;
import com.iortatechnxt.brokerverse.configpromo.engine.SchemaReader;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The configuration catalogue of this environment: the datasets resolved against its database (the
 * schema is read once, as it changes only with a deployment) and their number of items.
 */
@Service
public class CatalogueService {

  private final JdbcTemplate jdbc;
  private final ConfigCatalogue catalogue = ConfigCatalogue.load();
  private volatile CatalogueModel model;

  /**
   * Creates the service.
   *
   * @param jdbc database
   */
  public CatalogueService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * The catalogue.
   *
   * @return catalogue
   */
  public ConfigCatalogue catalogue() {
    return catalogue;
  }

  /**
   * The catalogue resolved against the database of this environment.
   *
   * @return model
   */
  public CatalogueModel model() {
    CatalogueModel current = model;
    if (current == null) {
      synchronized (this) {
        current = model;
        if (current == null) {
          current = new CatalogueModel(catalogue, SchemaReader.read(jdbc));
          model = current;
        }
      }
    }
    return current;
  }

  /**
   * A reader of the datasets of this environment (in the caller's transaction).
   *
   * @return reader
   */
  public DatasetReader reader() {
    return new DatasetReader(jdbc, model());
  }

  /**
   * One dataset of the catalogue with its number of items in this environment.
   *
   * @param dataset dataset
   * @param items number of items
   */
  public record DatasetCount(CatalogueDataset dataset, long items) {}

  /**
   * The datasets with their number of items, in load order.
   *
   * @return datasets
   */
  @Transactional(readOnly = true)
  public List<DatasetCount> counts() {
    CatalogueModel m = model();
    DatasetReader r = reader();
    return m.loadOrder().stream()
        .map(code -> new DatasetCount(m.model(code).dataset(), r.count(code)))
        .toList();
  }
}
