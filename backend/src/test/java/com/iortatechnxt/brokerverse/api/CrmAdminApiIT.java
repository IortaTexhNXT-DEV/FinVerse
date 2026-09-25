package com.iortatechnxt.brokerverse.api;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.RetentionRuleRepository;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/** CRM and broking administration endpoints through the full HTTP stack. */
@IntegrationTest
class CrmAdminApiIT {

  @Autowired private Api api;
  @Autowired private MockMvc mvc;
  @Autowired private UserDetailsService users;
  @Autowired private TestData data;
  @Autowired private ClientRepository clients;
  @Autowired private RetentionRuleRepository rules;

  private Client demo(String prospectCode) {
    return clients.findByCode(data.company().getId(), prospectCode).orElseThrow();
  }

  private String url(String template) {
    return template
        .replace("{c}", data.company().getId().toString())
        .replace("{client}", demo("PR-2026-000001").getId().toString())
        .replace("{rule}", rules.findAll().get(0).getId().toString());
  }

  @ParameterizedTest
  @CsvSource({
    "ao, /api/v1/crm/clients?companyId={c}",
    "ao, /api/v1/crm/clients?companyId={c}&status=CONFIRMED&kycStatus=VERIFIED&bankClient=true",
    "proc, /api/v1/crm/clients?companyId={c}&name=santos&code=cl-2026&clientType=INDIVIDUAL",
    "ao, /api/v1/crm/clients?companyId={c}&tin=201-555-101-000&idNumber=p5551010a&email=MARIA.santos@demo-client.ph",
    "ao, /api/v1/crm/clients?companyId={c}&mobile=09175550101&marketSegment=CBG&kycDue=true",
    "ao, /api/v1/crm/clients/{client}",
    "ao, /api/v1/crm/clients/{client}/summary",
    "ao, /api/v1/crm/clients/lookup?companyId={c}&q=santos",
    "ao, /api/v1/crm/clients/duplicates?companyId={c}&lastName=SANTOS&firstName=maria clara&birthDate=1984-03-12",
    "ao, /api/v1/crm/clients/{client}/kyc-checklist",
    "proc, /api/v1/crm/clients/{client}/instructions",
    "ao, /api/v1/crm/clients/{client}/notes",
    "ao, /api/v1/crm/clients/{client}/records",
    "ao, /api/v1/crm/clients/{client}/history",
    "ao, /api/v1/crm/kyc-reviews?companyId={c}",
    "ao, /api/v1/crm/kyc-reviews?companyId={c}&bank=ALL&riskRating=HIGH&marketSegment=RETAIL&dueBy=2030-12-31",
    "badmin, /api/v1/nbadmin/access-requests",
    "approver, /api/v1/nbadmin/access-requests?status=PENDING&type=CREATE_USER&text=u",
    "badmin, /api/v1/nbadmin/users",
    "badmin, /api/v1/nbadmin/roles",
    "badmin, /api/v1/nbadmin/access-matrix",
    "badmin, /api/v1/nbadmin/access-matrix/by-action",
    "approver, /api/v1/nbadmin/access-matrix/by-action?area=PACKAGE_REQUEST",
    "approver, /api/v1/nbadmin/access-requests?type=MODIFY_ROLE_PERMISSIONS&text=MBS",
    "approver, /api/v1/nbadmin/access-matrix/export",
    "badmin, /api/v1/nbadmin/retention/rules",
    "badmin, /api/v1/nbadmin/retention/rules/{rule}/eligible?limit=5",
    "badmin, /api/v1/system/session-policy",
  })
  void readEndpointsRespondOk(String user, String template) throws Exception {
    api.doGet(user, url(template)).andExpect(status().isOk());
  }

