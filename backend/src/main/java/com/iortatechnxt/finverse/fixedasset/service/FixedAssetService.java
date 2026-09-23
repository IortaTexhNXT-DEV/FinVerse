package com.iortatechnxt.finverse.fixedasset.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.fixedasset.api.dto.FixedAssetRequest;
import com.iortatechnxt.finverse.fixedasset.domain.AssetCategory;
import com.iortatechnxt.finverse.fixedasset.domain.AssetMovement;
import com.iortatechnxt.finverse.fixedasset.domain.AssetMovementRepository;
import com.iortatechnxt.finverse.fixedasset.domain.AssetStatus;
import com.iortatechnxt.finverse.fixedasset.domain.FixedAsset;
import com.iortatechnxt.finverse.fixedasset.domain.FixedAssetRepository;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationCalculator.Basis;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationCalculator.Position;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.party.service.PartyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fixed asset register: registration and maintenance of assets awaiting capitalization (maker) and
 * register queries. Postings are in {@link AssetLifecycleService}.
 */
@Service
@Transactional
public class FixedAssetService {

  private static final String ASSET = "FixedAsset";

  private final FixedAssetRepository assets;
  private final AssetMovementRepository movements;
  private final AssetCategoryService categories;
  private final AssetAccounting accounting;
  private final OrganizationService organization;
  private final PartyService parties;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param assets asset repository
   * @param movements movement repository
   * @param categories category service
   * @param accounting accounting helper
   * @param organization organization service
   * @param parties party service
   * @param audit audit trail
   */
  public FixedAssetService(
      FixedAssetRepository assets,
      AssetMovementRepository movements,
      AssetCategoryService categories,
      AssetAccounting accounting,
      OrganizationService organization,
      PartyService parties,
      AuditTrailService audit) {
    this.assets = assets;
    this.movements = movements;
    this.categories = categories;
    this.accounting = accounting;
    this.organization = organization;
    this.parties = parties;
    this.audit = audit;
  }

  /**
   * Searches the register.
   *
   * @param companyId company
   * @param status status filter (null = all)
   * @param branchId branch filter (null = all)
   * @param categoryId category filter (null = all)
   * @param term tag number / description fragment (null = all)
   * @return assets
   */
  @Transactional(readOnly = true)
  public List<FixedAsset> search(
      Long companyId, AssetStatus status, Long branchId, Long categoryId, String term) {
    return assets.search(
        companyId,
        status == null ? EnumSet.allOf(AssetStatus.class) : EnumSet.of(status),
        branchId,
        categoryId,
        term == null ? "" : term.trim().toLowerCase(Locale.ROOT));
  }

  /**
   * Gets an asset.
   *
   * @param id id
   * @return asset
   */
  @Transactional(readOnly = true)
  public FixedAsset get(Long id) {
    return assets.findById(id).orElseThrow(() -> new ResourceNotFoundException(ASSET, id));
  }

  /**
   * Movement history of an asset.
   *
   * @param id asset
   * @return movements
   */
  @Transactional(readOnly = true)
  public List<AssetMovement> movements(Long id) {
    return movements.findByAssetIdOrderByMovementDateAscIdAsc(id);
  }

  /**
   * Registers an asset (pending capitalization). Take-on assets get their opening accumulated
   * depreciation computed from the acquisition date to the take-on date unless supplied.
   *
   * @param r request
   * @return asset
   */
  public FixedAsset create(FixedAssetRequest r) {
    if (assets.existsByCompanyIdAndTagNo(r.companyId(), r.tagNo())) {
      throw new DuplicateResourceException(ASSET, r.tagNo());
    }
    AssetCategory category = categories.requireActive(r.categoryId());
    if (!category.getCompanyId().equals(r.companyId())) {
      throw new BusinessRuleException("WRONG_COMPANY", "Asset category belongs to another company");
    }
    organization.requireActiveBranch(r.branchId());
    FixedAsset asset =
        new FixedAsset(category, r.branchId(), r.tagNo(), r.acquisitionDate(), r.acquisitionCost());
    applyPolicy(asset, category, r);
    describe(asset, r);
    if (r.takeOn()) {
      applyTakeOn(asset, r);
    } else {
      LocalDate date =
          r.capitalizationDate() == null ? r.acquisitionDate() : r.capitalizationDate();
      if (date.isBefore(r.acquisitionDate())) {
        throw new BusinessRuleException(
            "CAPITALIZATION_BEFORE_ACQUISITION", "Capitalization date precedes acquisition");
      }
      asset.setCapitalizationDate(date);
    }
    FixedAsset saved = assets.save(asset);
    audit.record(ASSET, saved.getTagNo(), AuditAction.CREATE, "Registered " + r.description());
    return saved;
  }

