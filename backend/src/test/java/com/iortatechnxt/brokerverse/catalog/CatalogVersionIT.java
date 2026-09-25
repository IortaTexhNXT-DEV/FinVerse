package com.iortatechnxt.brokerverse.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.catalog.domain.FieldRule;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRuleType;
import com.iortatechnxt.brokerverse.catalog.domain.FieldTarget;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLifecycle;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersion;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.RuleScope;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.catalog.service.FieldValues;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.version.CatalogPackageSetupService;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSetupService;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageVersionLifecycleJob;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductExpired;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueries;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReleased;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionReturned;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.catalog.service.version.VersionRef;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Package versions (BRPM.006/007/017, PMADD01/02/06): drafted by MBS, submitted, validated by the
 * TSU Head and released; returned; superseded, expired and retired.
 */
@IntegrationTest
@RecordApplicationEvents
class CatalogVersionIT {

  @Autowired private PackageFixtures fx;
  @Autowired private PackageSetupService setup;
  @Autowired private CatalogPackageSetupService catalogSetup;
  @Autowired private ProductVersionService versions;
  @Autowired private ProductVersionQueries queries;
  @Autowired private ProductVersionQueryService queryContract;
  @Autowired private ProductCatalogService catalog;
  @Autowired private ProductRuleService rules;
  @Autowired private CatalogRecords records;
  @Autowired private ApplicationContext context;
  @Autowired private PackageVersionLifecycleJob lifecycleJob;
  @Autowired private ApplicationEvents events;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  @Test
  void theCatalogImplementsTheContractsInsteadOfTheStubs() {
    assertThat(context.getBean(PackageSetupService.class))
        .isInstanceOf(CatalogPackageSetupService.class);
    assertThat(context.getBean(ProductVersionQueryService.class))
        .isInstanceOf(ProductVersionQueries.class);
    assertThat(queryContract.current("MTR10")).map(ProductVersionView::versionNo).contains(1);
    assertThat(queryContract.versions("MOP07")).isEmpty();
  }

  @Test
  void aPackageVersionIsDraftedValidatedAndReleased() {
    String code = PackageFixtures.code();
    LocalDate today = fx.today();
    VersionRef draft =
        as.run(
            "mbs",
            () -> setup.createDraftVersion(fx.spec(code, true, today, PackageFixtures.RATE)));
    assertThat(draft.versionNo()).isEqualTo(1);
    assertThat(draft.status()).isEqualTo(ProductVersionStatus.DRAFT);
    RiskProduct product = catalog.requireProduct(code);
    assertThat(product.isPackaged()).isTrue();
    assertThat(product.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThat(queryContract.current(code)).isEmpty();
    assertThatThrownBy(
            () ->
                as.run(
                    "mbs",
                    () ->
                        setup.createDraftVersion(
                            fx.spec(code, false, today, PackageFixtures.RATE))))
        .extracting("code")
        .isEqualTo("VERSION_IN_PROGRESS");

    PackageSpec full = fx.spec(code, false, today, PackageFixtures.RATE);
    PackageSpec withoutTerms =
        new PackageSpec(
            full.companyId(),
            code,
            null,
            null,
            full.rateScheme(),
            full.dates(),
            full.coverages(),
            full.insurers(),
            full.insurerTerms().subList(0, 2),
            full.origin());
    as.run("mbs", () -> setup.updateDraftVersion(code, 1, withoutTerms));
    assertThatThrownBy(() -> as.run("mbs", () -> versions.submitForValidation(code, 1)))
        .extracting("code")
        .isEqualTo("PACKAGE_INSURER_TERMS_MISSING");
    as.run("mbs", () -> setup.updateDraftVersion(code, 1, full));

    ProductVersion submitted = as.run("mbs", () -> versions.submitForValidation(code, 1));
    assertThat(submitted.getStatus()).isEqualTo(ProductVersionStatus.FOR_VALIDATION);
    assertThatThrownBy(() -> as.run("mbs", () -> versions.validate(code, 1, List.of())))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> as.run("mbs", () -> setup.updateDraftVersion(code, 1, full)))
        .extracting("code")
        .isEqualTo("VERSION_NOT_DRAFT");

    ProductVersion released =
        as.run(
            "tsuhead",
            () -> versions.validate(code, 1, List.of("Hierarchy complete", "Terms checked")));
    assertThat(released.getStatus()).isEqualTo(ProductVersionStatus.RELEASED);
    assertThat(released.getValidatedBy()).isEqualTo("tsuhead");
    assertThat(released.getTestPremium()).isPositive();
    assertThat(released.getValidationChecklist()).contains("Terms checked");
    RiskProduct live = catalog.requireProduct(code);
    assertThat(live.getRecordStatus()).isEqualTo(RecordStatus.ACTIVE);
    assertThat(live.getDefaultRate()).isEqualByComparingTo(PackageFixtures.RATE);
    assertThat(live.getMinimumPremium()).isEqualByComparingTo("3000");
    assertThat(events.stream(ProductVersionReleased.class))
        .anyMatch(e -> e.productCode().equals(code) && "tsuhead".equals(e.validatedBy()));
    ProductVersionView current = queryContract.current(code).orElseThrow();
    assertThat(current.versionNo()).isEqualTo(1);
    assertThat(current.origin().sourceRequestNo()).isEqualTo("PKR-TEST-" + code);
    assertThat(current.insurerTerms()).hasSize(4);
    assertThat(queryContract.packagesExpiring(fx.company(), 400))
        .extracting(ProductVersionView::productCode)
        .contains(code);
    assertThat(catalog.requireSellable(code, RatingQuery.Purpose.NEW_BUSINESS, today)).isNotNull();
  }

