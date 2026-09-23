package com.iortatechnxt.finverse.fixedasset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.domain.RecordStatus;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.fixedasset.api.dto.AssetCategoryRequest;
import com.iortatechnxt.finverse.fixedasset.api.dto.DisposalRequest;
import com.iortatechnxt.finverse.fixedasset.api.dto.FixedAssetRequest;
import com.iortatechnxt.finverse.fixedasset.api.dto.TransferRequest;
import com.iortatechnxt.finverse.fixedasset.domain.AssetCategory;
import com.iortatechnxt.finverse.fixedasset.domain.AssetCategoryRepository;
import com.iortatechnxt.finverse.fixedasset.domain.AssetMovement;
import com.iortatechnxt.finverse.fixedasset.domain.AssetStatus;
import com.iortatechnxt.finverse.fixedasset.domain.DepreciationMethod;
import com.iortatechnxt.finverse.fixedasset.domain.DepreciationRun;
import com.iortatechnxt.finverse.fixedasset.domain.FixedAsset;
import com.iortatechnxt.finverse.fixedasset.domain.MovementType;
import com.iortatechnxt.finverse.fixedasset.service.AssetCategoryService;
import com.iortatechnxt.finverse.fixedasset.service.AssetLifecycleService;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationService;
import com.iortatechnxt.finverse.fixedasset.service.DepreciationService.Proposal;
import com.iortatechnxt.finverse.fixedasset.service.FixedAssetService;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Fixed asset life cycle against the demo chart; every test rolls back. */
@IntegrationTest
@Transactional
class FixedAssetIT {

