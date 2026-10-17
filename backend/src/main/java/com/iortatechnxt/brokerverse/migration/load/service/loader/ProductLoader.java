package com.iortatechnxt.brokerverse.migration.load.service.loader;

import com.iortatechnxt.brokerverse.catalog.domain.PaymentGate;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.TsuInvolvement;
import com.iortatechnxt.brokerverse.catalog.service.MigratedProductService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.migration.common.service.Values;
import com.iortatechnxt.brokerverse.migration.load.domain.KeyXref;
import com.iortatechnxt.brokerverse.migration.load.service.LoadContext;
import com.iortatechnxt.brokerverse.migration.load.service.LoadOutcome;
import com.iortatechnxt.brokerverse.migration.load.service.LoadUnit;
import com.iortatechnxt.brokerverse.migration.load.service.MigrationLoader;
import com.iortatechnxt.brokerverse.migration.mapping.domain.EntryAction;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMapLoader;
import com.iortatechnxt.brokerverse.migration.mapping.service.CodeMaps;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Loader of the legacy products and risk codes (object R05; DATA_MIGRATION_DESIGN 7 and 10). The
 * approved PRODUCT code map decides each legacy risk code: MAP - the BIBS product exists and the
 * legacy code is recorded against it; CREATE - the product is added to the catalogue through {@link
 * MigratedProductService} with origin MIGRATED, pending authorization and not sellable until the
 * Product Owner completes and authorizes it (conservative defaults: one-year term, paid before
 * issuance, underwriting by the rules). A rolled-back batch removes (or retires) the created
 * products.
 */
@Component
public class ProductLoader implements MigrationLoader {

  /** Entity of a product in the cross-reference. */
  public static final String ENTITY = "Product";

  private static final String PRODUCT_MAP = "PRODUCT";

  private final ProductCatalogService catalog;
  private final MigratedProductService migrated;
  private final CodeMapLoader maps;

  /**
   * Creates the loader.
   *
   * @param catalog product catalogue
   * @param migrated products created by the migration
   * @param maps approved code maps (PRODUCT)
   */
  public ProductLoader(
      ProductCatalogService catalog, MigratedProductService migrated, CodeMapLoader maps) {
    this.catalog = catalog;
    this.migrated = migrated;
    this.maps = maps;
  }

  @Override
  public String objectCode() {
    return "R05";
  }

  @Override
  public LoadOutcome load(LoadUnit unit, LoadContext ctx) {
    String legacyCode = unit.legacyKey();
    CodeMaps.Resolution resolution =
        maps.approved(Set.of(PRODUCT_MAP))
            .resolve(PRODUCT_MAP, unit.sourceSystem(), legacyCode)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "MIG_PRODUCT_NOT_MAPPED",
                        "Risk code " + legacyCode + " is not in the approved PRODUCT map"));
    if (resolution.action() != EntryAction.CREATE) {
      RiskProduct existing = catalog.requireProduct(resolution.target());
      return LoadOutcome.of(ENTITY, existing.getId(), existing.getCode(), existing.getVersion());
    }
    RiskProduct created =
        migrated.create(
            resolution.target(),
            new ProductDetails(
                Values.text(unit.value("risk_name")),
                Values.text(unit.value("line_code")),
                Values.text(unit.value("cover_type_code")),
                Values.flag(unit.value("packaged_flag")),
                false,
                Values.items(unit.value("market_segments")),
                false,
                false,
                false,
                1,
                false,
                PaymentGate.PAID,
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                TsuInvolvement.BY_RULES),
            RecordOrigin.migrated(unit.sourceSystem(), legacyCode, ctx.batchNo()));
    return LoadOutcome.of(ENTITY, created.getId(), created.getCode(), created.getVersion());
  }

  @Override
  public List<String> reconciledColumns() {
    return List.of();
  }

  @Override
  public boolean reversible() {
    return true;
  }

  @Override
  public boolean compensate(KeyXref entry, LoadContext ctx) {
    RiskProduct product = catalog.requireProduct(entry.getTargetCode());
    if (product.getRecordOrigin().isMigrated()
        && ctx.batchNo().equals(product.getRecordOrigin().migrationBatch())) {
      migrated.rollback(entry.getTargetCode());
    }
    return true;
  }
}