  @Test
  void aSubmittedVersionIsReturnedAndACompleteHierarchyIsRequired() {
    String code = fx.released();
    fx.backdate(code, 1);
    VersionRef v2 =
        as.run("mbs", () -> catalogSetup.draftFromCurrent(fx.company(), code, "Rate review"));
    assertThat(v2.versionNo()).isEqualTo(2);
    assertThat(v2.effectiveFrom()).isEqualTo(fx.today().plusDays(1));
    as.run("mbs", () -> versions.submitForValidation(code, 2));
    ProductVersion returned =
        as.run("tsuhead", () -> versions.returnToDraft(code, 2, "Rate not signed off"));
    assertThat(returned.getStatus()).isEqualTo(ProductVersionStatus.DRAFT);
    assertThat(returned.getReturnedReason()).isEqualTo("Rate not signed off");
    assertThat(events.stream(ProductVersionReturned.class))
        .anyMatch(e -> e.productCode().equals(code) && e.versionNo() == 2);

    PackageSpec noBasic =
        new PackageSpec(
            fx.company(),
            code,
            null,
            null,
            new PackageSpec.RateScheme(null, BigDecimal.ZERO, BigDecimal.TEN, null, null),
            new PackageSpec.PackageDates(fx.today().plusDays(1), null, null, null),
            List.of(new PackageSpec.Coverage("PD", true, false, null, null, null, 1)),
            List.of(),
            List.of(),
            new PackageSpec.Origin(null, null, "No basic coverage"));
    as.run("mbs", () -> setup.updateDraftVersion(code, 2, noBasic));
    assertThatThrownBy(() -> as.run("mbs", () -> versions.submitForValidation(code, 2)))
        .extracting("code")
        .isEqualTo("PACKAGE_HIERARCHY_INCOMPLETE");
  }

  @Test
  void aReleasedSuccessorSupersedesAndTheCommercialColumnsFollowTheVersions() {
    String code = fx.released();
    fx.backdate(code, 1);
    as.run(
        "mbs",
        () -> setup.createDraftVersion(fx.spec(code, false, fx.today(), new BigDecimal("1.75"))));
    fx.release(code, 2);
    List<ProductVersion> all = queries.entities(code);
    assertThat(all.get(0).getStatus()).isEqualTo(ProductVersionStatus.RELEASED);
    assertThat(all.get(1).getStatus()).isEqualTo(ProductVersionStatus.SUPERSEDED);
    assertThat(all.get(1).getEffectiveTo()).isEqualTo(fx.today().minusDays(1));
    assertThat(catalog.requireProduct(code).getDefaultRate()).isEqualByComparingTo("1.75");
    assertThat(queryContract.inForce(code, LocalDate.of(2021, 1, 1)))
        .map(ProductVersionView::versionNo)
        .contains(1);

    RiskProduct product = catalog.requireProduct(code);
    ProductDetails changedRate =
        new ProductDetails(
            product.getName(),
            product.getLineCode(),
            product.getCoverTypeCode(),
            true,
            false,
            product.getMarketSegmentList(),
            false,
            false,
            false,
            1,
            false,
            product.getPaymentGate(),
            new BigDecimal("9"),
            product.getDefaultCommissionRate(),
            product.getMinimumPremium(),
            product.getMaxSumInsured(),
            product.getTsuInvolvement());
    assertThatThrownBy(() -> as.run("badmin", () -> catalog.updateProduct(code, changedRate)))
        .extracting("code")
        .isEqualTo("PRODUCT_FIELD_VERSIONED");
  }

