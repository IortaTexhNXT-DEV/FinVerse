package com.iortatechnxt.brokerverse.tax;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.catalog.domain.ChargeBasis;
import com.iortatechnxt.brokerverse.catalog.domain.ChargeVatTreatment;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge.Scope;
import com.iortatechnxt.brokerverse.catalog.domain.OtherCharge.Terms;
import com.iortatechnxt.brokerverse.catalog.domain.RateCode;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.catalog.service.OtherChargeService;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;
import com.iortatechnxt.brokerverse.catalog.service.RateResolver;
import com.iortatechnxt.brokerverse.catalog.service.RatingQuery;
import com.iortatechnxt.brokerverse.catalog.service.RatingService;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.support.TestParties;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The tax data BDOI is asked for in the inputs v1.1 (TX-Q02, TX-Q04, TX-Q06, TX-Q08, TX-Q09): party
 * withholding status and exemption certificate, final tax types, the company's tax registration,
 * the insurer's VAT registration in the premium calculator and the other charges billed with the
 * premium behind their switch.
 */
@IntegrationTest
class TaxStatusAndChargesIT {

  private static final String PROFILES = "/api/v1/tax/profiles";
  private static final String INSURER = "INS-MGIC";

  @Autowired private Api api;
  @Autowired private TaxFixtures fixtures;
  @Autowired private TestParties parties;
  @Autowired private TestData data;
  @Autowired private RatingService rating;
  @Autowired private RateResolver resolver;
  @Autowired private OtherChargeService charges;
  @Autowired private CatalogRecords records;
  @Autowired private SystemParameterService parameters;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private Map<String, Object> profile(String party) {
    return Json.of(
        "companyId",
        fixtures.companyId(),
        "partyCode",
        party,
        "tin",
        "123-456-789",
        "payeeClass",
        "CORPORATE",
        "registeredName",
        "Government Office " + party,
        "vatTreatment",
        "EXEMPT",
        "governmentPayor",
        true,
        "exemptionCertificateNo",
        "TEC-2027-01");
  }