  private static final YearMonth JULY = YearMonth.of(2026, 7);
  private static final YearMonth AUGUST = YearMonth.of(2026, 8);
  private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);

  @Autowired private FixedAssetService register;
  @Autowired private AssetLifecycleService lifecycle;
  @Autowired private DepreciationService depreciation;
  @Autowired private AssetCategoryService categoryService;
  @Autowired private AssetCategoryRepository categories;
  @Autowired private LedgerQueryService ledger;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private Long company() {
    return data.company().getId();
  }

  private AssetCategory category(String code) {
    return categories.findByCompanyIdAndCode(company(), code).orElseThrow();
  }

  private FixedAssetRequest request(String tag, String category, String date, String cost) {
    return new FixedAssetRequest(
        company(),
        data.branch("HO").getId(),
        category(category).getId(),
        tag,
        "Test asset " + tag,
        "FIN",
        "S-0001",
        LocalDate.parse(date),
        new BigDecimal(cost),
        null,
        null,
        "Head office",
        "Finance",
        "2501",
        false,
        null,
        null,
        null);
  }

  private FixedAsset capitalized(FixedAssetRequest request) {
    FixedAsset asset = as.run("accountant", () -> register.create(request));
    return as.run("checker", () -> lifecycle.capitalize(asset.getId()));
  }

  private BigDecimal balance(String account) {
    return ledger.netBalance(
        company(),
        accounts.getByCode(company(), account).getId(),
        null,
        LocalDate.of(2026, 12, 31));
  }

  private DepreciationRun run(YearMonth period) {
    return as.run("fmanager", () -> depreciation.post(company(), period));
  }

  @Test
  void capitalizationPostsCostAgainstTheSupplierPayableAfterCheckerApproval() {
    BigDecimal before = balance("1701");
    FixedAsset asset =
        as.run(
            "accountant",
            () -> register.create(request("T-CAP-1", "OFFEQ", "2026-08-05", "60000")));
    assertThat(asset.getStatus()).isEqualTo(AssetStatus.PENDING_CAPITALIZATION);
    assertThat(asset.getResidualValue()).isEqualByComparingTo("3000.00");
    assertThatThrownBy(() -> as.run("accountant", () -> lifecycle.capitalize(asset.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot be authorized");

    FixedAsset done = as.run("checker", () -> lifecycle.capitalize(asset.getId()));

    assertThat(done.getStatus()).isEqualTo(AssetStatus.ACTIVE);
    assertThat(done.getCapitalizationBatchNo()).isNotBlank();
    assertThat(balance("1701")).isEqualByComparingTo(before.add(new BigDecimal("60000")));
    assertThat(register.movements(done.getId()))
        .extracting(AssetMovement::getMovementType)
        .containsExactly(MovementType.ADDITION);
    assertThatThrownBy(
            () ->
                as.run(
                    "accountant",
                    () ->
                        register.update(
                            done.getId(), request("T-CAP-1", "OFFEQ", "2026-08-05", "60000"))))
        .hasMessageContaining("already capitalized");
  }

  @Test
  void straightLineAndDecliningBalanceRunIsPostedOnceAndCatchesUpMissedMonths() {
    FixedAsset straight = capitalized(request("T-SL-1", "OFFEQ", "2026-08-05", "60000"));
    FixedAsset declining = capitalized(request("T-DB-1", "ITEQ", "2026-09-01", "36000"));
    BigDecimal expenseBefore = balance("5611");

    List<Proposal> preview = depreciation.preview(company(), SEPTEMBER);
    assertThat(preview)
        .filteredOn(p -> p.asset().getId().equals(straight.getId()))
        .singleElement()
        .satisfies(
            p -> {
              assertThat(p.months()).isEqualTo(2);
              assertThat(p.amount()).isEqualByComparingTo("1900.00");
            });
    assertThat(preview)
        .filteredOn(p -> p.asset().getId().equals(declining.getId()))
        .singleElement()
        .satisfies(p -> assertThat(p.amount()).isEqualByComparingTo("2000.00"));

    DepreciationRun posted = run(SEPTEMBER);
    DepreciationRun again = run(SEPTEMBER);

    assertThat(again.getId()).isEqualTo(posted.getId());
    assertThat(posted.getTotalDepreciation()).isEqualByComparingTo("3900.00");
    assertThat(depreciation.lines(posted.getId())).hasSize(2);
    assertThat(depreciation.findRun(company(), SEPTEMBER)).isPresent();
    assertThat(depreciation.runs(company()))
        .extracting(DepreciationRun::getPeriod)
        .contains("2026-09");
    assertThat(balance("5611")).isEqualByComparingTo(expenseBefore.add(new BigDecimal("3900")));
    FixedAsset after = register.get(straight.getId());
    assertThat(after.getAccumulatedDepreciation()).isEqualByComparingTo("1900.00");
    assertThat(after.getLastDepreciationPeriod()).isEqualTo("2026-09");
    assertThat(depreciation.preview(company(), SEPTEMBER)).isEmpty();
  }

  @Test
  void disposalBooksLossOrGainAgainstNetBookValue() {
    FixedAsset lossAsset = capitalized(request("T-DSP-1", "OFFEQ", "2026-07-01", "60000"));
    FixedAsset gainAsset = capitalized(request("T-DSP-2", "OFFEQ", "2026-07-01", "60000"));
    DisposalRequest early =
        new DisposalRequest(
            LocalDate.of(2026, 9, 10), new BigDecimal("50000"), "1111", "OR-1", null);
    assertThatThrownBy(
            () -> as.run("accountant", () -> lifecycle.dispose(lossAsset.getId(), early)))
        .hasMessageContaining("Run depreciation up to 2026-08");
    run(JULY);
    run(AUGUST);
    BigDecimal lossBefore = balance("5613");
    BigDecimal gainBefore = balance("4700");

    AssetMovement loss = as.run("accountant", () -> lifecycle.dispose(lossAsset.getId(), early));
    AssetMovement gain =
        as.run(
            "accountant",
            () ->
                lifecycle.dispose(
                    gainAsset.getId(),
                    new DisposalRequest(
                        LocalDate.of(2026, 9, 10), new BigDecimal("60000"), "1111", null, "Sold")));

    assertThat(loss.getNetBookValue()).isEqualByComparingTo("58100.00");
    assertThat(loss.getGainLoss()).isEqualByComparingTo("-8100.00");
    assertThat(gain.getGainLoss()).isEqualByComparingTo("1900.00");
    assertThat(balance("5613")).isEqualByComparingTo(lossBefore.add(new BigDecimal("8100")));
    assertThat(balance("4700")).isEqualByComparingTo(gainBefore.subtract(new BigDecimal("1900")));
    assertThat(register.get(lossAsset.getId()).getStatus()).isEqualTo(AssetStatus.DISPOSED);
    assertThatThrownBy(
            () -> as.run("accountant", () -> lifecycle.dispose(lossAsset.getId(), early)))
        .hasMessageContaining("not in service");
    DisposalRequest noBank =
        new DisposalRequest(LocalDate.of(2026, 9, 10), BigDecimal.ONE, null, null, null);
    FixedAsset third = capitalized(request("T-DSP-3", "OFFEQ", "2026-09-01", "1000"));
    assertThatThrownBy(() -> as.run("accountant", () -> lifecycle.dispose(third.getId(), noBank)))
        .hasMessageContaining("bank account");
  }

  @Test
  void transferMovesTheAssetThroughInterBranchClearing() {
    FixedAsset asset = capitalized(request("T-TRF-1", "FURN", "2026-08-03", "84000"));
    run(AUGUST);
    Long cebu = data.branch("CEB").getId();
    BigDecimal cebuBefore =
        ledger.netBalance(
            company(),
            accounts.getByCode(company(), "1704").getId(),
            cebu,
            LocalDate.of(2026, 12, 31));

    AssetMovement movement =
        as.run(
            "accountant",
            () ->
                lifecycle.transfer(
                    asset.getId(),
                    new TransferRequest(
                        cebu, LocalDate.of(2026, 9, 2), "Cebu", "Manager", "Move")));

    assertThat(movement.getBatchNo()).contains("/");
    assertThat(movement.getAccumulatedDepreciation()).isEqualByComparingTo("1000.00");
    FixedAsset moved = register.get(asset.getId());
    assertThat(moved.getBranchId()).isEqualTo(cebu);
    assertThat(moved.getStatus()).isEqualTo(AssetStatus.TRANSFERRED);
    assertThat(
            ledger.netBalance(
                company(),
                accounts.getByCode(company(), "1704").getId(),
                cebu,
                LocalDate.of(2026, 12, 31)))
        .isEqualByComparingTo(cebuBefore.add(new BigDecimal("84000")));
    TransferRequest same = new TransferRequest(cebu, LocalDate.of(2026, 9, 3), null, null, null);
    assertThatThrownBy(() -> as.run("accountant", () -> lifecycle.transfer(asset.getId(), same)))
        .hasMessageContaining("already there");
  }

  @Test
  void takeOnAssetPostsOpeningBalanceWithComputedAccumulatedDepreciation() {
    FixedAssetRequest takeOn =
        new FixedAssetRequest(
            company(),
            data.branch("DVO").getId(),
            category("VEH").getId(),
            "T-TKO-1",
            "Legacy vehicle",
            "CLM",
            null,
            LocalDate.of(2025, 1, 15),
            new BigDecimal("600000"),
            DepreciationMethod.STRAIGHT_LINE,
            60,
            null,
            null,
            null,
            true,
            LocalDate.of(2026, 1, 1),
            null,
            null);
    BigDecimal retained = balance("3500");

    FixedAsset asset = as.run("accountant", () -> register.create(takeOn));
    assertThat(asset.getOpeningMonths()).isEqualTo(12);
    assertThat(asset.getOpeningAccumulatedDepreciation()).isEqualByComparingTo("108000.00");
    FixedAsset done = as.run("checker", () -> lifecycle.capitalize(asset.getId()));

    assertThat(done.netBookValue()).isEqualByComparingTo("492000.00");
    assertThat(balance("3500")).isEqualByComparingTo(retained.subtract(new BigDecimal("492000")));
    assertThat(register.movements(done.getId())).isEmpty();
    assertThat(register.search(company(), AssetStatus.ACTIVE, null, null, "t-tko"))
        .extracting(FixedAsset::getTagNo)
        .containsExactly("T-TKO-1");
  }

  @Test
  void invalidRegistrationsAreRejected() {
    FixedAssetRequest noSettlement =
        new FixedAssetRequest(
            company(),
            data.branch("HO").getId(),
            category("OFFEQ").getId(),
            "T-BAD-1",
            "Bad",
            null,
            null,
            LocalDate.of(2026, 8, 1),
            BigDecimal.TEN,
            null,
            null,
            null,
            null,
            null,
            false,
            null,
            null,
            null);
    assertThatThrownBy(() -> as.run("accountant", () -> register.create(noSettlement)))
        .hasMessageContaining("settlement account");
    FixedAssetRequest backwards =
        new FixedAssetRequest(
            company(),
            data.branch("HO").getId(),
            category("OFFEQ").getId(),
            "T-BAD-2",
            "Bad",
            null,
            null,
            LocalDate.of(2026, 8, 1),
            BigDecimal.TEN,
            null,
            null,
            null,
            null,
            "1111",
            false,
            LocalDate.of(2026, 7, 1),
            null,
            null);
    assertThatThrownBy(() -> as.run("accountant", () -> register.create(backwards)))
        .hasMessageContaining("precedes acquisition");
    FixedAssetRequest badTakeOn =
        new FixedAssetRequest(
            company(),
            data.branch("HO").getId(),
            category("OFFEQ").getId(),
            "T-BAD-3",
            "Bad",
            null,
            null,
            LocalDate.of(2026, 8, 1),
            BigDecimal.TEN,
            null,
            null,
            null,
            null,
            null,
            true,
            null,
            null,
            null);
    assertThatThrownBy(() -> as.run("accountant", () -> register.create(badTakeOn)))
        .hasMessageContaining("take-on date");
    FixedAssetRequest tooMuch =
        new FixedAssetRequest(
            company(),
            data.branch("HO").getId(),
            category("OFFEQ").getId(),
            "T-BAD-4",
            "Bad",
            null,
            null,
            LocalDate.of(2025, 8, 1),
            BigDecimal.TEN,
            null,
            null,
            null,
            null,
            null,
            true,
            LocalDate.of(2026, 1, 1),
            new BigDecimal("50"),
            5);
    assertThatThrownBy(() -> as.run("accountant", () -> register.create(tooMuch)))
        .hasMessageContaining("exceeds");
  }

  @Test
  void categoriesFollowMakerChecker() {
    AssetCategoryRequest request =
        new AssetCategoryRequest(
            company(),
            "T-CAT",
            "Test category",
            "1701",
            "1709",
            "5611",
            DepreciationMethod.STRAIGHT_LINE,
            24,
            BigDecimal.ONE);
    AssetCategory created = as.run("accountant", () -> categoryService.create(request));
    assertThat(created.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThatThrownBy(
            () -> as.run("accountant", () -> categoryService.requireActive(created.getId())))
        .hasMessageContaining("not active");
    AssetCategory authorized = as.run("checker", () -> categoryService.authorize(created.getId()));
    assertThat(authorized.isActive()).isTrue();
    AssetCategory updated =
        as.run("accountant", () -> categoryService.update(created.getId(), request));
    assertThat(updated.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThat(categoryService.list(company()))
        .extracting(AssetCategory::getCode)
        .contains("T-CAT");
    AssetCategoryRequest heading =
        new AssetCategoryRequest(
            company(),
            "T-CAT2",
            "Heading",
            "1700",
            "1709",
            "5611",
            DepreciationMethod.STRAIGHT_LINE,
            24,
            BigDecimal.ONE);
    assertThatThrownBy(() -> as.run("accountant", () -> categoryService.create(heading)))
        .hasMessageContaining("heading");
    AssetCategoryRequest unknown =
        new AssetCategoryRequest(
            company(),
            "T-CAT3",
            "Unknown",
            "0000",
            "1709",
            "5611",
            DepreciationMethod.STRAIGHT_LINE,
            24,
            BigDecimal.ONE);
    assertThatThrownBy(() -> as.run("accountant", () -> categoryService.create(unknown)))
        .hasMessageContaining("Unknown GL account");
  }
}
