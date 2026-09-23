package com.iortatechnxt.finverse.fixedasset;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.finverse.support.Api;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.Json;
import com.iortatechnxt.finverse.support.TestData;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Fixed asset and investment endpoints through the full HTTP stack; every test rolls back. */
@IntegrationTest
@Transactional
class AssetInvestmentApiIT {

  private static final String ASSETS = "/api/v1/assets/register";
  private static final String HOLDINGS = "/api/v1/investments/holdings";
  private static final String ACCOUNTANT = "accountant";
  private static final String CHECKER = "checker";
  private static final String MANAGER = "fmanager";

  @Autowired private Api api;
  @Autowired private TestData data;

  private Map<String, Object> category(String name) {
    return Json.of(
        "companyId",
        data.company().getId(),
        "code",
        "API-CAT",
        "name",
        name,
        "assetAccount",
        "1701",
        "accumulatedDepreciationAccount",
        "1709",
        "depreciationExpenseAccount",
        "5611",
        "depreciationMethod",
        "STRAIGHT_LINE",
        "usefulLifeMonths",
        12,
        "residualPercent",
        0);
  }

  private Map<String, Object> asset(long categoryId, String costCenter) {
    return Json.of(
        "companyId",
        data.company().getId(),
        "branchId",
        data.branch("HO").getId(),
        "categoryId",
        categoryId,
        "tagNo",
        "API-001",
        "description",
        "API asset",
        "costCenter",
        costCenter,
        "acquisitionDate",
        "2026-08-03",
        "acquisitionCost",
        12000,
        "settlementAccount",
        "1111",
        "location",
        "HO",
        "takeOn",
        false);
  }

