package com.iortatechnxt.brokerverse.renewal.setup.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.renewal.domain.PackageMapEntry;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Bulk upload {@code RNW_PACKAGE_MAP} (DMQ36): the PACKAGE code map loaded by the migration team
 * (source MIGRATION). Each row is an entry pending authorization by another member of the Renewal
 * processing team; a row without a BIBS package is a REJECT entry.
 */
@Component
public class PackageMapBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "RNW_PACKAGE_MAP";

  private static final String PACKAGE = "Legacy Package";
  private static final String PACKAGE_VERSION = "Legacy Package Version";
  private static final String RISK = "Risk Code";
  private static final String INSURER = "Insurer";
  private static final String SI_FROM = "Sum Insured From";
  private static final String SI_TO = "Sum Insured To";
  private static final String PRODUCT = "BIBS Package";
  private static final String VERSION = "BIBS Package Version";
  private static final String REMARKS = "Remarks";

  private final PackageMapService map;

  /**
   * Creates the handler.
   *
   * @param map package map
   */
  public PackageMapBulkHandler(PackageMapService map) {
    this.map = map;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Renewal - package map of migrated policies";
  }

  @Override
  public String permission() {
    return Permission.RNW_PACKAGE_REMAP.name();
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(PACKAGE, "Package code in the legacy system", "QPS-HOME-A"),
        BulkColumn.optional(PACKAGE_VERSION, "Legacy version, empty for any", "3"),
        BulkColumn.optional(RISK, "Qualifier: risk code, empty for any", "PAR01"),
        BulkColumn.optional(INSURER, "Qualifier: insurer code, empty for any", "INS-MGIC"),
        new BulkColumn(SI_FROM, "Qualifier: sum insured from", false, BulkColumn.Type.NUMBER, ""),
        new BulkColumn(SI_TO, "Qualifier: sum insured to", false, BulkColumn.Type.NUMBER, ""),
        BulkColumn.optional(PRODUCT, "BIBS package; empty when not renewable", "PKG-HOME"),
        new BulkColumn(VERSION, "BIBS package version", false, BulkColumn.Type.NUMBER, "2"),
        BulkColumn.optional(REMARKS, "Remarks", "Home package, standard"));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return String.join(
        "|",
        String.valueOf(row.text(PACKAGE)),
        String.valueOf(row.text(PACKAGE_VERSION)),
        String.valueOf(row.text(RISK)),
        String.valueOf(row.text(INSURER)),
        String.valueOf(row.number(SI_FROM)));
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return map.problems(data(row));
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    PackageMapEntry entry =
        map.create(context.companyId(), data(row), PackageMapService.SOURCE_MIGRATION);
    return "Package map entry " + entry.getId();
  }

  private static PackageMapEntry.Data data(BulkRow row) {
    BigDecimal version = row.number(VERSION);
    return new PackageMapEntry.Data(
        row.text(PACKAGE),
        row.text(PACKAGE_VERSION),
        row.text(RISK),
        row.text(INSURER),
        row.number(SI_FROM),
        row.number(SI_TO),
        row.text(PRODUCT),
        version == null ? null : version.intValue(),
        row.text(REMARKS));
  }
}
