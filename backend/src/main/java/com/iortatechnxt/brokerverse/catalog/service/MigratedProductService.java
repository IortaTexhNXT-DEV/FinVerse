package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLifecycle;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionRepository;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Products created by the Data Migration (object R05, a legacy risk code the PRODUCT map creates;
 * DATA_MIGRATION_DESIGN 10): added pending authorization with origin MIGRATED, and undone with
 * their migration batch.
 */
@Service
@Transactional
public class MigratedProductService {

  private final ProductCatalogService catalog;
  private final RiskProductRepository products;
  private final ProductVersionRepository versions;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param catalog product catalogue
   * @param products products
   * @param versions product versions
   * @param audit audit trail
   */
  public MigratedProductService(
      ProductCatalogService catalog,
      RiskProductRepository products,
      ProductVersionRepository versions,
      AuditTrailService audit) {
    this.catalog = catalog;
    this.products = products;
    this.versions = versions;
    this.audit = audit;
  }

  /**
   * Adds a product created by the migration, pending authorization, with origin MIGRATED.
   *
   * @param code risk code
   * @param details attributes
   * @param origin source system, legacy risk code and batch
   * @return product
   */
  public RiskProduct create(String code, ProductDetails details, RecordOrigin origin) {
    RiskProduct product = catalog.createProduct(code, details);
    product.markMigrated(origin);
    return product;
  }

  /**
   * Undoes a product of a rolled-back migration batch: removed while it has no version, otherwise
   * retired.
   *
   * @param code risk code
   */
  public void rollback(String code) {
    RiskProduct product = catalog.requireProduct(code);
    if (!product.getRecordOrigin().isMigrated()) {
      throw new BusinessRuleException(
          "PRODUCT_NOT_MIGRATED", "Product " + code + " was not created by the migration");
    }
    if (versions.existsByProductCode(code)) {
      product.changeLifecycle(ProductLifecycle.RETIRED);
      audit.record(
          CatalogKind.PRODUCT.label(), code, AuditAction.UPDATE, "Retired: migration rolled back");
    } else {
      products.delete(product);
      audit.record(
          CatalogKind.PRODUCT.label(),
          code,
          AuditAction.DEACTIVATE,
          "Removed: migration batch rolled back");
    }
  }
}
