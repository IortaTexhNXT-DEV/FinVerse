package com.iortatechnxt.brokerverse.api;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static com.iortatechnxt.brokerverse.support.CsrfRequests.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import com.iortatechnxt.brokerverse.support.TestData;
import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Placement and issuance actions through the full HTTP stack. */
@IntegrationTest
class PlacementIssuanceActionsApiIT {

  private static final String JSON_EMPTY = "{}";

  @Autowired private MockMvc mvc;
  @Autowired private TestData data;
  @Autowired private UserDetailsService users;
  @Autowired private PlacementTestData fx;

  private ResultActions postJson(String username, String url, String body) throws Exception {
    return mvc.perform(
        post(url)
            .with(user(users.loadUserByUsername(username)))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private String company() {
    return data.company().getId().toString();
  }

  private static byte[] pdf(String text) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (Document doc = new Document()) {
      PdfWriter.getInstance(doc, out);
      doc.open();
      doc.add(new Paragraph(text));
    }
    return out.toByteArray();
  }

  @Test
  void gateBillingAndPlacementActions() throws Exception {
    String liability = fx.awaitingPayment(fx.liability()).getArn();
    postJson("proc", "/api/v1/placement/gate/" + liability + "/direct-payment", JSON_EMPTY)
        .andExpect(status().isUnprocessableEntity());
    postJson(
            "proc",
            "/api/v1/placement/gate/" + liability + "/client-confirmation",
            "{\"channel\":\"SIGNED_FORM\",\"remarks\":\"Signed\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.open").value(true))
        .andExpect(jsonPath("$.evidence[0].channel").value("SIGNED_FORM"));
    mvc.perform(
            post("/api/v1/placement/gate/sweep")
                .param("companyId", company())
                .with(user(users.loadUserByUsername("proc"))))
        .andExpect(status().isOk());

    String fire = fx.awaitingPayment(fx.fire()).getArn();
    String batch =
        postJson(
                "proc",
                "/api/v1/placement/billing/batches",
                "{\"companyId\":" + company() + ",\"arns\":[\"" + fire + "\"]}")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].arn").value(fire))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer batchId = JsonPath.read(batch, "$.id");
    String report =
        mvc.perform(
                multipart("/api/v1/placement/billing/reports")
                    .file(
                        new MockMultipartFile(
                            "file",
                            "clpc.csv",
                            "text/csv",
                            "PN No.\nPN-UNKNOWN\n".getBytes(StandardCharsets.UTF_8)))
                    .param("companyId", company())
                    .param("kind", "CLPC")
                    .param("batchId", String.valueOf(batchId))
                    .with(user(users.loadUserByUsername("proc"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.unmatched").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer reportId = JsonPath.read(report, "$.id");
    Integer lineId = JsonPath.read(report, "$.lines[0].id");
    postJson(
            "proc",
            "/api/v1/placement/billing/reports/" + reportId + "/lines/" + lineId + "/match",
            "{\"arn\":\"" + fire + "\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matched").value(1));
    postJson("proc", "/api/v1/placement/billing/reports/" + reportId + "/discard", JSON_EMPTY)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISCARDED"));

    String slips =
        postJson(
                "proc",
                "/api/v1/placement/slips/generate",
                "{\"companyId\":" + company() + ",\"arns\":[\"" + liability + "\"]}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer slipId = JsonPath.read(slips, "$[0].id");
    postJson(
            "proc",
            "/api/v1/placement/slips/" + slipId + "/send",
            "{\"to\":[\"uw@insurer.example\"],\"subject\":\"Placement\",\"body\":\"Please place\",\"protect\":false}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SENT"));
    postJson(
            "proc",
            "/api/v1/placement/accounts/" + liability + "/hold-cover",
            "{\"startDate\":\"2026-10-01\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REQUESTED"));
    postJson(
            "ao",
            "/api/v1/placement/accounts/" + liability + "/hold-cover/confirm",
            "{\"reference\":\"HC-API\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
    postJson(
            "proc",
            "/api/v1/placement/accounts/" + liability + "/hold-cover/decline",
            "{\"comment\":\"x\"}")
        .andExpect(status().isOk());
    postJson(
            "proc",
            "/api/v1/placement/accounts/" + liability + "/insurer-return",
            "{\"reasonCode\":\"INSURER_REQUIREMENTS\"}")
        .andExpect(status().isOk());
    postJson(
            "proc",
            "/api/v1/placement/accounts/" + liability + "/resubmit",
            "{\"comment\":\"fixed\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("READY_FOR_PLACEMENT"));
    postJson("proc", "/api/v1/placement/slips/" + slipId + "/regenerate", JSON_EMPTY)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.versionNo").value(2));
    postJson(
            "proc",
            "/api/v1/placement/accounts/cancel",
            "{\"arns\":[\"" + liability + "\"],\"reasonCode\":\"CLIENT_REQUEST\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ok").value(true));
    postJson("ao", "/api/v1/placement/accounts/reactivate", "{\"arns\":[\"" + liability + "\"]}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ok").value(true));
  }

  @Test
  void issuanceActions() throws Exception {
    String arn = fx.placed(fx.fire());
    String received =
        mvc.perform(
                multipart("/api/v1/issuance/epolicies")
                    .file(
                        new MockMultipartFile(
                            "file", "policy.pdf", "application/pdf", pdf("Policy No: API-" + arn)))
                    .param("companyId", company())
                    .param("arn", arn)
                    .with(user(users.loadUserByUsername("proc"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("REVIEW"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer epolicyId = JsonPath.read(received, "$.id");
    postJson("proc", "/api/v1/issuance/epolicies/" + epolicyId + "/extract", JSON_EMPTY)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.differences").isArray());
    postJson(
            "proc",
            "/api/v1/issuance/epolicies/" + epolicyId + "/confirm",
            "{\"policyNumbers\":[\"API-" + arn + "\"],\"issueDate\":\"2026-09-30\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.epolicy.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.account.status").value("POLICY_ISSUED"));

    String uploaded =
        mvc.perform(
                multipart("/api/v1/issuance/epolicy-uploads")
                    .file(
                        new MockMultipartFile(
                            "files", arn + ".pdf", "application/pdf", pdf("copy")))
                    .param("companyId", company())
                    .with(user(users.loadUserByUsername("proc"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].arn").value(arn))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer uploadId = JsonPath.read(uploaded, "$.id");
    Integer itemId = JsonPath.read(uploaded, "$.items[0].id");
    postJson(
            "proc",
            "/api/v1/issuance/epolicy-uploads/" + uploadId + "/items/" + itemId,
            "{\"included\":true}")
        .andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/issuance/epolicy-uploads/" + uploadId)
                .with(user(users.loadUserByUsername("proc"))))
        .andExpect(status().isOk());
    postJson("proc", "/api/v1/issuance/epolicy-uploads/" + uploadId + "/confirm", JSON_EMPTY)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
    String second =
        JsonPath.read(
                mvc.perform(
                        get("/api/v1/issuance/policies/" + arn)
                            .with(user(users.loadUserByUsername("proc"))))
                    .andReturn()
                    .getResponse()
                    .getContentAsString(),
                "$.epolicies[0].id")
            .toString();
    postJson(
            "proc",
            "/api/v1/issuance/epolicies/" + second + "/reject",
            "{\"reasonCode\":\"WRONG_DOCUMENT\",\"remarks\":\"duplicate\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"));
    String another =
        mvc.perform(
                multipart("/api/v1/issuance/epolicy-uploads")
                    .file(new MockMultipartFile("files", "x.pdf", "application/pdf", pdf("none")))
                    .param("companyId", company())
                    .with(user(users.loadUserByUsername("proc"))))
            .andReturn()
            .getResponse()
            .getContentAsString();
    postJson(
            "proc",
            "/api/v1/issuance/epolicy-uploads/" + JsonPath.read(another, "$.id") + "/discard",
            JSON_EMPTY)
        .andExpect(status().isOk());

    postJson("epol", "/api/v1/issuance/dispatch/batch", "{\"epolicyIds\":[" + epolicyId + "]}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ok").value(true));
    postJson(
            "epol",
            "/api/v1/issuance/dispatch/" + epolicyId,
            "{\"to\":[\"client@example.ph\"],\"subject\":\"Your e-policy\",\"body\":\"Attached\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.dispatchCount").value(2));

    String advices =
        mvc.perform(
                get("/api/v1/issuance/policies/" + arn).with(user(users.loadUserByUsername("ao"))))
            .andExpect(jsonPath("$.advices.length()").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer adviceId = JsonPath.read(advices, "$.advices[0].id");
    postJson(
            "ao",
            "/api/v1/issuance/insurance-advice/send",
            "{\"adviceIds\":[" + adviceId + "],\"to\":[\"hl@bdo.example\"]}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("SENT"));
    postJson("proc", "/api/v1/issuance/insurance-advice/generate", "{\"arns\":[\"" + arn + "\"]}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ok").value(true));
  }
}
