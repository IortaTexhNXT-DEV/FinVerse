package com.iortatechnxt.brokerverse.fixedasset.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.fixedasset.api.dto.AssetCategoryRequest;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetCategory;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetCategoryRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fixed asset category maintenance (maker-checker). */
@Service
@Transactional
public class AssetCategoryService {

  private static final String CATEGORY = "AssetCategory";

  private final AssetCategoryRepository categories;
  private final AssetAccounting accounting;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param categories repository
   * @param accounting GL account checks
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public AssetCategoryService(
      AssetCategoryRepository categories,
      AssetAccounting accounting,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.categories = categories;
    this.accounting = accounting;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Lists categories.
   *
   * @param companyId company
   * @return categories by code
   */
  @Transactional(readOnly = true)
  public List<AssetCategory> list(Long companyId) {
    return categories.findByCompanyIdOrderByCode(companyId);
  }

  /**
   * Gets a category.
   *
   * @param id id
   * @return category
   */
  @Transactional(readOnly = true)
  public AssetCategory get(Long id) {
    return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException(CATEGORY, id));
  }

  /**
   * Returns an authorized category or fails.
   *
   * @param id id
   * @return category
   */
  @Transactional(readOnly = true)
  public AssetCategory requireActive(Long id) {
    AssetCategory category = get(id);
    if (!category.isActive()) {
      throw new BusinessRuleException(
          "INACTIVE_ASSET_CATEGORY", "Asset category " + category.getCode() + " is not active");
    }
    return category;
  }

  /**
   * Creates a category (pending authorization).
   *
   * @param r request
   * @return category
   */
  public AssetCategory create(AssetCategoryRequest r) {
    if (categories.existsByCompanyIdAndCode(r.companyId(), r.code())) {
      throw new DuplicateResourceException(CATEGORY, r.code());
    }
    AssetCategory category = new AssetCategory(r.companyId(), r.code(), r.name());
    apply(category, r);
    AssetCategory saved = categories.save(category);
    audit.record(
        CATEGORY, saved.getCode(), AuditAction.CREATE, "Created asset category " + r.name());
    return saved;
  }

  /**
   * Updates a category; it returns to pending authorization. Existing assets keep the method and
   * useful life they were registered with.
   *
   * @param id id
   * @param r request
   * @return category
   */
  public AssetCategory update(Long id, AssetCategoryRequest r) {
    AssetCategory category = get(id);
    category.setName(r.name());
    apply(category, r);
    category.markModified();
    audit.record(CATEGORY, category.getCode(), AuditAction.UPDATE, "Updated asset category");
    return category;
  }

  /**
   * Authorizes a category (checker).
   *
   * @param id id
   * @return category
   */
  public AssetCategory authorize(Long id) {
    AssetCategory category = get(id);
    category.authorize(currentUser.username(), clock.instant());
    audit.record(CATEGORY, category.getCode(), AuditAction.AUTHORIZE, "Authorized asset category");
    return category;
  }

  private void apply(AssetCategory c, AssetCategoryRequest r) {
    accounting.requirePostable(r.companyId(), r.assetAccount());
    accounting.requirePostable(r.companyId(), r.accumulatedDepreciationAccount());
    accounting.requirePostable(r.companyId(), r.depreciationExpenseAccount());
    c.setAssetAccount(r.assetAccount());
    c.setAccumulatedDepreciationAccount(r.accumulatedDepreciationAccount());
    c.setDepreciationExpenseAccount(r.depreciationExpenseAccount());
    c.setDepreciationMethod(r.depreciationMethod());
    c.setUsefulLifeMonths(r.usefulLifeMonths());
    c.setResidualPercent(r.residualPercent());
  }
}
