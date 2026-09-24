package com.iortatechnxt.brokerverse.api;

import static com.iortatechnxt.brokerverse.support.CsrfRequests.multipart;
import static com.iortatechnxt.brokerverse.support.CsrfRequests.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

/** Placement and issuance endpoints through the full HTTP stack, on the V986 / V987 demo data. */
@IntegrationTest
class PlacementIssuanceApiIT {

  @Autowired private MockMvc mvc;
  @Autowired private TestData data;
  @Autowired private UserDetailsService users;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PlacementTestData fx;

  private String id(String sql, String key) {
    return String.valueOf(jdbc.queryForObject(sql, Long.class, key));
  }

  private String resolve(String url) {
    return url.replace("{c}", data.company().getId().toString())
        .replace(
            "{slip}",
            id("select id from plc_slip where slip_no = ? and version_no = 1", "PL-2026-900001"))
        .replace(
            "{batch}",
            id("select id from plc_billing_batch where batch_no = ?", "BILL-2026-900001"))
        .replace(
            "{report}",
            id("select id from plc_payment_report where report_no = ?", "PMT-2026-900001"))
        .replace(
            "{ia}", id("select id from iss_insurance_advice where ia_no = ?", "IA-2026-900001"))
        .replace("{epolicy}", id("select id from iss_epolicy where arn = ?", "ARN-2026-910004"));
  }

  @ParameterizedTest
  @CsvSource({
    "proc, /api/v1/placement/workbench?companyId={c}",
    "proc, /api/v1/placement/workbench?companyId={c}&tab=AWAITING_PAYMENT&text=ARN&page=0&size=5",
    "proc, /api/v1/placement/workbench?companyId={c}&tab=RETURNED",
    "proc, /api/v1/placement/workbench?companyId={c}&tab=CANCELLED",
    "proc, /api/v1/placement/workbench?companyId={c}&tab=HOLD_COVER_EXPIRING",
    "ao, /api/v1/placement/workbench?companyId={c}&tab=BOOKED",
    "proc, /api/v1/placement/workbench/counts?companyId={c}",
    "proc, /api/v1/placement/readiness?companyId={c}&arn=ARN-2026-900007&arn=ARN-2026-910001",
    "ao, /api/v1/placement/accounts/ARN-2026-910001",
    "proc, /api/v1/placement/slips?companyId={c}",
    "proc, /api/v1/placement/slips?companyId={c}&status=SENT",
    "proc, /api/v1/placement/slips/{slip}",
    "proc, /api/v1/placement/slips/{slip}/email-draft",
    "proc, /api/v1/placement/gate/rules",
    "proc, /api/v1/placement/gate/ARN-2026-910004",
    "proc, /api/v1/placement/billing/candidates?companyId={c}",
    "proc, /api/v1/placement/billing/batches?companyId={c}",
    "proc, /api/v1/placement/billing/batches/{batch}",
    "proc, /api/v1/placement/billing/reports?companyId={c}",
    "proc, /api/v1/placement/billing/reports/{report}",
    "proc, /api/v1/issuance/workbench?companyId={c}",
    "proc, /api/v1/issuance/workbench?companyId={c}&tab=REVIEW",
    "proc, /api/v1/issuance/workbench?companyId={c}&tab=READY_TO_DISPATCH&text=ARN",
    "proc, /api/v1/issuance/workbench?companyId={c}&tab=IA_TO_GENERATE",
    "epol, /api/v1/issuance/workbench/counts?companyId={c}",
    "ao, /api/v1/issuance/policies/ARN-2026-910002",
    "proc, /api/v1/issuance/triggers",
    "proc, /api/v1/issuance/epolicies/{epolicy}",
    "ao, /api/v1/issuance/insurance-advice?companyId={c}",
    "ao, /api/v1/issuance/insurance-advice?companyId={c}&text=910002",
    "ao, /api/v1/issuance/insurance-advice/{ia}",
    "epol, /api/v1/issuance/dispatch/{epolicy}/draft",
    "epol, /api/v1/issuance/dispatch/log",
    "proc, /api/v1/issuance/dispatch/log?text=ARN-2026-910005",
  })
  void readEndpointsRespondOk(String username, String url) throws Exception {
    mvc.perform(get(resolve(url)).with(user(users.loadUserByUsername(username))))
        .andExpect(status().isOk());
  }