  @Test
  void aPartyProfileKeepsTheWithholdingStatusAndTheExemptionCertificate() throws Exception {
    String party = parties.create(PartyType.CORPORATE_CLIENT).getCode();
    api.doPost("accountant", PROFILES, profile(party))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("EXEMPTION_VALIDITY_REQUIRED"));
    Map<String, Object> request = profile(party);
    request.put("exemptionValidFrom", "2027-01-01");
    request.put("exemptionValidTo", "2027-12-31");
    api.doPost("accountant", PROFILES, request)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.governmentPayor").value(true))
        .andExpect(jsonPath("$.withholdingAgent").value(true))
        .andExpect(jsonPath("$.topWithholdingAgent").value(false))
        .andExpect(jsonPath("$.exemptionCertificateNo").value("TEC-2027-01"))
        .andExpect(jsonPath("$.exemptionValidTo").value("2027-12-31"));
  }

  @Test
  void finalTaxesAndPercentageTaxAreTaxCodes() throws Exception {
    fixtures.masters();
    String suffix = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
    api.doPost(
            "accountant",
            "/api/v1/tax/codes",
            Json.of(
                "companyId", fixtures.companyId(),
                "code", "FVAT" + suffix,
                "name", "Final VAT withheld by government payors",
                "taxType", "FINAL_VAT",
                "rate", 5,
                "glAccountCode", "2508",
                "effectiveFrom", "2027-01-01"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.taxType").value("FINAL_VAT"));
    api.doPost(
            "accountant",
            "/api/v1/tax/codes",
            Json.of(
                "companyId", fixtures.companyId(),
                "code", "FWT" + suffix,
                "name", "Final tax on bank interest",
                "taxType", "FWT",
                "rate", 20,
                "glAccountCode", "2508",
                "effectiveFrom", "2027-01-01"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ATC_REQUIRED"));
  }

  @Test
  void theCompanyKeepsItsTaxRegistrationAndTheBranchesTheirBirCodes() throws Exception {
    String code = "R" + ThreadLocalRandom.current().nextInt(1000, 9999);
    Map<String, Object> company =
        Json.of(
            "code",
            code,
            "name",
            "Registered " + code,
            "baseCurrency",
            "PHP",
            "taxId",
            "123-456-789-000",
            "fiscalYearStartMonth",
            1,
            "backValueDays",
            30,
            "forwardValueDays",
            5,
            "taxRegistration",
            Json.of(
                "rdoCode", "050",
                "vatRegistered", true,
                "casPermitNo", "CAS-2027-0001",
                "casPermitDate", "2027-01-15",
                "einvoicingPermitNo", "EIS-0001"));
    long companyId =
        api.read(
                api.doPost("fmanager", "/api/v1/organization/companies", company)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.taxRegistration.rdoCode").value("050"))
                    .andExpect(jsonPath("$.taxRegistration.casPermitNo").value("CAS-2027-0001")))
            .get("id")
            .asLong();
    Map<String, Object> bad = Json.of("rdoCode", "5O");
    company.put("taxRegistration", bad);
    api.doPut("fmanager", "/api/v1/organization/companies/" + companyId, company)
        .andExpect(status().isBadRequest());
    api.doPost(
            "accountant",
            "/api/v1/organization/branches",
            Json.of(
                "companyId",
                companyId,
                "code",
                "B" + code,
                "name",
                "Branch " + code,
                "openingDate",
                "2027-01-01",
                "headOffice",
                true,
                "forexAuthorized",
                false,
                "birBranchCode",
                "00001",
                "rdoCode",
                "050"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.birBranchCode").value("00001"))
        .andExpect(jsonPath("$.rdoCode").value("050"));
  }

  @Test
  void theInsurerTaxStatusChoosesVatOrPremiumTax() {
    Long company = data.company().getId();
    LocalDate day = LocalDate.of(2026, 10, 1);
    BigDecimal premiumTaxRate =
        resolver
            .rate(RateCode.PREMIUM_TAX, "FIRE", day)
            .filter(r -> r.signum() > 0)
            .or(() -> resolver.rate(RateCode.PREMIUM_TAX, null, day))
            .orElse(BigDecimal.ZERO);
    try {
      setTaxStatus(company, "VAT_REGISTERED");
      PremiumBreakdown vat = fire(company, day);
      assertThat(vat.premiumTax()).isEqualByComparingTo("0.00");
      BigDecimal vatRate =
          resolver
              .rate(RateCode.VAT_PREMIUM, "FIRE", day)
              .filter(r -> r.signum() > 0)
              .or(() -> resolver.rate(RateCode.VAT_PREMIUM, null, day))
              .orElse(BigDecimal.ZERO);
      assertThat(vat.vat()).isEqualByComparingTo(percent(vat.netPremium(), vatRate));

      setTaxStatus(company, "NON_VAT");
      PremiumBreakdown nonVat = fire(company, day);
      assertThat(nonVat.vat()).isEqualByComparingTo("0.00");
      assertThat(nonVat.premiumTax()).isPositive();
      assertThat(nonVat.premiumTax())
          .isGreaterThanOrEqualTo(percent(nonVat.netPremium(), premiumTaxRate));
    } finally {
      setTaxStatus(company, null);
    }
  }

  @Test
  void otherChargesAreBilledOnlyWhenSwitchedOn() {
    Long company = data.company().getId();
    LocalDate day = LocalDate.of(2026, 10, 1);
    String code = "DOC" + ThreadLocalRandom.current().nextInt(1000, 9999);
    OtherCharge row =
        as.run(
            "badmin",
            () ->
                charges.create(
                    company,
                    code,
                    new Scope(null, "PAR01"),
                    new Terms(
                        "Documentation fee",
                        ChargeBasis.AMOUNT,
                        new BigDecimal("250"),
                        ChargeVatTreatment.EXEMPT,
                        "4100",
                        day.minusDays(1),
                        null)));
    as.run("approver", () -> records.authorize(CatalogKind.OTHER_CHARGE, row.getId()));
    PremiumBreakdown off = fire(company, day);
    assertThat(off.otherCharges()).isEmpty();
    try {
      as.run("badmin", () -> parameters.update(OtherChargeService.ENABLED, "true"));
      PremiumBreakdown on = fire(company, day);
      assertThat(on.otherCharges())
          .filteredOn(c -> c.code().equals(code))
          .singleElement()
          .satisfies(c -> assertThat(c.amount()).isEqualByComparingTo("250.00"));
      assertThat(on.grossPremium().subtract(off.grossPremium()))
          .isEqualByComparingTo(on.otherChargesTotal());
    } finally {
      as.run("badmin", () -> parameters.update(OtherChargeService.ENABLED, "false"));
    }
  }

  private PremiumBreakdown fire(Long company, LocalDate day) {
    return rating
        .rate(
            new RatingQuery(
                company,
                "PAR01",
                INSURER,
                "MKT",
                List.of(
                    new RatingQuery.Item("Building", new BigDecimal("1000000"), null, null, null)),
                false,
                PeriodBasis.ANNUAL,
                day,
                day.plusYears(1),
                null,
                false,
                null))
        .breakdown();
  }

  private void setTaxStatus(Long company, String status) {
    jdbc.update(
        "update cat_insurer set tax_status = ? where company_id = ? and party_code = ?",
        status,
        company,
        INSURER);
  }

  private static BigDecimal percent(BigDecimal amount, BigDecimal rate) {
    return amount.multiply(rate).movePointLeft(2).setScale(2, java.math.RoundingMode.HALF_UP);
  }
}
