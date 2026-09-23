package com.iortatechnxt.finverse.fixedasset.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.RecordFacts;
import com.iortatechnxt.finverse.approval.service.MasterRecordApprovals.Scope;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.approval.service.PendingApprovalSource;
import com.iortatechnxt.finverse.fixedasset.domain.AssetCategory;
import com.iortatechnxt.finverse.fixedasset.domain.AssetStatus;
import com.iortatechnxt.finverse.fixedasset.domain.FixedAsset;
import com.iortatechnxt.finverse.fixedasset.domain.FixedAssetRepository;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Approval inbox source of fixed assets: assets registered but not yet capitalized, and asset
 * categories pending authorization. Both are authorized under {@code MASTER_AUTHORIZE} by a user
 * other than the one who last maintained the record. The amount of an asset is its acquisition cost
 * in the company's base currency.
 */
@Component
public class FixedAssetApprovalSource implements PendingApprovalSource {

  /** Module code of fixed asset items. */
  public static final String MODULE = "FIXED_ASSETS";

  private final FixedAssetRepository assets;
  private final MasterRecordApprovals records;
  private final OrganizationService organization;

  /**
   * Creates the source.
   *
   * @param assets asset register
   * @param records master record helper (categories)
   * @param organization organization (base currency)
   */
  public FixedAssetApprovalSource(
      FixedAssetRepository assets,
      MasterRecordApprovals records,
      OrganizationService organization) {
    this.assets = assets;
    this.records = records;
    this.organization = organization;
  }

  @Override
  public List<PendingApproval> pendingFor(ApprovalViewer viewer) {
    if (!viewer.can(MasterRecordApprovals.PERMISSION)) {
      return List.of();
    }
    Map<Long, String> baseCurrencies = new HashMap<>();
    List<PendingApproval> items = new ArrayList<>();
    assets.findByStatusOrderById(AssetStatus.PENDING_CAPITALIZATION).stream()
        .filter(a -> viewer.mayApproveItemOf(maker(a)))
        .map(
            a ->
                new PendingApproval(
                    MODULE,
                    "Asset capitalization",
                    a.getTagNo(),
                    a.getDescription(),
                    a.getAcquisitionCost(),
                    baseCurrencies.computeIfAbsent(
                        a.getCompanyId(), id -> organization.getCompany(id).getBaseCurrency()),
                    maker(a),
                    a.getUpdatedAt() != null ? a.getUpdatedAt() : a.getCreatedAt(),
                    a.getCompanyId(),
                    "/assets/register"))
        .forEach(items::add);
    items.addAll(
        records.pending(
            viewer,
            new Scope(MODULE, MasterRecordApprovals.PERMISSION),
            AssetCategory.class,
            c ->
                new RecordFacts(
                    "Asset category",
                    c.getCode(),
                    c.getName(),
                    c.getCompanyId(),
                    "/assets/categories")));
    return items;
  }

  /** The capitalizing checker must differ from the last maintainer (see {@code authorize}). */
  private static String maker(FixedAsset a) {
    return a.getUpdatedBy() != null ? a.getUpdatedBy() : a.getCreatedBy();
  }
}