  @Test
  void filesAreDownloaded() throws Exception {
    mvc.perform(
            get(resolve("/api/v1/placement/slips/{slip}/files/pdf"))
                .with(user(users.loadUserByUsername("proc"))))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"));
    mvc.perform(
            get(resolve("/api/v1/placement/slips/{slip}/files/xlsx"))
                .with(user(users.loadUserByUsername("proc"))))
        .andExpect(status().isOk());
    mvc.perform(
            get(resolve("/api/v1/placement/billing/batches/{batch}/file?format=ods"))
                .with(user(users.loadUserByUsername("proc"))))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Type", "application/vnd.oasis.opendocument.spreadsheet"));
    mvc.perform(
            get(resolve("/api/v1/placement/billing/batches/{batch}/file"))
                .with(user(users.loadUserByUsername("proc"))))
        .andExpect(status().isOk());
    mvc.perform(
            get(resolve("/api/v1/issuance/insurance-advice/{ia}/file"))
                .with(user(users.loadUserByUsername("ao"))))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"));
  }

  @Test
  void marketingCannotRunProcessingActions() throws Exception {
    mvc.perform(
            get(resolve("/api/v1/placement/billing/candidates?companyId={c}"))
                .with(user(users.loadUserByUsername("ao"))))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/placement/accounts/cancel")
                .with(user(users.loadUserByUsername("ao")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"arns\":[\"ARN-2026-910001\"],\"reasonCode\":\"CLIENT_REQUEST\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/issuance/dispatch/batch")
                .with(user(users.loadUserByUsername("proc")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"epolicyIds\":[1]}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void aReportIsUploadedAndAnAccountPlacedOverHttp() throws Exception {
    String arn = fx.awaitingPayment(fx.liability()).getArn();
    String company = data.company().getId().toString();
    mvc.perform(
            multipart("/api/v1/placement/billing/reports")
                .file(
                    new MockMultipartFile(
                        "file",
                        "pay.csv",
                        "text/csv",
                        ("ARN\n" + arn + "\n").getBytes(StandardCharsets.UTF_8)))
                .param("companyId", company)
                .param("kind", "REFERENCE")
                .with(user(users.loadUserByUsername("proc"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.matched").value(1))
        .andExpect(jsonPath("$.lines[0].arn").value(arn));
    String reportId = id("select max(report_id) from plc_payment_report_line where arn = ?", arn);
    mvc.perform(
            post("/api/v1/placement/billing/reports/" + reportId + "/confirm")
                .with(user(users.loadUserByUsername("proc"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
    mvc.perform(
            post("/api/v1/placement/slips/generate")
                .with(user(users.loadUserByUsername("proc")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":" + company + ",\"arns\":[\"" + arn + "\"]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$[0].status").value("GENERATED"));
    mvc.perform(
            post("/api/v1/placement/slips/send")
                .with(user(users.loadUserByUsername("proc")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyId\":" + company + ",\"arns\":[\"" + arn + "\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ok").value(true));
    mvc.perform(
            post("/api/v1/placement/accounts/" + arn + "/insurer-return")
                .with(user(users.loadUserByUsername("proc")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reasonCode\":\"INSURER_DECLINED\",\"remarks\":\"Declined\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reasonCode").value("INSURER_DECLINED"));
    mvc.perform(
            get("/api/v1/placement/accounts/" + arn).with(user(users.loadUserByUsername("proc"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.gate.status").value("RETURNED_BY_INSURER"))
        .andExpect(jsonPath("$.returns.length()").value(1));
  }
}
