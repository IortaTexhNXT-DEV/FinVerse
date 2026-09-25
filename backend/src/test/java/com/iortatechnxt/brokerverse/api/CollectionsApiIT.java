package com.iortatechnxt.brokerverse.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.collections.CollectionsFixtures;
import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * HTTP contract of the Collections core (BRCLXN.001-029, 043-046, 051-057): every read endpoint
 * with its permission, and the collector's actions through the API.
 */
@IntegrationTest
class CollectionsApiIT {

  private static final String BASE = "/api/v1/collections";

  @Autowired private Api api;
  @Autowired private CollectionsFixtures fx;

  private String company() {
    return "?companyId=" + fx.company();
  }

  @AfterEach
  void drainOutbox() {
    fx.drainOutbox();
  }

  @Test
  void theWorklistAndTheAccountPageAreRead() throws Exception {
    CollectionItem item = fx.listedMotor();
    String no = item.getInvoiceNo();
    String account = BASE + "/items/" + no;

    api.doGet("clxhandler", BASE + "/worklist" + company() + "&mine=true&segment=CBG&size=5")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].currentHandler").value("clxhandler"));
    api.doGet("clxhandler", BASE + "/worklist" + company() + "&q=" + no + "&sort=netOutstanding")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].invoiceNo").value(no));
    api.doGet("clxtl", BASE + "/worklist/totals" + company() + "&groupBy=ARN")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].key").exists());
    api.doGet("uw", BASE + "/worklist" + company()).andExpect(status().isForbidden());

    api.doGet("clxhandler", account)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.item.invoiceNo").value(no))
        .andExpect(jsonPath("$.ledger.paymentStatus").value("UNPAID"))
        .andExpect(jsonPath("$.breakdown[0].component").exists())
        .andExpect(jsonPath("$.lock.mine").value(false));
    api.doGet("clxhandler", account + "/payments")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.booked").isNumber());
    api.doGet("clxhandler", account + "/policy")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.shares[0].insurerCode").value("INS-MGIC"))
        .andExpect(jsonPath("$.family[0].invoiceNo").value(no));
    api.doGet("clxhandler", account + "/assignments")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].kind").value("RULE"));
    api.doGet("clxhandler", account + "/history").andExpect(status().isOk());
    api.doGet("clxhandler", account + "/timeline")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].kind").exists());
    api.doGet("clxhandler", account + "/dispositions").andExpect(status().isOk());
    api.doGet("clxhandler", account + "/efforts").andExpect(status().isOk());
    api.doGet("clxhandler", account + "/handoffs").andExpect(status().isOk());
    api.doGet("clxhandler", BASE + "/items/NO-SUCH").andExpect(status().isNotFound());
    api.doGet("clxhandler", BASE + "/disposition-rules")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == 'FOR_CHECK_PICKUP')].opsAction").value("CHECK_PICKUP"));
    api.doGet("clxhandler", BASE + "/clients/" + BookingFixtures.CLIENT + company())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.clientCode").value(BookingFixtures.CLIENT))
        .andExpect(jsonPath("$.openOutstanding").isNumber());
    api.doGet("clxtl", BASE + "/handlers")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@ == 'clxhandler')]").exists());
    api.doGet("cashier", BASE + "/handlers").andExpect(status().isForbidden());
  }

  @Test
  void theCollectorWorksAnAccountThroughTheApi() throws Exception {
    String no = fx.listedMotor().getInvoiceNo();
    String account = BASE + "/items/" + no;

    api.doPost("clxhandler", account + "/lock", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.mine").value(true));
    api.doGet("clxtl", account).andExpect(jsonPath("$.lock.editingBy").value("clxhandler"));
    api.doPost(
            "clxhandler",
            BASE + "/efforts" + company(),
            Map.of("invoiceNos", List.of(no), "code", "EMAIL", "remarks", "SOA sent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].code").value("EMAIL"));
    api.doPost(
            "clxhandler",
            BASE + "/dispositions" + company(),
            Map.of(
                "invoiceNos",
                List.of(no),
                "code",
                "FOR_CHECK_PICKUP",
                "remarks",
                "Pick up Monday",
                "details",
                Map.of(
                    "pickupDate", LocalDate.now().plusDays(3).toString(),
                    "pickupAddress", "Makati",
                    "amount", "2500")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].opsAction").value("CHECK_PICKUP"))
        .andExpect(jsonPath("$[0].outboxId").isNumber());
    api.doPost(
            "clxtl",
            BASE + "/dispositions" + company(),
            Map.of("invoiceNos", List.of(no), "code", "COORDINATE_FURTHER"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CLX_ITEM_LOCKED"));
    api.doPut(
            "clxhandler",
            account + "/details" + company(),
            Map.of("remarks", "Pays on the 30th", "category", "A"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.category").value("A"));
    api.doDelete("clxhandler", account + "/lock")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.editingBy").doesNotExist());
    api.doGet("clxhandler", account + "/handoffs")
        .andExpect(jsonPath("$[0].feedCode").value("COLLECTION_CHECK_PICKUP"))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
    api.doPost("clxhandler", account + "/refresh", null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dispositionCode").value("FOR_CHECK_PICKUP"));
    api.doPost(
            "cashier",
            BASE + "/efforts" + company(),
            Map.of("invoiceNos", List.of(no), "code", "EMAIL"))
        .andExpect(status().isForbidden());
    api.doPost("clxhandler", BASE + "/dispositions" + company(), Map.of("invoiceNos", List.of()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void theTeamLeadAssignsAndReassigns() throws Exception {
    String no = fx.listedMotor().getInvoiceNo();
    api.doGet("clxtl", BASE + "/assignment-rules" + company())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].handler").exists());
    api.doGet("clxhandler", BASE + "/assignment-rules" + company())
        .andExpect(status().isForbidden());
    String rule =
        api.read(
                api.doPost(
                        "clxtl",
                        BASE + "/assignment-rules" + company(),
                        Map.of(
                            "priority",
                            7000,
                            "name",
                            "API rule " + BookingFixtures.token(),
                            "criteria",
                            Map.of("segment", "NO-SUCH-SEGMENT"),
                            "handler",
                            "mkthandler"))
                    .andExpect(status().isOk()))
            .get("id")
            .asText();
    api.doPut(
            "clxtl",
            BASE + "/assignment-rules/" + rule,
            Map.of("priority", 7001, "name", "API rule changed", "handler", "mkthandler"))
        .andExpect(jsonPath("$.priority").value(7001));
    api.doPost("clxtl", BASE + "/assignment-rules/" + rule + "/active", Map.of("active", false))
        .andExpect(jsonPath("$.active").value(false));

    Map<String, Object> selection =
        Map.of(
            "invoiceNos", List.of(no),
            "handler", "mkthandler",
            "kind", "PERMANENT",
            "reason", "Workload");
    api.doPost("clxtl", BASE + "/reassignments/preview" + company(), selection)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
    api.doPost(
            "clxtl",
            BASE + "/reassignments/preview" + company(),
            Map.of("criteria", Map.of("segment", "CBG", "handler", "clxhandler")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").isNumber());
    api.doPost("clxtl", BASE + "/reassignments" + company(), selection)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.moved").value(1));
    api.doGet("clxtl", BASE + "/items/" + no)
        .andExpect(jsonPath("$.item.currentHandler").value("mkthandler"));
  }

  @Test
  void homeFilesExportsAndSetup() throws Exception {
    fx.listedMotor();
    api.doGet("clxhandler", BASE + "/home" + company())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.tiles[?(@.key == 'mine')]").exists())
        .andExpect(jsonPath("$.aging").isArray());
    api.doGet("clxtl", BASE + "/home" + company())
        .andExpect(jsonPath("$.tiles[?(@.key == 'unassigned')]").exists());
    api.doGet("uw", BASE + "/home" + company()).andExpect(status().isForbidden());

    api.doPost("clxhandler", BASE + "/exports" + company(), Map.of("segment", "CBG"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.frequency").value("ON_REQUEST"))
        .andExpect(jsonPath("$.available").value(true));
    api.doPost(
            "clxuh",
            BASE + "/files/generate" + company(),
            Map.of("frequency", "DAILY", "date", LocalDate.now().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reportCode").value("CLX-OUTSTANDING-PR"));
    api.doPost(
            "clxhandler",
            BASE + "/files/generate" + company(),
            Map.of("frequency", "DAILY", "date", LocalDate.now().toString()))
        .andExpect(status().isForbidden());
    api.doGet("cashier", BASE + "/files" + company() + "&frequency=DAILY&frequency=ON_REQUEST")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].reportRunId").isNumber());
    api.doGet("uw", BASE + "/files" + company()).andExpect(status().isForbidden());

    api.doGet("clxuh", BASE + "/setup" + company())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parameters[?(@.key == 'CLX_MIN_BALANCE_THRESHOLD')]").exists())
        .andExpect(jsonPath("$.dispositions[?(@.code == 'DP_PR_FOR_REVERSAL')]").exists())
        .andExpect(jsonPath("$.units[?(@.code == 'T-CBG1')].headUsername").value("mkttl"));
    api.doGet("clxhandler", BASE + "/setup" + company()).andExpect(status().isForbidden());
    api.doPut(
            "clxuh",
            BASE + "/setup/parameters/CLX_EDIT_LOCK_MINUTES" + company(),
            Map.of("value", "15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("15"));
    api.doPut(
            "clxuh",
            BASE + "/setup/parameters/CLX_AGING_BRACKETS" + company(),
            Map.of("value", "0-30,31-X"))
        .andExpect(status().isUnprocessableEntity());
    api.doPut(
            "clxuh",
            BASE + "/setup/parameters/SESSION_TIMEOUT_MINUTES" + company(),
            Map.of("value", "30"))
        .andExpect(status().isUnprocessableEntity());
    api.doPut(
            "clxuh",
            BASE + "/setup/lov-attributes" + company(),
            Map.of(
                "typeCode", "CLX_PR_DISPOSITION",
                "code", "COORDINATE_FURTHER",
                "attribute", "category",
                "value", "B"))
        .andExpect(status().isOk());
    api.doPut(
            "clxuh",
            BASE + "/setup/lov-attributes" + company(),
            Map.of(
                "typeCode", "CLX_PR_DISPOSITION",
                "code", "COORDINATE_FURTHER",
                "attribute", "category",
                "value", ""))
        .andExpect(status().isOk());
    api.doPut(
            "clxuh",
            BASE + "/setup/lov-attributes" + company(),
            Map.of(
                "typeCode", "CLX_PR_DISPOSITION",
                "code", "COORDINATE_FURTHER",
                "attribute", "ops_action",
                "value", "TELEPORT"))
        .andExpect(status().isUnprocessableEntity());
    api.doPut("clxuh", BASE + "/setup/unit-heads/T-VIS1" + company(), Map.of("username", "clxuh"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headUsername").value("clxuh"));
    api.doPut("clxuh", BASE + "/setup/unit-heads/T-VIS1" + company(), Map.of("username", ""))
        .andExpect(status().isOk());
    api.doPost("clxuh", BASE + "/refresh" + company(), null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").exists());
  }
}