  @Test
  void demoDataShowsTheClientBannerDuplicatesAndOverdueKyc() throws Exception {
    api.doGet("proc", url("/api/v1/crm/clients/{client}/instructions"))
        .andExpect(jsonPath("$.tags[0].code").value("VIP"))
        .andExpect(jsonPath("$.instructions[0].type").value("COMMUNICATION"));
    api.doGet(
            "ao",
            url(
                "/api/v1/crm/clients/duplicates?companyId={c}&clientType=INDIVIDUAL&tin=201-555-101-000"))
        .andExpect(jsonPath("$[0].code").value("CL-2026-000001"))
        .andExpect(jsonPath("$[0].hard").value(true));
    api.doGet("ao", url("/api/v1/crm/kyc-reviews?companyId={c}"))
        .andExpect(jsonPath("$.content[*].code", Matchers.hasItem("CL-2026-000002")));
    api.doGet("ao", url("/api/v1/crm/clients/{client}"))
        .andExpect(jsonPath("$.clientCode").value("CL-2026-000001"))
        .andExpect(jsonPath("$.infoComplete").value(true))
        .andExpect(jsonPath("$.kyc.status").value("VERIFIED"));
  }

  @Test
  void clientIsCreatedValidatedAndOnboardedOverHttp() throws Exception {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    String tin =
        String.format(
            "8%02d-%03d-%03d-000", random.nextInt(100), random.nextInt(1000), random.nextInt(1000));
    Map<String, Object> body =
        Map.of(
            "companyId",
            data.company().getId(),
            "clientType",
            "CORPORATE",
            "corporateName",
            "Api Test " + System.nanoTime() + " Inc.",
            "tin",
            tin,
            "email",
            "api" + System.nanoTime() + "@corp.ph",
            "addressLine",
            "1 Api Road",
            "city",
            "Taguig",
            "marketSegment",
            "CORBANK");
    JsonNode created =
        api.read(api.doPost("ao", "/api/v1/crm/clients", body).andExpect(status().isCreated()));
    long id = created.get("id").asLong();
    api.doPost(
            "ao",
            "/api/v1/crm/clients",
            Map.of("clientType", "CORPORATE", "corporateName", "X", "tin", "123"))
        .andExpect(status().isBadRequest());
    api.doPost("ao", "/api/v1/crm/clients", body)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CLIENT_DUPLICATE"));
    api.doPost("proc", "/api/v1/crm/clients", body).andExpect(status().isForbidden());

    for (String type :
        new String[] {"KYC_FORM", "SEC_CERTIFICATE", "GIS", "SECRETARY_CERTIFICATE"}) {
      mvc.perform(
              multipart("/api/v1/crm/clients/" + id + "/kyc-documents")
                  .file(
                      new MockMultipartFile(
                          "file",
                          type + ".pdf",
                          "application/pdf",
                          "%PDF-1.4\n%%EOF".getBytes(StandardCharsets.US_ASCII)))
                  .param("documentType", type)
                  .with(
                      org.springframework.security.test.web.servlet.request
                          .SecurityMockMvcRequestPostProcessors.user(
                          users.loadUserByUsername("ao"))))
          .andExpect(status().isOk());
    }
    api.doPost("ao", "/api/v1/crm/clients/" + id + "/submit-kyc", Map.of("comment", "all in"))
        .andExpect(jsonPath("$.onboardingStage").value("KYC_REVIEW"));
    api.doPost("ao", "/api/v1/crm/clients/" + id + "/verify-kyc", Map.of())
        .andExpect(status().isForbidden());
    api.doPost("mkttl", "/api/v1/crm/clients/" + id + "/verify-kyc", Map.of())
        .andExpect(jsonPath("$.kyc.status").value("VERIFIED"));
    api.doPost("ao", "/api/v1/crm/clients/" + id + "/confirm", Map.of())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.clientCode", Matchers.startsWith("CL-")));
    api.doPost("ao", "/api/v1/crm/clients/" + id + "/tags", Map.of("tagCode", "VIP"))
        .andExpect(status().isCreated());
    api.doPost("ao", "/api/v1/crm/clients/" + id + "/tags/VIP/remove", Map.of())
        .andExpect(status().isOk());
    JsonNode instruction =
        api.read(
            api.doPost(
                    "ao",
                    "/api/v1/crm/clients/" + id + "/instructions",
                    Map.of(
                        "type",
                        "BILLING",
                        "text",
                        "Monthly billing",
                        "effectiveFrom",
                        "2026-01-01"))
                .andExpect(status().isCreated()));
    api.doPut(
            "ao",
            "/api/v1/crm/clients/" + id + "/instructions/" + instruction.get("id").asLong(),
            Map.of("type", "BILLING", "text", "Quarterly billing", "effectiveFrom", "2026-01-01"))
        .andExpect(jsonPath("$.text").value("Quarterly billing"));
    api.doPost(
            "ao",
            "/api/v1/crm/clients/"
                + id
                + "/instructions/"
                + instruction.get("id").asLong()
                + "/end",
            Map.of())
        .andExpect(jsonPath("$.active").value(false));
    api.doPost(
            "ao", "/api/v1/crm/clients/" + id + "/deactivate", Map.of("reasonCode", "NO_BUSINESS"))
        .andExpect(jsonPath("$.status").value("INACTIVE"));
    api.doPut("ao", "/api/v1/crm/clients/" + id, body)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CLIENT_INACTIVE"));
  }