  @Test
  void packagesExpireAndRetireWithoutBeingDeleted() {
    String code = fx.released();
    jdbc.update(
        "update cat_product_version set effective_from = date '2020-01-01',"
            + " package_start_date = date '2020-01-01', package_end_date = ?"
            + " where product_code = ?",
        fx.today().minusDays(1),
        code);
    assertThat(lifecycleJob.execute(fx.today()).itemsProcessed()).isPositive();
    assertThat(queries.require(code, 1).getStatus()).isEqualTo(ProductVersionStatus.EXPIRED);
    assertThat(catalog.requireProduct(code).getLifecycleStatus())
        .isEqualTo(ProductLifecycle.EXPIRED);
    assertThat(events.stream(ProductExpired.class)).anyMatch(e -> e.productCode().equals(code));
    assertThatThrownBy(
            () -> catalog.requireSellable(code, RatingQuery.Purpose.NEW_BUSINESS, fx.today()))
        .extracting("code")
        .isEqualTo("PRODUCT_NOT_SELLABLE");
    assertThat(catalog.requireSellable(code, RatingQuery.Purpose.RENEWAL, fx.today())).isNotNull();
    assertThat(
            catalog.products(
                new ProductCatalogService.ProductFilter(
                    null, null, null, code, false, ProductLifecycle.EXPIRED, false)))
        .extracting(RiskProduct::getCode)
        .containsExactly(code);
    assertThat(
            catalog.products(
                new ProductCatalogService.ProductFilter(null, null, null, code, false)))
        .isEmpty();

    String retired = fx.released();
    as.run(
        "mbs",
        () -> {
          setup.retireProduct(retired, new PackageSpec.Origin("PKR-RET", "MC-RET", "Retire"));
          return null;
        });
    assertThat(catalog.requireProduct(retired).getLifecycleStatus())
        .isEqualTo(ProductLifecycle.RETIRED);
    assertThatThrownBy(
            () ->
                as.run(
                    "mbs",
                    () -> {
                      setup.retireProduct(retired, null);
                      return null;
                    }))
        .extracting("code")
        .isEqualTo("PRODUCT_NOT_ACTIVE");
    assertThat(queries.entities(retired)).hasSize(1);
  }

  @Test
  void typedFieldRulesCheckTheGivenValues() {
    String code = fx.released();
    RiskProduct product = catalog.requireProduct(code);
    FieldRule range =
        as.run(
            "badmin",
            () ->
                rules.createFieldRule(
                    new FieldRule.RuleKey(RuleScope.PRODUCT, code, FieldTarget.ITEM, "yearModel"),
                    "Year model",
                    false,
                    10,
                    new FieldRule.Check(
                        FieldRuleType.RANGE, null, new BigDecimal("2000"), null, null)));
    FieldRule lov =
        as.run(
            "badmin",
            () ->
                rules.createFieldRule(
                    new FieldRule.RuleKey(
                        RuleScope.PRODUCT, code, FieldTarget.ACCOUNT, "marketSegment"),
                    "Segment",
                    true,
                    20,
                    new FieldRule.Check(FieldRuleType.LOV, "MARKET_SEGMENT", null, null, null)));
    FieldRule pattern =
        as.run(
            "badmin",
            () ->
                rules.createFieldRule(
                    new FieldRule.RuleKey(RuleScope.PRODUCT, code, FieldTarget.ITEM, "plateNo"),
                    "Plate number",
                    false,
                    30,
                    new FieldRule.Check(
                        FieldRuleType.PATTERN, null, null, null, "[A-Z]{3} \\d{3,4}")));
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        rules.createFieldRule(
                            new FieldRule.RuleKey(
                                RuleScope.PRODUCT, code, FieldTarget.ITEM, "make"),
                            "Make",
                            false,
                            40,
                            new FieldRule.Check(FieldRuleType.PATTERN, null, null, null, "("))))
        .extracting("code")
        .isEqualTo("FIELD_RULE_INCOMPLETE");
    for (FieldRule rule : List.of(range, lov, pattern)) {
      as.run("approver", () -> records.authorize(CatalogKind.FIELD_RULE, rule.getId()));
    }
    var errors =
        rules.violations(
            product,
            new FieldValues(
                java.util.Map.of("marketSegment", "NOPE"),
                List.of(java.util.Map.of("yearModel", "1995", "plateNo", "ABC 1234"))));
    assertThat(errors).containsOnlyKeys("marketSegment", "items[0].yearModel");
    assertThat(
            rules.violations(
                product,
                new FieldValues(
                    java.util.Map.of("marketSegment", "CBG"),
                    List.of(java.util.Map.of("yearModel", "abc", "plateNo", "bad")))))
        .containsOnlyKeys("items[0].yearModel", "items[0].plateNo");
  }
}
