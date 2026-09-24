package com.iortatechnxt.brokerverse.fixedasset.demo;

import com.iortatechnxt.brokerverse.fixedasset.api.dto.DisposalRequest;
import com.iortatechnxt.brokerverse.fixedasset.api.dto.FixedAssetRequest;
import com.iortatechnxt.brokerverse.fixedasset.api.dto.TransferRequest;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetCategoryRepository;
import com.iortatechnxt.brokerverse.fixedasset.domain.AssetStatus;
import com.iortatechnxt.brokerverse.fixedasset.domain.FixedAsset;
import com.iortatechnxt.brokerverse.fixedasset.domain.FixedAssetRepository;
import com.iortatechnxt.brokerverse.fixedasset.service.AssetLifecycleService;
import com.iortatechnxt.brokerverse.fixedasset.service.DepreciationService;
import com.iortatechnxt.brokerverse.fixedasset.service.FixedAssetService;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Demo profile only: builds a fixed asset register for the demo company through the services, so
 * every posting goes through the accounting engine. Assets acquired before 2026 are taken on at
 * 2026-01-01 (opening balance journal), 2026 acquisitions are capitalized on their acquisition
 * dates, depreciation is run for January to September 2026, one asset is transferred to Cebu and
 * one vehicle is sold. Idempotent: each step is skipped when already done.
 */
@Component
@Profile("demo")
@Order(70)
public class FixedAssetDemoData implements ApplicationRunner {

  private static final String COMPANY = "FVI";
  private static final LocalDate TAKE_ON = LocalDate.of(2026, 1, 1);
  private static final YearMonth FIRST = YearMonth.of(2026, 1);
  private static final YearMonth LAST = YearMonth.of(2026, 9);
  private static final LocalDate TRANSFER_DATE = LocalDate.of(2026, 6, 15);
  private static final LocalDate DISPOSAL_DATE = LocalDate.of(2026, 8, 20);
  private static final String MAKER = "accountant";
  private static final String CHECKER = "checker";
  private static final String MANAGER = "fmanager";
  private static final String TRANSFERRED_TAG = "FA-FRN-0002";
  private static final String DISPOSED_TAG = "FA-VEH-0002";
  private static final String BANK = "1111";

  /**
   * Tag | description | category | branch | cost centre | supplier (- = none) | acquired | cost.
   */
  private static final String ASSET_TABLE =
      """
      FA-OFF-0001|Photocopier Ricoh MP 3055|OFFEQ|HO|FIN|S-0001|2024-02-15|185000
      FA-OFF-0002|Air-conditioning units (6)|OFFEQ|HO|EXEC|S-0001|2024-05-10|420000
      FA-OFF-0003|Fireproof vault|OFFEQ|CEB|FIN|S-0001|2024-08-01|96000
      FA-OFF-0004|Telephone PABX system|OFFEQ|DVO|FIN|S-0001|2025-01-20|128000
      FA-IT-0001|Core servers (2 x rack server)|ITEQ|HO|IT|S-0002|2024-03-01|1450000
      FA-IT-0002|Laptops batch 2024 (25 units)|ITEQ|HO|IT|S-0002|2024-06-15|1125000
      FA-IT-0003|Network switches and firewall|ITEQ|HO|IT|S-0002|2024-11-05|380000
      FA-IT-0004|Branch desktops Cebu (10)|ITEQ|CEB|IT|S-0002|2025-03-12|350000
      FA-IT-0005|Branch desktops Davao (8)|ITEQ|DVO|IT|S-0002|2025-04-22|280000
      FA-VEH-0001|Toyota Innova service vehicle|VEH|HO|EXEC|-|2024-01-20|1650000
      FA-VEH-0002|Toyota Vios claims adjuster car|VEH|HO|CLM|-|2024-07-01|895000
      FA-VEH-0003|Mitsubishi L300 van (Cebu)|VEH|CEB|CLM|-|2025-02-14|1180000
      FA-VEH-0004|Motorcycle for field sales (Davao)|VEH|DVO|MKT|-|2025-06-30|98000
      FA-FRN-0001|Office workstations Head Office|FURN|HO|FIN|S-0001|2024-04-01|760000
      FA-FRN-0002|Board room furniture|FURN|HO|EXEC|S-0001|2024-09-15|340000
      FA-FRN-0003|Cebu office furniture|FURN|CEB|FIN|S-0001|2025-05-02|410000
      FA-IT-0006|Laptops batch 2026 (15 units)|ITEQ|HO|IT|S-0002|2026-01-19|795000
      FA-IT-0007|Storage area network array|ITEQ|HO|IT|S-0002|2026-02-23|1280000
      FA-OFF-0005|Queue management system|OFFEQ|CEB|MKT|S-0001|2026-03-09|145000
      FA-VEH-0005|Toyota Hilux pick-up (Davao)|VEH|DVO|CLM|-|2026-04-15|1560000
      FA-FRN-0004|Davao office furniture|FURN|DVO|FIN|S-0001|2026-05-11|385000
      FA-OFF-0006|Generator set 60 kVA|OFFEQ|HO|EXEC|S-0001|2026-06-08|690000
      FA-IT-0008|Video conferencing kits|ITEQ|HO|IT|S-0002|2026-07-06|245000
      FA-IT-0009|Tablets for sales agents (20)|ITEQ|CEB|MKT|S-0002|2026-08-10|320000
      FA-OFF-0007|Document scanners (4)|OFFEQ|HO|UW|S-0001|2026-09-07|112000
      """;