  @Test
  void kycReportRunsAndExports() throws Exception {
    Map<String, String> params =
        Map.of("companyId", data.company().getId().toString(), "bankClients", "ALL");
    api.doPost("ao", "/api/v1/reports/NB-KYC-DUE/run", params).andExpect(status().isOk());
    for (String format : new String[] {"PDF", "XLSX", "CSV"}) {
      api.doPost("ao", "/api/v1/reports/NB-KYC-DUE/export?format=" + format, params)
          .andExpect(status().isOk())
          .andExpect(header().exists("Content-Disposition"));
    }
  }

  @Test
  void accessRequestsAndRetentionOverHttp() throws Exception {
    String name = String.format("a%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
    JsonNode submitted =
        api.read(
            api.doPost(
                    "badmin",
                    "/api/v1/nbadmin/access-requests",
                    Map.of(
                        "type", "CREATE_USER",
                        "username", name,
                        "fullName", "Api User",
                        "roleCodes", new String[] {"TSU"},
                        "justification", "New TSU analyst"))
                .andExpect(status().isCreated()));
    long id = submitted.get("id").asLong();
    api.doGet("approver", "/api/v1/nbadmin/access-requests/" + id)
        .andExpect(jsonPath("$.status").value("PENDING"));
    api.doPost("badmin", "/api/v1/nbadmin/access-requests/" + id + "/approve", Map.of())
        .andExpect(status().isForbidden());
    api.doPost(
            "approver",
            "/api/v1/nbadmin/access-requests/" + id + "/approve",
            Map.of("comment", "ok"))
        .andExpect(jsonPath("$.request.status").value("APPROVED"))
        .andExpect(jsonPath("$.temporaryPassword").isString());
    api.doPost("badmin", "/api/v1/nbadmin/access-requests", Map.of("type", "CREATE_USER"))
        .andExpect(status().isUnprocessableEntity());
    api.doGet("ao", "/api/v1/nbadmin/access-requests").andExpect(status().isForbidden());

    String rule = rules.findAll().get(0).getId().toString();
    api.doGet("ao", "/api/v1/nbadmin/retention/rules").andExpect(status().isForbidden());
    api.doPost("badmin", "/api/v1/nbadmin/retention/review", Map.of()).andExpect(status().isOk());
    api.doPut(
            "badmin",
            "/api/v1/nbadmin/retention/rules/" + rule,
            Map.of(
                "statuses",
                "",
                "yearsOnline",
                5,
                "yearsArchive",
                15,
                "action",
                "ARCHIVE",
                "active",
                true,
                "description",
                "x"))
        .andExpect(status().isBadRequest());
  }
}