  @Test
  void assetCategoryAssetAndDepreciationEndpoints() throws Exception {
    long companyId = data.company().getId();
    long categoryId =
        api.read(
                api.doPost(ACCOUNTANT, "/api/v1/assets/categories", category("API category"))
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPut(ACCOUNTANT, "/api/v1/assets/categories/" + categoryId, category("Renamed"))
        .andExpect(jsonPath("$.name").value("Renamed"));
    api.doPost(CHECKER, "/api/v1/assets/categories/" + categoryId + "/authorize", null)
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    api.doGet(ACCOUNTANT, "/api/v1/assets/categories?companyId=" + companyId)
        .andExpect(status().isOk());

    api.doPost(ACCOUNTANT, ASSETS, asset(categoryId, ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.costCenter").exists());
    long assetId =
        api.read(
                api.doPost(ACCOUNTANT, ASSETS, asset(categoryId, "FIN"))
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPut(ACCOUNTANT, ASSETS + "/" + assetId, asset(categoryId, "IT"))
        .andExpect(jsonPath("$.costCenter").value("IT"));
    api.doPost(CHECKER, ASSETS + "/" + assetId + "/capitalize", null)
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.netBookValue").value(12000.0));
    api.doGet(ACCOUNTANT, ASSETS + "/" + assetId).andExpect(jsonPath("$.tagNo").value("API-001"));

    String preview =
        "/api/v1/assets/depreciation/preview?companyId=" + companyId + "&period=2026-08";
    api.doGet(ACCOUNTANT, preview)
        .andExpect(jsonPath("$.posted").value(false))
        .andExpect(jsonPath("$.total").value(1000.0));
    api.doPost(
            ACCOUNTANT,
            "/api/v1/assets/depreciation/runs?companyId=" + companyId + "&period=2026-08",
            null)
        .andExpect(status().isForbidden());
    long runId =
        api.read(
                api.doPost(
                        MANAGER,
                        "/api/v1/assets/depreciation/runs?companyId="
                            + companyId
                            + "&period=2026-08",
                        null)
                    .andExpect(jsonPath("$.totalDepreciation").value(1000.0)))
            .get("id")
            .asLong();
    api.doGet(ACCOUNTANT, preview).andExpect(jsonPath("$.posted").value(true));
    api.doGet(ACCOUNTANT, "/api/v1/assets/depreciation/runs/" + runId + "/lines")
        .andExpect(jsonPath("$[0].amount").value(1000.0));

    api.doPost(
            ACCOUNTANT,
            ASSETS + "/" + assetId + "/transfer",
            Json.of("toBranchId", data.branch("CEB").getId(), "transferDate", "2026-09-01"))
        .andExpect(jsonPath("$.movementType").value("TRANSFER"));
    api.doPost(
            ACCOUNTANT,
            ASSETS + "/" + assetId + "/dispose",
            Json.of("disposalDate", "2026-09-15", "proceeds", 12000, "bankAccount", "1111"))
        .andExpect(jsonPath("$.gainLoss").value(1000.0));
    api.doGet(ACCOUNTANT, ASSETS + "/" + assetId + "/movements")
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  void portfolioHoldingAndRunEndpoints() throws Exception {
    long companyId = data.company().getId();
    Map<String, Object> portfolio =
        Json.of(
            "companyId",
            companyId,
            "code",
            "API-PF",
            "name",
            "API portfolio",
            "classification",
            "FVOCI",
            "investmentAccount",
            "1502",
            "accruedInterestAccount",
            "1504",
            "interestIncomeAccount",
            "4501",
            "realizedGainAccount",
            "4503",
            "fairValueAccount",
            "3400");
    long portfolioId =
        api.read(
                api.doPost(ACCOUNTANT, "/api/v1/investments/portfolios", portfolio)
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPut(ACCOUNTANT, "/api/v1/investments/portfolios/" + portfolioId, portfolio)
        .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"));
    api.doPost(CHECKER, "/api/v1/investments/portfolios/" + portfolioId + "/authorize", null)
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));

    Map<String, Object> holding =
        Json.of(
            "companyId",
            companyId,
            "branchId",
            data.branch("HO").getId(),
            "portfolioId",
            portfolioId,
            "instrumentType",
            "CORPORATE_BOND",
            "securityCode",
            "API-BOND",
            "description",
            "API bond",
            "issuerCode",
            "IS-ALI",
            "currency",
            "PHP",
            "faceValue",
            1000000,
            "purchasePrice",
            1000000,
            "tradeDate",
            "2026-06-30",
            "settlementDate",
            "2026-06-30",
            "maturityDate",
            "2029-06-30",
            "couponRate",
            6,
            "couponFrequency",
            "QUARTERLY",
            "dayCount",
            "THIRTY_360",
            "securityDeposit",
            false,
            "bankAccount",
            "1111",
            "takeOn",
            false);
    long holdingId =
        api.read(api.doPost(ACCOUNTANT, HOLDINGS, holding).andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPut(ACCOUNTANT, HOLDINGS + "/" + holdingId, holding)
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    api.doPost(CHECKER, HOLDINGS + "/" + holdingId + "/approve", null)
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.classification").value("FVOCI"));

    String preview =
        "/api/v1/investments/runs/preview?companyId=" + companyId + "&type=ACCRUAL&period=2026-07";
    api.doGet(ACCOUNTANT, preview).andExpect(jsonPath("$.total").value(5000.0));
    long runId =
        api.read(
                api.doPost(
                        MANAGER,
                        "/api/v1/investments/runs?companyId="
                            + companyId
                            + "&type=ACCRUAL&period=2026-07",
                        null)
                    .andExpect(jsonPath("$.holdingCount").value(1)))
            .get("id")
            .asLong();
    api.doGet(ACCOUNTANT, preview).andExpect(jsonPath("$.posted").value(true));
    api.doGet(ACCOUNTANT, "/api/v1/investments/runs/" + runId + "/transactions")
        .andExpect(jsonPath("$[0].amount").value(5000.0));

    api.doPost(
            ACCOUNTANT,
            HOLDINGS + "/" + holdingId + "/fair-value",
            Json.of("valuationDate", "2026-08-31", "fairValue", 1010000))
        .andExpect(jsonPath("$.amount").value(10000.0));
    api.doPost(
            ACCOUNTANT,
            HOLDINGS + "/" + holdingId + "/maturity",
            Json.of("valueDate", "2026-09-01", "proceeds", 1, "finalTax", 0))
        .andExpect(jsonPath("$.code").value("NOT_MATURED"));
    api.doPost(
            ACCOUNTANT,
            HOLDINGS + "/" + holdingId + "/sale",
            Json.of("valueDate", "2026-09-01", "proceeds", 1015000, "finalTax", 0))
        .andExpect(jsonPath("$.txnType").value("SALE"));
    api.doGet(ACCOUNTANT, HOLDINGS + "/" + holdingId).andExpect(jsonPath("$.status").value("SOLD"));
    api.doGet(ACCOUNTANT, HOLDINGS + "/" + holdingId + "/transactions")
        .andExpect(jsonPath("$[0].txnType").value("PURCHASE"));
    api.doPost(
            ACCOUNTANT,
            HOLDINGS + "/" + holdingId + "/coupons",
            Json.of("receiptDate", "2026-10-01", "cashAmount", 12000, "finalTax", 3000))
        .andExpect(status().isUnprocessableEntity());
  }
}