  private static final List<DemoAsset> ASSETS =
      ASSET_TABLE.lines().map(line -> DemoAsset.parse(line.split("\\|"))).toList();

  private final CompanyRepository companies;
  private final BranchRepository branches;
  private final AssetCategoryRepository categories;
  private final FixedAssetRepository assets;
  private final FixedAssetService assetService;
  private final AssetLifecycleService lifecycle;
  private final DepreciationService depreciation;

  /**
   * Creates the runner.
   *
   * @param companies companies
   * @param branches branches
   * @param categories asset categories
   * @param assets asset repository
   * @param assetService asset service
   * @param lifecycle asset life cycle service
   * @param depreciation depreciation service
   */
  public FixedAssetDemoData(
      CompanyRepository companies,
      BranchRepository branches,
      AssetCategoryRepository categories,
      FixedAssetRepository assets,
      FixedAssetService assetService,
      AssetLifecycleService lifecycle,
      DepreciationService depreciation) {
    this.companies = companies;
    this.branches = branches;
    this.categories = categories;
    this.assets = assets;
    this.assetService = assetService;
    this.lifecycle = lifecycle;
    this.depreciation = depreciation;
  }

  @Override
  public void run(ApplicationArguments args) {
    Optional<Company> company = companies.findByCode(COMPANY);
    if (company.isEmpty()
        || categories.findByCompanyIdAndCode(company.get().getId(), "VEH").isEmpty()) {
      return;
    }
    Long companyId = company.get().getId();
    ASSETS.forEach(a -> register(companyId, a));
    for (YearMonth month = FIRST; !month.isAfter(LAST); month = month.plusMonths(1)) {
      if (month.equals(YearMonth.from(TRANSFER_DATE))) {
        transferBoardRoomFurniture(companyId);
      }
      if (month.equals(YearMonth.from(DISPOSAL_DATE))) {
        sellAdjusterCar(companyId);
      }
      YearMonth period = month;
      as(MANAGER, () -> depreciation.post(companyId, period));
    }
  }

  private void register(Long companyId, DemoAsset d) {
    FixedAsset asset =
        assets
            .findByCompanyIdAndTagNo(companyId, d.tag())
            .orElseGet(() -> as(MAKER, () -> assetService.create(request(companyId, d))));
    if (asset.getStatus() == AssetStatus.PENDING_CAPITALIZATION) {
      as(CHECKER, () -> lifecycle.capitalize(asset.getId()));
    }
  }

  private FixedAssetRequest request(Long companyId, DemoAsset d) {
    LocalDate acquired = LocalDate.parse(d.acquired());
    boolean takeOn = acquired.isBefore(TAKE_ON);
    String settlement = d.supplier() == null ? BANK : "2501";
    return new FixedAssetRequest(
        companyId,
        branchId(companyId, d.branch()),
        categories.findByCompanyIdAndCode(companyId, d.category()).orElseThrow().getId(),
        d.tag(),
        d.description(),
        d.costCenter(),
        d.supplier(),
        acquired,
        new BigDecimal(d.cost()),
        null,
        null,
        d.branch() + " office",
        "Branch administrator",
        takeOn ? null : settlement,
        takeOn,
        takeOn ? TAKE_ON : acquired,
        null,
        null);
  }

  private void transferBoardRoomFurniture(Long companyId) {
    FixedAsset asset = assets.findByCompanyIdAndTagNo(companyId, TRANSFERRED_TAG).orElseThrow();
    Long cebu = branchId(companyId, "CEB");
    if (!asset.getBranchId().equals(cebu)) {
      as(
          MAKER,
          () ->
              lifecycle.transfer(
                  asset.getId(),
                  new TransferRequest(
                      cebu,
                      TRANSFER_DATE,
                      "Cebu board room",
                      "Cebu branch manager",
                      "Board room moved to the Cebu regional office")));
    }
  }

  private void sellAdjusterCar(Long companyId) {
    FixedAsset asset = assets.findByCompanyIdAndTagNo(companyId, DISPOSED_TAG).orElseThrow();
    if (asset.getStatus() != AssetStatus.DISPOSED) {
      as(
          MAKER,
          () ->
              lifecycle.dispose(
                  asset.getId(),
                  new DisposalRequest(
                      DISPOSAL_DATE,
                      new BigDecimal("520000.00"),
                      BANK,
                      "DOS-2026-014",
                      "Sold to employee under the car plan")));
    }
  }

  private Long branchId(Long companyId, String code) {
    return branches.findByCompanyIdAndCode(companyId, code).orElseThrow().getId();
  }

  private static <T> T as(String user, Supplier<T> action) {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    try {
      return action.get();
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  /** A demo asset definition. */
  private record DemoAsset(
      String tag,
      String description,
      String category,
      String branch,
      String costCenter,
      String supplier,
      String acquired,
      String cost) {

    /** Parses a table line; fields are consumed left to right. */
    static DemoAsset parse(String[] fields) {
      Iterator<String> f = List.of(fields).iterator();
      return new DemoAsset(
          f.next(), f.next(), f.next(), f.next(), f.next(), none(f.next()), f.next(), f.next());
    }

    private static String none(String value) {
      return "-".equals(value) ? null : value;
    }
  }
}
