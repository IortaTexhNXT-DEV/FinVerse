package com.iortatechnxt.brokerverse.api;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static com.iortatechnxt.brokerverse.support.CsrfRequests.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

/** Broking foundation endpoints through the full HTTP stack. */
@IntegrationTest
class BrokingFoundationApiIT {

  @Autowired private MockMvc mvc;
  @Autowired private TestData data;

  @ParameterizedTest
  @CsvSource({
    "ao, /api/v1/lov/RETURN_REASON/options",
    "ao, /api/v1/lov/DOCUMENT_TYPE/options?date=2026-09-01",
    "badmin, /api/v1/lov/types",
    "badmin, /api/v1/lov/MARKET_SEGMENT/values",
    "proc, /api/v1/workflow/queue?companyId={c}",
    "proc, /api/v1/workflow/queue?companyId={c}&workflow=NB_ACCOUNT&scope=MINE&overdue=true&text=x",
    "proc, /api/v1/workflow/counts?companyId={c}",
    "ao, /api/v1/workflow/definitions/NB_ACCOUNT/stages",
    "proc, /api/v1/bulk/handlers",
    "proc, /api/v1/bulk/handlers/TEST_VEHICLES",
    "proc, /api/v1/bulk/jobs?companyId={c}",
    "badmin, /api/v1/messages",
    "badmin, /api/v1/messages?status=FAILED&purpose=EPOLICY&text=a",
    "ao, /api/v1/messages/by-record?entityType=Account&entityId=1",
    "ao, /api/v1/notifications",
    "ao, /api/v1/notifications?unreadOnly=true",
    "ao, /api/v1/notifications/unread-count",
    "badmin, /api/v1/doc-templates",
    "badmin, /api/v1/doc-templates/QUOTATION_TERMS/versions/1/docx",
    "ao, /api/v1/doc-renditions/0000000000000000000000000000000000000000000000000000000000000000",
    "ao, /api/v1/organization/companies",
    "proc, /api/v1/organization/branches?companyId={c}",
  })
  void readEndpointsRespondOk(String user, String url) throws Exception {
    mvc.perform(
            get(url.replace("{c}", data.company().getId().toString()))
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user(loadUser(user))))
        .andExpect(status().isOk());
  }

  @Autowired private org.springframework.security.core.userdetails.UserDetailsService userDetails;

  private org.springframework.security.core.userdetails.UserDetails loadUser(String name) {
    return userDetails.loadUserByUsername(name);
  }

  @Test
  @WithUserDetails("ao")
  void permissionsAreEnforced() throws Exception {
    mvc.perform(get("/api/v1/lov/types")).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/messages")).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/workflow/cases/1/assignees")).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/workflow/cases/by-record?entityType=Nope&entityId=1"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("proc")
  void bulkUploadValidateAndCommitOverHttp() throws Exception {
    mvc.perform(get("/api/v1/bulk/handlers/TEST_VEHICLES/template"))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    "Content-Disposition",
                    org.hamcrest.Matchers.containsString("test_vehicles_template.xlsx")));
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "api.csv",
            "text/csv",
            "Plate No,Amount,Inception,Fleet,Remarks\nAPI 1,10,,,\nAPI 2,x,,,\n"
                .getBytes(StandardCharsets.UTF_8));
    String body =
        mvc.perform(
                multipart("/api/v1/bulk/jobs")
                    .file(file)
                    .param("companyId", data.company().getId().toString())
                    .param("handler", "TEST_VEHICLES")
                    .param("parameters", "{\"product\":\"MTR10\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.validRows").value(1))
            .andExpect(jsonPath("$.invalidRows").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer id = JsonPath.read(body, "$.id");
    mvc.perform(get("/api/v1/bulk/jobs/" + id + "/rows?status=INVALID"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].rowNo").value(3));
    mvc.perform(post("/api/v1/bulk/jobs/" + id + "/commit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.committedRows").value(1));
    mvc.perform(get("/api/v1/bulk/jobs/" + id + "/report")).andExpect(status().isOk());
    mvc.perform(get("/api/v1/bulk/jobs/" + id)).andExpect(jsonPath("$.status").value("COMPLETED"));
    mvc.perform(
            multipart("/api/v1/bulk/jobs")
                .file(file)
                .param("companyId", data.company().getId().toString())
                .param("handler", "TEST_VEHICLES")
                .param("parameters", "not json"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  @WithUserDetails("badmin")
  void lovMaintenanceOverHttp() throws Exception {
    String code = "API_" + (System.nanoTime() % 100000);
    String body =
        mvc.perform(
                post("/api/v1/lov/RETURN_REASON/values")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"code\":\""
                            + code
                            + "\",\"label\":\"Api reason\",\"sortOrder\":1,\"effectiveFrom\":\"2026-01-01\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING_AUTHORIZATION"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Integer id = JsonPath.read(body, "$.id");
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/lov/values/" + id)
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\""
                        + code
                        + "\",\"label\":\"Api reason 2\",\"sortOrder\":2,\"effectiveFrom\":\"2026-01-01\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.label").value("Api reason 2"));
    mvc.perform(post("/api/v1/lov/values/" + id + "/deactivate"))
        .andExpect(jsonPath("$.status").value("INACTIVE"));
    mvc.perform(post("/api/v1/lov/values/" + id + "/authorize")).andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/lov/RETURN_REASON/values")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"code\":\"bad code\",\"label\":\"x\",\"sortOrder\":1,\"effectiveFrom\":\"2026-01-01\"}"))
        .andExpect(status().isBadRequest());
  }
}
