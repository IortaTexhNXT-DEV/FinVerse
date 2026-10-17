package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.legacy.domain.PackageMapRow;
import com.iortatechnxt.brokerverse.migration.legacy.domain.PackageMapRow.PackageMapEntry;
import com.iortatechnxt.brokerverse.migration.legacy.domain.PackageMapRowRepository;
import com.iortatechnxt.brokerverse.migration.legacy.service.port.PackageMapSink;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Loader of the package map (object R06; DATA_MIGRATION_DESIGN section 15.2): each legacy package
 * and version, split by risk code, insurer or sum-insured band where one legacy package becomes
 * several BIBS packages, is mapped to a BIBS package version maintained by TSU or rejected. The
 * entries stay in {@code mig_package_map} and go to the Renewal package map through {@link
 * PackageMapSink}; the legacy package of the migrated headers is kept as given and remapped at
 * Renewal sanitation.
 */
@Component
public class PackageMapLoader implements MigrationLoader {

  /** Entity type of an entry in the cross-reference. */
  public static final String ENTITY = "MigPackageMap";

  private static final Pattern VERSIONED = Pattern.compile("^(.+?)\\s+v(\\d+)$");
  private static final Pattern BAND = Pattern.compile("^\\s*([\\d.]*)\\s*-\\s*([\\d.]*)\\s*$");
  private static final String REJECT = "REJECT";

  private final PackageMapRowRepository rows;
  private final PackageMapSink sink;

  /**
   * Creates the loader.
   *
   * @param rows package map entries
   * @param sink Renewal package map seam
   */
  public PackageMapLoader(PackageMapRowRepository rows, PackageMapSink sink) {
    this.rows = rows;
    this.sink = sink;
  }

  @Override
  public String objectCode() {
    return "R06";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    PackageMapEntry entry = entry(unit.values());
    PackageMapRow saved = rows.save(new PackageMapRow(ctx.companyId(), entry, ctx.batch().getId()));
    sink.add(ctx.companyId(), entry);
    return LoadOutcome.of(ENTITY, saved.getId(), code(entry), saved.getVersion());
  }

  @Override
  public Optional<LoadOutcome> update(LoadUnit unit, KeyXref entry, LoadContext ctx) {
    return rows.findById(entry.getTargetId())
        .filter(r -> !r.isRolledBack())
        .map(
            r -> {
              sink.remove(ctx.companyId(), r.entry());
              PackageMapEntry e = entry(unit.values());
              r.apply(e);
              sink.add(ctx.companyId(), e);
              return LoadOutcome.of(ENTITY, r.getId(), code(e), r.getVersion());
            });
  }

  private static String code(PackageMapEntry e) {
    return e.legacyPackageCode() + " v" + e.legacyPackageVersion();
  }

  static PackageMapEntry entry(Map<String, String> v) {
    String action = Values.code(v.get("action"));
    String qualifier = Values.code(v.get("qualifier"));
    String value = Values.text(v.get("qualifier_value"));
    BigDecimal[] band = "SI_BAND".equals(qualifier) ? band(value) : new BigDecimal[2];
    String target = Values.text(v.get("bibs_package_version"));
    String product = null;
    Integer version = null;
    if (!REJECT.equals(action) && target != null) {
      Matcher m = VERSIONED.matcher(target);
      product = m.matches() ? m.group(1).strip() : target;
      version = m.matches() ? Integer.valueOf(m.group(2)) : null;
    }
    return new PackageMapEntry(
        Values.code(v.get("legacy_package_code")),
        Values.decimal(v.get("legacy_package_version")).map(BigDecimal::intValue).orElse(0),
        Values.text(v.get("legacy_package_name")),
        "RISK_CODE".equals(qualifier) ? upper(value) : null,
        "INSURER".equals(qualifier) ? upper(value) : null,
        band[0],
        band[1],
        REJECT.equals(action) ? REJECT : "MAP",
        product,
        version,
        Values.text(v.get("remarks")));
  }

  private static BigDecimal[] band(String value) {
    Matcher m = value == null ? null : BAND.matcher(value);
    if (m == null || !m.matches()) {
      throw new BusinessRuleException(
          "MIG_SI_BAND", "The sum-insured band " + value + " is not from-to amounts");
    }
    return new BigDecimal[] {amount(m.group(1)), amount(m.group(2))};
  }

  private static BigDecimal amount(String s) {
    return s == null || s.isBlank() ? null : new BigDecimal(s);
  }

  private static String upper(String s) {
    return s == null ? null : s.toUpperCase(Locale.ROOT);
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    rows.findById(entry.getTargetId())
        .filter(r -> !r.isRolledBack())
        .ifPresent(
            r -> {
              r.rollBack();
              sink.remove(ctx.companyId(), r.entry());
            });
    return true;
  }
}
