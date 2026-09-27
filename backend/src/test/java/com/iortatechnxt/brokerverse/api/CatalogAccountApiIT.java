package com.iortatechnxt.brokerverse.api;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static com.iortatechnxt.brokerverse.support.CsrfRequests.post;
import static com.iortatechnxt.brokerverse.support.CsrfRequests.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

/** Catalog, account and document endpoints through the full HTTP stack. */
@IntegrationTest
class CatalogAccountApiIT {

  @Autowired private MockMvc mvc;
  @Autowired private TestData data;
  @Autowired private UserDetailsService users;
  @Autowired private JdbcTemplate jdbc;

  private String company() {
    return data.company().getId().toString();
  }

  private Long seedAccountId(String arn) {
    return jdbc.queryForObject("select id from acc_account where arn = ?", Long.class, arn);
  }

  @ParameterizedTest
  @CsvSource({
    "ao, /api/v1/catalog/lines",
    "ao, /api/v1/catalog/cover-types",
    "ao, /api/v1/catalog/products",
    "ao, /api/v1/catalog/products?line=MOTOR&packaged=true&segment=CBG&q=mtr&activeOnly=true",
    "ao, /api/v1/catalog/products/MTR10",
    "badmin, /api/v1/catalog/field-rules",
    "badmin, /api/v1/catalog/document-rules",
    "tsu, /api/v1/catalog/tsu-rules",
    "ao, /api/v1/catalog/insurers?companyId={c}",
    "badmin, /api/v1/catalog/rates/taxes",
    "badmin, /api/v1/catalog/rates/short-period",
    "badmin, /api/v1/catalog/rates/motor-limits",
    "badmin, /api/v1/catalog/sales-organisation?companyId={c}",
    "ao, /api/v1/catalog/sales-organisation/assignment?companyId={c}&username=ao",
    "ao, /api/v1/accounts?companyId={c}",
    "ao, /api/v1/accounts?companyId={c}&mine=true&status=DRAFT&status=RETURNED_TO_MARKETING",
    "proc, /api/v1/accounts?companyId={c}&text=arn&pn=PN&vehicle=GAC&location=cebu&product=MTR10&line=MOTOR"
        + "&insurer=INS-MGIC&ffy=false&directPayment=false&periodFrom=2026-01-01&periodTo=2027-12-31"
        + "&officer=ao&includeVoided=true&page=0&size=5",
    "ao, /api/v1/accounts/by-arn/ARN-2026-900002",
    "ao, /api/v1/attachments/policy",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    mvc.perform(get(url.replace("{c}", company())).with(user(users.loadUserByUsername(username))))
        .andExpect(status().isOk());
  }

  @Test
  @WithUserDetails("ao")
  void accountDetailCheckAndInsurerDetail() throws Exception {
    Long id = seedAccountId("ARN-2026-900003");
    mvc.perform(get("/api/v1/accounts/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.arn").value("ARN-2026-900003"))
        .andExpect(jsonPath("$.items[0].location.insuredItems.length()").value(2))
        .andExpect(jsonPath("$.pnNumbers.length()").value(2))
        .andExpect(jsonPath("$.lifecycle.paymentStatus").value("UNPAID"));
    mvc.perform(get("/api/v1/accounts/" + id + "/check"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.premiumRated").value(true));
    String insurers =
        mvc.perform(get("/api/v1/catalog/insurers?companyId=" + company()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer insurerId = JsonPath.read(insurers, "$[0].id");
    mvc.perform(get("/api/v1/catalog/insurers/" + insurerId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.branches").isArray());
  }

  @Test
  @WithUserDetails("ao")
  void ratingQuoteAndAccountCreationOverHttp() throws Exception {
    mvc.perform(
            post("/api/v1/catalog/rating/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"companyId\":"
                        + company()
                        + ",\"productCode\":\"PAR01\",\"insurerCode\":\"INS-MGIC\",\"branchCode\":\"MKT\","
                        + "\"items\":[{\"label\":\"Building\",\"sumInsured\":1000000}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.breakdown.grossPremium").value(3181.25))
        .andExpect(jsonPath("$.rates.lgt").value(0.75));
    mvc.perform(
            post("/api/v1/catalog/rating/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"companyId\":" + company() + ",\"productCode\":\"PAR01\",\"items\":[]}"))
        .andExpect(status().isBadRequest());
    String reduction =
        "{\"companyId\":"
            + company()
            + ",\"productCode\":\"PAR01\",\"basis\":\"PRO_RATA\",\"periodFrom\":\"2026-07-01\","
            + "\"periodTo\":\"2027-01-01\",\"items\":[{\"sumInsured\":-200000,\"ratePercent\":0.25}]";
    mvc.perform(
            post("/api/v1/catalog/rating/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reduction + ",\"endorsement\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.breakdown.netPremium").value(-252.05));
    mvc.perform(
            post("/api/v1/catalog/rating/quote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reduction + "}"))
        .andExpect(status().is4xxClientError())
        .andExpect(jsonPath("$.code").value("RATING_SUM_INSURED_INVALID"));

    String token = Long.toString(System.nanoTime() % 1_000_000_000L, 36).toUpperCase();
    String clientId =
        jdbc.queryForObject(
                "select id from crm_client where client_code = 'CL-2026-900001'", Long.class)
            .toString();
    String body =
        "{\"companyId\":"
            + company()
            + ",\"clientId\":"
            + clientId
            + ",\"productCode\":\"MTR10\",\"marketSegment\":\"CBG\",\"termYears\":1,"
            + "\"periodFrom\":\"2026-10-01\",\"periodTo\":\"2027-10-01\",\"insurerCode\":\"INS-MGIC\","
            + "\"insurerBranch\":\"MKT\",\"pnNumbers\":[\"PN-"
            + token
            + "\"],\"items\":[{\"sumInsured\":900000,\"vehicle\":{\"plateNo\":\"A"
            + token
            + "\",\"engineNo\":\"B"
            + token
            + "\",\"chassisNo\":\"C"
            + token
            + "\",\"make\":\"Toyota\",\"model\":\"Wigo\",\"yearModel\":2025}}]}";
    String created =
        mvc.perform(post("/api/v1/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.arn").value(Matchers.startsWith("ARN-")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer id = JsonPath.read(created, "$.id");
    mvc.perform(put("/api/v1/accounts/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/accounts/" + id + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("MISSING_DOCUMENTS"));

    MockMultipartFile one =
        new MockMultipartFile(
            "files",
            "idf.pdf",
            "application/pdf",
            "%PDF-1.4 x".getBytes(StandardCharsets.US_ASCII));
    MockMultipartFile two =
        new MockMultipartFile(
            "files", "id.pdf", "application/pdf", "%PDF-1.4 y".getBytes(StandardCharsets.US_ASCII));
    String uploaded =
        mvc.perform(
                multipart("/api/v1/attachments/batch")
                    .file(one)
                    .file(two)
                    .param("entityType", "Account")
                    .param("entityId", id.toString())
                    .param("documentType", "IDF")
                    .param("naming", "NOMINATE")
                    .param("reference", "ARN-TEST"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$[1].fileName").value("ARN-TEST_IDF_2.pdf"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer fileId = JsonPath.read(uploaded, "$[0].id");
    Integer secondId = JsonPath.read(uploaded, "$[1].id");
    mvc.perform(
            post("/api/v1/attachments/" + fileId + "/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"records\":[{\"entityType\":\"Account\",\"entityId\":\""
                        + seedAccountId("ARN-2026-900008")
                        + "\"}]}"))
        .andExpect(status().isOk());
    mvc.perform(
            get(
                "/api/v1/attachments?entityType=Account&entityId="
                    + seedAccountId("ARN-2026-900008")))
        .andExpect(jsonPath("$[?(@.id == " + fileId + ")].linked").value(Matchers.contains(true)));
    mvc.perform(get("/api/v1/attachments/zip?ids=" + fileId + "," + secondId + "&name=docs"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Disposition", Matchers.containsString("docs.zip")));
    mvc.perform(
            post("/api/v1/accounts/" + id + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"ready\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SUBMITTED"));
  }

  @Test
  @WithUserDetails("ao")
  void fieldErrorsAndPermissionsAreReported() throws Exception {
    Long draft = seedAccountId("ARN-2026-900001");
    mvc.perform(
            post("/api/v1/accounts/" + draft + "/submit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(
            post("/api/v1/accounts/" + draft + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/catalog/field-rules")).andExpect(status().isOk());
    mvc.perform(
            post("/api/v1/catalog/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\"ZZ1\",\"name\":\"x\",\"lineCode\":\"OTHERS\",\"maxTermYears\":1,"
                        + "\"paymentGate\":\"PAID\",\"defaultCommissionRate\":1,\"minimumPremium\":0}"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/catalog/records/PRODUCT/1/authorize"))
        .andExpect(status().isForbidden());
    Long pa = seedAccountId("ARN-2026-900008");
    mvc.perform(
            put("/api/v1/accounts/" + pa + "/ffy")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"start\":\"2026-10-05\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("FFY_NOT_ELIGIBLE"));
  }

  @Test
  @WithUserDetails("badmin")
  void catalogMaintenanceOverHttp() throws Exception {
    String code = "TX" + Long.toString(System.nanoTime() % 1_000_000L, 36).toUpperCase();
    String product =
        "{\"code\":\""
            + code
            + "\",\"name\":\"Http product\",\"lineCode\":\"OTHERS\",\"coverTypeCode\":\"PV\","
            + "\"maxTermYears\":1,\"paymentGate\":\"CLIENT_CONFIRMATION\",\"defaultCommissionRate\":10,"
            + "\"minimumPremium\":0,\"marketSegments\":[\"CORBANK\"]}";
    String created =
        mvc.perform(
                post("/api/v1/catalog/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(product))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recordStatus").value("PENDING_AUTHORIZATION"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(
            put("/api/v1/catalog/products/" + code)
                .contentType(MediaType.APPLICATION_JSON)
                .content(product))
        .andExpect(status().isOk());
    Integer id = JsonPath.read(created, "$.id");
    mvc.perform(post("/api/v1/catalog/records/PRODUCT/" + id + "/deactivate"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("INACTIVE"));
    mvc.perform(
            post("/api/v1/catalog/rates/taxes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"rateCode\":\"DST\",\"lineCode\":\"OTHERS\",\"rate\":12.5,\"effectiveFrom\":\"2032-01-01\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/catalog/tsu-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\""
                        + code
                        + "\",\"description\":\"Http rule\",\"minLocations\":2,\"priority\":50}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/catalog/insurers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"companyId\":"
                        + company()
                        + ",\"partyCode\":\"INS-"
                        + code
                        + "\",\"name\":\"Http Insurance\",\"placementChannel\":\"EMAIL\",\"defaultCreditDays\":30,"
                        + "\"placementEmails\":[\"not-an-email\"]}"))
        .andExpect(status().isBadRequest());
  }
}
