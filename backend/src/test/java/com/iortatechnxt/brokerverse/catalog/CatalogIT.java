package com.iortatechnxt.brokerverse.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRule;
import com.iortatechnxt.brokerverse.catalog.domain.FieldRule.RuleKey;
import com.iortatechnxt.brokerverse.catalog.domain.FieldTarget;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBranch.BranchDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.InsurerDetails;
import com.iortatechnxt.brokerverse.catalog.domain.PaymentGate;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.catalog.domain.ProductClass;
import com.iortatechnxt.brokerverse.catalog.domain.ProductLine.LineDetails;
import com.iortatechnxt.brokerverse.catalog.domain.RatingMethod;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct.ProductDetails;
import com.iortatechnxt.brokerverse.catalog.domain.RuleScope;
import com.iortatechnxt.brokerverse.catalog.domain.SalesLevel;
import com.iortatechnxt.brokerverse.catalog.domain.SalesUnit.UnitDetails;
import com.iortatechnxt.brokerverse.catalog.domain.TsuInvolvement;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule.TsuCriteria;
import com.iortatechnxt.brokerverse.catalog.domain.TsuRule.TsuFacts;
import com.iortatechnxt.brokerverse.catalog.service.CatalogApprovalSource;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.catalog.service.FieldPresence;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService.PartyContact;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService.ProductFilter;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
import com.iortatechnxt.brokerverse.catalog.service.RateResolver;
import com.iortatechnxt.brokerverse.catalog.service.RateTableService;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.catalog.service.TsuRoutingService;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CatalogIT {

  @Autowired private ProductCatalogService catalog;
  @Autowired private ProductRuleService rules;
  @Autowired private CatalogRecords records;
  @Autowired private CatalogApprovalSource approvals;
  @Autowired private InsurerService insurers;
  @Autowired private PartyService parties;
  @Autowired private RateTableService rates;
  @Autowired private RateResolver resolver;
  @Autowired private RatingService rating;
  @Autowired private TsuRoutingService tsu;
  @Autowired private SalesOrganisationService sales;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private static String unique(String prefix) {
    return prefix + Long.toString(System.nanoTime() % 1_000_000_000L, 36).toUpperCase();
  }

  private static ProductDetails details(String line, String cover, boolean packaged) {
    return new ProductDetails(
        "Test product",
        line,
        cover,
        packaged,
        false,
        List.of("CBG"),
        false,
        true,
        true,
        3,
        false,
        PaymentGate.CLIENT_CONFIRMATION,
        new BigDecimal("0.5"),
        new BigDecimal("15"),
        new BigDecimal("1000"),
        null,
        TsuInvolvement.BY_RULES);
  }

  @Test
  void seededCatalogFollowsAnnexesAndAppendixB() {
    assertThat(catalog.lines()).extracting("code").contains("PROPERTY", "MOTOR", "OTHERS");
    List<RiskProduct> motor = catalog.products(new ProductFilter("MOTOR", true, "CBG", null, true));
    assertThat(motor).extracting(RiskProduct::getCode).contains("MTR10", "MTR38", "CTP01");
    assertThat(catalog.products(new ProductFilter(null, null, null, "car2", false)))
        .extracting(RiskProduct::getCode)
        .contains("CAR20", "CAR29");
    RiskProduct par = catalog.requireUsableProduct("PAR01");
    assertThat(par.getPaymentGate()).isEqualTo(PaymentGate.PAID);
    assertThat(par.isMultiYearAllowed()).isTrue();
    assertThat(par.allowsSegment("CBG")).isTrue();
    assertThat(par.allowsSegment("CORBANK")).isFalse();
    assertThat(catalog.requireProduct("MOP07").isPackaged()).isFalse();
    assertThat(catalog.coverTypes()).extracting("code").contains("TPL_ONLY", "JEWELERS_BLOCK");
  }

  @Test
  void minimumFieldMatrixAppliesTheNarrowestScope() {
    RiskProduct mtr = catalog.requireProduct("MTR10");
    List<FieldRule> effective = rules.effectiveFieldRules(mtr);
    assertThat(effective)
        .extracting(FieldRule::getFieldKey)
        .contains("clientId", "plateNo|conductionSticker", "engineNo", "sumInsured");
    assertThat(rules.requiredDocuments(mtr)).containsExactly("IDF");

    var missing =
        rules.missingFields(
            mtr,
            new FieldPresence(
                Set.of("clientId", "marketSegment", "periodFrom", "periodTo", "currency", "items"),
                List.of(Set.of("sumInsured", "conductionSticker", "chassisNo", "make", "model"))));
    assertThat(missing).containsOnlyKeys("items[0].engineNo", "items[0].yearModel");
    assertThat(missing.get("items[0].engineNo")).contains("Item 1").contains("Engine");

    String code = unique("TM");
    as.run("badmin", () -> catalog.createProduct(code, details("MOTOR", "COMPREHENSIVE", true)));
    as.run(
        "badmin",
        () ->
            rules.createFieldRule(
                new RuleKey(RuleScope.PRODUCT, code, FieldTarget.ITEM, "engineNo"),
                "Engine number (optional for this product)",
                false,
                30));
    RiskProduct product = catalog.requireProduct(code);
    var relaxed =
        rules.missingFields(
            product,
            new FieldPresence(
                Set.of("clientId", "marketSegment", "periodFrom", "periodTo", "currency", "items"),
                List.of(
                    Set.of("sumInsured", "plateNo", "chassisNo", "make", "model", "yearModel"))));
    // the pending product rule does not apply until authorized
    assertThat(relaxed).containsOnlyKeys("items[0].engineNo");
    FieldRule rule =
        rules.fieldRules().stream()
            .filter(r -> r.getScopeCode().equals(code))
            .findFirst()
            .orElseThrow();
    as.run("approver", () -> records.authorize(CatalogKind.FIELD_RULE, rule.getId()));
    assertThat(rules.missingFields(product, new FieldPresence(Set.of(), List.of())))
        .containsKey("clientId")
        .doesNotContainKey("items[0].engineNo");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        rules.createFieldRule(
                            new RuleKey(RuleScope.ALL, "MOTOR", FieldTarget.ITEM, "make"),
                            "x",
                            true,
                            1)))
        .extracting("code")
        .isEqualTo("RULE_SCOPE_INVALID");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () -> rules.createDocumentRule(RuleScope.PRODUCT, code, "NOT_A_DOC", true)))
        .extracting("code")
        .isEqualTo("LOV_VALUE_INVALID");
    var doc =
        as.run("badmin", () -> rules.createDocumentRule(RuleScope.PRODUCT, code, "IDF", false));
    as.run("badmin", () -> rules.updateDocumentRule(doc.getId(), false));
    as.run("approver", () -> records.authorize(CatalogKind.DOCUMENT_RULE, doc.getId()));
    assertThat(rules.requiredDocuments(product)).isEmpty();
  }

  @Test
  void productsAreMakerCheckerAndListedInTheInbox() {
    String code = unique("TP");
    RiskProduct product =
        as.run("badmin", () -> catalog.createProduct(code, details("LIABILITY", "CGL", false)));
    assertThat(product.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThatThrownBy(() -> catalog.requireUsableProduct(code))
        .extracting("code")
        .isEqualTo("PRODUCT_NOT_ACTIVE");
    assertThatThrownBy(
            () -> as.run("badmin", () -> records.authorize(CatalogKind.PRODUCT, product.getId())))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");
    ApprovalViewer approver = ApprovalViewer.user("approver", Set.of("MASTER_AUTHORIZE"));
    assertThat(approvals.pendingFor(approver)).extracting("reference").contains(code);
    as.run("approver", () -> records.authorize(CatalogKind.PRODUCT, product.getId()));
    assertThat(catalog.requireUsableProduct(code).getMaxTermYears()).isEqualTo(3);

    as.run("badmin", () -> catalog.updateProduct(code, details("LIABILITY", "CYBER", false)));
    assertThat(catalog.requireProduct(code).getRecordStatus())
        .isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () -> catalog.updateProduct(code, details("LIABILITY", "CAR", false))))
        .extracting("code")
        .isEqualTo("COVER_TYPE_INVALID");
    assertThatThrownBy(
            () -> as.run("badmin", () -> catalog.createProduct(code, details("MOTOR", null, true))))
        .hasMessageContaining(code);
    as.run("badmin", () -> records.deactivate(CatalogKind.PRODUCT, product.getId()));
    assertThat(catalog.requireProduct(code).getRecordStatus()).isEqualTo(RecordStatus.INACTIVE);

    String line = unique("L");
    as.run(
        "badmin",
        () ->
            catalog.createLine(
                line,
                new LineDetails("Test line", RiskItemKind.GENERIC, RatingMethod.GENERIC, 500)));
    as.run("badmin", () -> catalog.createCoverType(line, "BASIC", "Basic cover", 10));
    as.run(
        "badmin",
        () ->
            catalog.updateLine(
                line,
                new LineDetails("Test line 2", RiskItemKind.PERSON, RatingMethod.GENERIC, 500)));
    assertThat(catalog.requireLine(line).getRiskItemKind()).isEqualTo(RiskItemKind.PERSON);
  }

  @Test
  void insurersComeWithTheirPartyBranchesAndLgt() {
    Long company = data.company().getId();
    String code = unique("INS-");
    InsurerDetails details =
        new InsurerDetails(
            "Test Mutual Insurance",
            "Test Mutual",
            "IC-TEST-1",
            LocalDate.of(2027, 12, 31),
            PlacementChannel.EMAIL,
            List.of("nb@testmutual.example"),
            30);
    InsurerProfile insurer =
        as.run(
            "badmin",
            () ->
                insurers.create(
                    company,
                    code,
                    new PartyContact("123", "Makati", "a@b.example", "02"),
                    details));
    InsurerBranch branch =
        as.run(
            "badmin",
            () ->
                insurers.createBranch(
                    insurer.getId(),
                    "MNL",
                    new BranchDetails("Manila", "Manila", new BigDecimal("0.7"), null)));
    assertThatThrownBy(() -> insurers.requireUsableBranch(company, code, "MNL"))
        .extracting("code")
        .isEqualTo("INSURER_NOT_ACTIVE");
    as.run("approver", () -> records.authorize(CatalogKind.INSURER, insurer.getId()));
    assertThat(parties.getByCode(company, code).isActive()).isTrue();
    assertThatThrownBy(() -> insurers.requireUsableBranch(company, code, "MNL"))
        .extracting("code")
        .isEqualTo("INSURER_BRANCH_NOT_ACTIVE");
    as.run("approver", () -> records.authorize(CatalogKind.INSURER_BRANCH, branch.getId()));
    assertThat(insurers.requireUsableBranch(company, code, "MNL").getLgtRate())
        .isEqualByComparingTo("0.7");
    assertThatThrownBy(() -> insurers.requireUsableBranch(company, code, "XXX"))
        .extracting("code")
        .isEqualTo("INSURER_BRANCH_UNKNOWN");
    assertThat(insurer.isAccreditedOn(LocalDate.of(2028, 1, 1))).isFalse();

    InsurerDetails sftp =
        new InsurerDetails("x", null, null, null, PlacementChannel.SFTP, List.of(), 30);
    assertThatThrownBy(() -> as.run("badmin", () -> insurers.update(insurer.getId(), sftp)))
        .extracting("code")
        .isEqualTo("PLACEMENT_CHANNEL_PARKED");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        insurers.create(
                            company,
                            "C-000101",
                            new PartyContact(null, null, null, null),
                            details)))
        .extracting("code")
        .isEqualTo("PARTY_NOT_INSURER");
    as.run(
        "badmin",
        () ->
            insurers.updateBranch(
                branch.getId(), new BranchDetails("Manila 2", null, new BigDecimal("0.8"), null)));
    assertThat(insurers.branches(insurer.getId()))
        .extracting(InsurerBranch::getName)
        .contains("Manila 2");
  }

  @Test
  void tsuRoutingFollowsTheRules() {
    RiskProduct packaged = catalog.requireProduct("MTR10");
    RiskProduct nonPackage = catalog.requireProduct("MOP07");
    assertThat(
            tsu.evaluate(nonPackage, new TsuFacts(false, "MARINE", 0, 0, BigDecimal.TEN, null))
                .ruleCode())
        .isEqualTo("NON_PACKAGE");
    assertThat(
            tsu.evaluate(packaged, new TsuFacts(true, "MOTOR", 5, 0, BigDecimal.TEN, null))
                .ruleCode())
        .isEqualTo("FLEET_5");
    assertThat(
            tsu.evaluate(packaged, new TsuFacts(true, "MOTOR", 4, 0, BigDecimal.TEN, null))
                .required())
        .isFalse();
    assertThat(
            tsu.evaluate(
                    catalog.requireProduct("CGL01"),
                    new TsuFacts(true, "LIABILITY", 1, 0, new BigDecimal("50000000.01"), null))
                .ruleCode())
        .isEqualTo("TSI_50M");
    assertThat(
            tsu.evaluate(
                    catalog.requireProduct("CGL01"),
                    new TsuFacts(true, "LIABILITY", 1, 0, new BigDecimal("50000000"), null))
                .required())
        .isFalse();

    String code = unique("R");
    var rule =
        as.run(
            "badmin",
            () ->
                tsu.create(
                    code,
                    new TsuCriteria(
                        "Several fire locations",
                        ProductClass.PACKAGE,
                        "PROPERTY",
                        null,
                        3,
                        null,
                        null,
                        5)));
    as.run("approver", () -> records.authorize(CatalogKind.TSU_RULE, rule.getId()));
    RiskProduct par = catalog.requireProduct("PAR01");
    assertThat(
            tsu.evaluate(par, new TsuFacts(true, "PROPERTY", 0, 3, BigDecimal.ONE, null))
                .ruleCode())
        .isEqualTo(code);
    as.run(
        "badmin",
        () ->
            tsu.update(
                rule.getId(),
                new TsuCriteria(
                    "Endorsements", ProductClass.ANY, null, null, null, null, "EXTENSION", 5)));
    assertThat(tsu.rules()).extracting("code").contains(code);

    var aboveLimit =
        tsu.evaluate(
            packaged, new TsuFacts(true, "MOTOR", 1, 0, new BigDecimal("5000000.01"), null));
    assertThat(aboveLimit.ruleCode()).isEqualTo(TsuRoutingService.PACKAGE_LIMIT);
    assertThat(aboveLimit.reason()).contains("5000000");
    assertThat(packaged.exceedsPackageLimit(new BigDecimal("5000000"))).isFalse();
  }

  @Test
  void insurerPanelListsActiveInsurersWithTheirBranches() {
    var panel = insurers.panel(data.company().getId());
    var mabuhay =
        panel.stream()
            .filter(p -> p.insurer().getPartyCode().equals("INS-MGIC"))
            .findFirst()
            .orElseThrow();
    assertThat(mabuhay.branches()).extracting(InsurerBranch::getCode).contains("MKT", "CEB", "DVO");
    assertThat(panel).allMatch(p -> p.insurer().isActive());
  }

  @Test
  void salesOrganisationGivesTheUnitAndCostCenterOfAnOfficer() {
    Long company = data.company().getId();
    var ao = sales.assignmentOf(company, "ao").orElseThrow();
    assertThat(ao.team()).isEqualTo("T-CBG1");
    assertThat(ao.department()).isEqualTo("CBG-NCR");
    assertThat(ao.region()).isEqualTo("NCR");
    assertThat(ao.costCenter()).isEqualTo("NB-CBG-M");
    assertThat(sales.assignmentOf(company, "proc")).isEmpty();

    String team = unique("T");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        sales.createUnit(
                            company, SalesLevel.TEAM, team, new UnitDetails("x", "NCR", null))))
        .extracting("code")
        .isEqualTo("SALES_PARENT_INVALID");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        sales.createUnit(
                            company,
                            SalesLevel.TEAM,
                            team,
                            new UnitDetails("x", "CBG-NCR", "NOPE"))))
        .extracting("code")
        .isEqualTo("INVALID_DIMENSION");
    var unit =
        as.run(
            "badmin",
            () ->
                sales.createUnit(
                    company, SalesLevel.TEAM, team, new UnitDetails("Team x", "CBG-NCR", "MKT")));
    as.run(
        "badmin",
        () -> sales.updateUnit(unit.getId(), new UnitDetails("Team y", "CBG-NCR", "MKT")));
    assertThatThrownBy(
            () -> as.run("badmin", () -> sales.assignOfficer(company, team, "nobody-" + team)))
        .extracting("code")
        .isEqualTo("USER_UNKNOWN");
    var officer = as.run("badmin", () -> sales.assignOfficer(company, team, "epol"));
    assertThat(officer.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThat(sales.units(company)).extracting("code").contains(team);
    assertThat(sales.officers(company)).extracting("username").contains("epol");
  }
}
