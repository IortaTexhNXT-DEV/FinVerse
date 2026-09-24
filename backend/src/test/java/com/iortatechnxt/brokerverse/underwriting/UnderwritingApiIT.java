package com.iortatechnxt.brokerverse.underwriting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.ApprovalRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.CertificateRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.ConvertQuotationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.EndorsementRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.IterationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.OpenCoverRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.ProductRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.QuotationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.RiskRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.domain.SourceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@IntegrationTest
class UnderwritingApiIT {

  private static final String BASE = "/api/v1/underwriting";

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper json;
  @Autowired private UserDetailsService users;
  @Autowired private UwFixtures fx;

  /** Authenticated demo user with a CSRF token (session-style requests need one). */
  private RequestPostProcessor as(String username) {
    RequestPostProcessor principal = user(users.loadUserByUsername(username));
    RequestPostProcessor token = csrf();
    return request -> token.postProcessRequest(principal.postProcessRequest(request));
  }

  private JsonNode call(MockHttpServletRequestBuilder request, String username, Object body)
      throws Exception {
    request.with(as(username));
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
    }
    String response =
        mvc.perform(request)
            .andExpect(status().is2xxSuccessful())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(response);
  }

  private long id(JsonNode node) {
    return node.get("id").asLong();
  }

  @Test
  void productLifecycleThroughTheApi() throws Exception {
    ProductRequest request = fx.productRequest(UwFixtures.uniqueCode("A"), "PA", false);
    long id = id(call(post(BASE + "/products"), "uw", request));
    call(put(BASE + "/products/" + id), "uw", request);
    call(post(BASE + "/products/" + id + "/authorize"), "fmanager", null);
    mvc.perform(get(BASE + "/products/" + id).with(as("auditor")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.recordStatus").value("ACTIVE"));
    mvc.perform(
            post(BASE + "/products")
                .with(as("auditor"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void riskWithoutPositiveSumInsuredIsRejected() throws Exception {
    Product fire = fx.product("FIRE", false);
    PolicyRequest zero =
        fx.request(fire, SourceType.BROKER, "B-0001", List.of(fx.risk("0", "0", "NCR-1")));
    mvc.perform(
            post(BASE + "/policies")
                .with(as("uw"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(zero)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors['risks[0].sumInsured']").value("must be greater than 0"));
  }

  @Test
  void policyAndEndorsementWorkflowThroughTheApi() throws Exception {
    Product fire = fx.product("FIRE", false);
    PolicyRequest request = fx.brokerRequest(fire);
    call(post(BASE + "/policies/preview"), "uw", request);
    long policyId = id(call(post(BASE + "/policies"), "uw", request));
    call(put(BASE + "/policies/" + policyId), "uw", request);
    call(post(BASE + "/policies/" + policyId + "/submit"), "uw", null);
    call(post(BASE + "/policies/" + policyId + "/reject"), "fmanager", Map.of("reason", "Check"));
    call(post(BASE + "/policies/" + policyId + "/submit"), "uw", null);
    JsonNode approved =
        call(
            post(BASE + "/policies/" + policyId + "/approve"),
            "fmanager",
            new ApprovalRequest(LocalDate.of(2026, 3, 15)));
    assertThat(approved.at("/document/status").asText()).isEqualTo("APPROVED");
    mvc.perform(
            get(BASE + "/policies")
                .with(as("uw"))
                .param("companyId", fx.companyId().toString())
                .param("status", "APPROVED")
                .param("q", approved.get("policyNo").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].policyNo").value(approved.get("policyNo").asText()));

    EndorsementRequest additional =
        new EndorsementRequest(
            EndorsementType.ADDITIONAL,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 1),
            "More stock",
            new BigDecimal("5000"),
            new BigDecimal("500000"),
            null,
            null);
    long endtId =
        id(call(post(BASE + "/policies/" + policyId + "/endorsements"), "uw", additional));
    call(post(BASE + "/endorsements/" + endtId + "/submit"), "uw", null);
    call(post(BASE + "/endorsements/" + endtId + "/reject"), "fmanager", Map.of("reason", "No"));
    call(post(BASE + "/endorsements/" + endtId + "/submit"), "uw", null);
    call(post(BASE + "/endorsements/" + endtId + "/approve"), "fmanager", null);
    call(get(BASE + "/endorsements/" + endtId), "uw", null);
    EndorsementRequest nil =
        new EndorsementRequest(
            EndorsementType.NIL,
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 1),
            "Address change",
            null,
            null,
            null,
            null);
    long nilId = id(call(post(BASE + "/policies/" + policyId + "/endorsements"), "uw", nil));
    call(post(BASE + "/endorsements/" + nilId + "/discard"), "uw", null);
    mvc.perform(get(BASE + "/policies/" + policyId + "/endorsements").with(as("uw")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));

    long draftId = id(call(post(BASE + "/policies"), "uw", request));
    call(post(BASE + "/policies/" + draftId + "/discard"), "uw", null);
    call(get(BASE + "/policies/" + draftId), "uw", null);
  }

  @Test
  void quotationWorkflowThroughTheApi() throws Exception {
    Product motor = fx.product("MOTOR", false);
    QuotationRequest request =
        new QuotationRequest(
            fx.companyId(),
            fx.branchId(),
            motor.getId(),
            "C-000101",
            "Juan Dela Cruz",
            SourceType.AGENT,
            "A-0002",
            LocalDate.of(2026, 7, 1),
            30,
            LocalDate.of(2026, 7, 15),
            LocalDate.of(2027, 7, 14),
            "PHP",
            new BigDecimal("100"),
            new BigDecimal("12.5"),
            new IterationRequest(
                new BigDecimal("1500000"),
                new BigDecimal("45000"),
                new BigDecimal("2000"),
                null,
                null,
                null));
    long id = id(call(post(BASE + "/quotations"), "uw", request));
    call(put(BASE + "/quotations/" + id), "uw", request);
    call(post(BASE + "/quotations/" + id + "/iterations"), "uw", request.iteration());
    call(post(BASE + "/quotations/" + id + "/submit"), "uw", null);
    call(post(BASE + "/quotations/" + id + "/approve"), "fmanager", null);
    JsonNode policy =
        call(
            post(BASE + "/quotations/" + id + "/convert"),
            "uw",
            new ConvertQuotationRequest(LocalDate.of(2026, 7, 5), null, false, "Toyota Fortuner"));
    assertThat(policy.get("quotationId").asLong()).isEqualTo(id);

    long rejected = id(call(post(BASE + "/quotations"), "uw", request));
    call(post(BASE + "/quotations/" + rejected + "/submit"), "uw", null);
    call(post(BASE + "/quotations/" + rejected + "/reject"), "fmanager", Map.of("reason", "No"));
    call(get(BASE + "/quotations/" + rejected), "uw", null);
    mvc.perform(
            get(BASE + "/quotations")
                .with(as("uw"))
                .param("companyId", fx.companyId().toString())
                .param("status", "CONVERTED")
                .param("from", "2026-01-01")
                .param("to", "2026-12-31"))
        .andExpect(status().isOk());
    mvc.perform(
            post(BASE + "/quotations/expire")
                .with(as("fmanager"))
                .param("companyId", fx.companyId().toString())
                .param("asOf", "2026-01-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expired").isNumber());
    // The Quotations screen offers the run to makers (POLICY_MAINTAIN) as well.
    mvc.perform(
            post(BASE + "/quotations/expire")
                .with(as("uw"))
                .param("companyId", fx.companyId().toString())
                .param("asOf", "2026-01-01"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expired").value(0));
    mvc.perform(
            post(BASE + "/quotations/expire")
                .with(as("accountant"))
                .param("companyId", fx.companyId().toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void openCoverAndCertificatesThroughTheApi() throws Exception {
    Product marine = fx.product("MARINE", true);
    OpenCoverRequest request =
        new OpenCoverRequest(
            fx.companyId(),
            fx.branchId(),
            marine.getId(),
            "C-000203",
            "Mindanao Agri Ventures Inc.",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            "PHP",
            new BigDecimal("5000000"),
            new BigDecimal("50000000"),
            new BigDecimal("0.25"),
            "Fertilizer imports");
    long id = id(call(post(BASE + "/open-covers"), "uw", request));
    call(post(BASE + "/open-covers/" + id + "/authorize"), "fmanager", null);
    RiskRequest shipment =
        new RiskRequest(
            "Urea fertilizer 2,000 MT",
            new BigDecimal("3000000"),
            new BigDecimal("0.25"),
            null,
            null,
            null,
            "MV Davao Star",
            "Bintulu",
            "Davao",
            LocalDate.of(2026, 4, 2),
            "BL-1",
            LocalDate.of(2026, 4, 2),
            "LC-1",
            "Land Bank",
            "CIF + 10%");
    call(
        post(BASE + "/open-covers/" + id + "/certificates"),
        "uw",
        new CertificateRequest(LocalDate.of(2026, 4, 1), 45, SourceType.DIRECT, null, shipment));
    mvc.perform(get(BASE + "/open-covers/" + id + "/certificates").with(as("uw")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].risks[0].marine.vesselName").value("MV Davao Star"));
    call(get(BASE + "/open-covers/" + id), "uw", null);
    mvc.perform(
            get(BASE + "/open-covers").with(as("uw")).param("companyId", fx.companyId().toString()))
        .andExpect(status().isOk());
  }
}