  /**
   * Updates the descriptive fields of an asset awaiting capitalization.
   *
   * @param id id
   * @param r request
   * @return asset
   */
  public FixedAsset update(Long id, FixedAssetRequest r) {
    FixedAsset asset = get(id);
    asset.requirePending();
    describe(asset, r);
    asset.markModified();
    audit.record(ASSET, asset.getTagNo(), AuditAction.UPDATE, "Updated asset");
    return asset;
  }

  private static void applyPolicy(FixedAsset asset, AssetCategory category, FixedAssetRequest r) {
    if (r.depreciationMethod() != null) {
      asset.setDepreciationMethod(r.depreciationMethod());
    }
    if (r.usefulLifeMonths() != null) {
      asset.setUsefulLifeMonths(r.usefulLifeMonths());
    }
    asset.setResidualValue(
        Money.round(
            r.acquisitionCost()
                .multiply(category.getResidualPercent())
                .divide(BigDecimal.valueOf(100))));
  }

  private void describe(FixedAsset asset, FixedAssetRequest r) {
    if (!r.takeOn() && isBlank(r.settlementAccount())) {
      throw new BusinessRuleException(
          "SETTLEMENT_ACCOUNT_REQUIRED",
          "A settlement account (bank or supplier payable) is required to capitalize");
    }
    if (!isBlank(r.settlementAccount())) {
      accounting.requirePostable(r.companyId(), r.settlementAccount());
    }
    if (!isBlank(r.supplierCode())) {
      parties.requireActive(r.companyId(), r.supplierCode(), EnumSet.allOf(PartyType.class));
    }
    asset.setDescription(r.description());
    asset.setCostCenter(blankToNull(r.costCenter()));
    asset.setSupplierCode(blankToNull(r.supplierCode()));
    asset.setLocation(blankToNull(r.location()));
    asset.setCustodian(blankToNull(r.custodian()));
    asset.setSettlementAccount(blankToNull(r.settlementAccount()));
  }

  private static void applyTakeOn(FixedAsset asset, FixedAssetRequest r) {
    LocalDate takeOnDate = r.capitalizationDate();
    if (takeOnDate == null || !r.acquisitionDate().isBefore(takeOnDate)) {
      throw new BusinessRuleException(
          "INVALID_TAKE_ON_DATE", "A take-on asset needs a take-on date after its acquisition");
    }
    long months =
        ChronoUnit.MONTHS.between(YearMonth.from(r.acquisitionDate()), YearMonth.from(takeOnDate));
    Basis basis = Basis.of(asset);
    Position computed =
        DepreciationCalculator.advance(basis, new Position(Money.zero(), 0), (int) months);
    BigDecimal accumulated =
        r.openingAccumulatedDepreciation() == null
            ? computed.accumulated()
            : r.openingAccumulatedDepreciation();
    int charged = r.openingMonths() == null ? computed.months() : r.openingMonths();
    if (accumulated.compareTo(basis.cost().subtract(basis.residual())) > 0) {
      throw new BusinessRuleException(
          "OPENING_DEPRECIATION_TOO_HIGH",
          "Opening accumulated depreciation exceeds the depreciable amount");
    }
    asset.takeOnWith(takeOnDate, accumulated, charged);
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private static String blankToNull(String s) {
    return isBlank(s) ? null : s.trim();
  }
}
