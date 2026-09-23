package com.iortatechnxt.finverse.masters;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.finverse.support.Api;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.Json;
import com.iortatechnxt.finverse.support.TestData;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class CurrencyAndDimensionApiIT {

  @Autowired private Api api;
  @Autowired private TestData data;

  @Test
  void exchangeRatesCanBeMaintainedAndLookedUp() throws Exception {
    var rate =
        Json.of(
            "currencyCode",
            "SGD",
            "rateType",
            "BUDGET",
            "effectiveDate",
            "2026-01-01",
            "rate",
            42.5);
    api.doPost("accountant", "/api/v1/currencies/rates", rate)
        .andExpect(jsonPath("$.rate").value(42.5));
    rate.put("rate", 43.1);
    api.doPost("accountant", "/api/v1/currencies/rates", rate)
        .andExpect(jsonPath("$.rate").value(43.1));
    api.doGet(
            "auditor",
            "/api/v1/currencies/rates/effective?baseCurrency=PHP&currency=SGD&rateType=BUDGET&date=2026-05-01")
        .andExpect(status().isOk());
    api.doGet(
            "auditor",
            "/api/v1/currencies/rates/effective?baseCurrency=PHP&currency=PHP&date=2026-05-01")
        .andExpect(status().isOk());
    api.doGet(
            "auditor",
            "/api/v1/currencies/rates/effective?baseCurrency=PHP&currency=INR&rateType=AVERAGE&date=2026-05-01")
        .andExpect(jsonPath("$.code").value("RATE_NOT_FOUND"));
    rate.put("rate", -1);
    api.doPost("accountant", "/api/v1/currencies/rates", rate).andExpect(status().isBadRequest());
  }

  @Test
  void dimensionValuesCanBeCreatedAndDeactivated() throws Exception {
    String code = "CC" + ThreadLocalRandom.current().nextInt(100, 999);
    var body =
        Json.of(
            "companyId",
            data.company().getId(),
            "type",
            "COST_CENTER",
            "code",
            code,
            "name",
            "Test centre");
    long id =
        api.read(
                api.doPost("accountant", "/api/v1/dimensions", body)
                    .andExpect(status().isCreated()))
            .get("id")
            .asLong();
    api.doPost("accountant", "/api/v1/dimensions", body).andExpect(status().isConflict());
    api.doPost("accountant", "/api/v1/dimensions/" + id + "/deactivate", null)
        .andExpect(jsonPath("$.active").value(false));
    api.doPost("accountant", "/api/v1/dimensions/" + id + "/activate", null)
        .andExpect(jsonPath("$.active").value(true));
  }
}
