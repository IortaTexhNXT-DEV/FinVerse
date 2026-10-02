package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessBatchStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestBatch;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestBatchRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessBatchService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessBatchService.BatchDecision;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessBatchService.LineOutcome;
import com.iortatechnxt.brokerverse.nbadmin.service.bulk.AccessRequestBulkHandler;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/** Bulk access requests (BRD 1.009) and the User Access endpoints over HTTP. */
@IntegrationTest
class UserAccessBulkAndApiIT {

  private static final String HEADER =
      "Action,User ID,Full Name,E-mail,Windows ID,Home Branch Code,Business Unit,User Level,"
          + "Group Profiles,Effective Date,Remarks\n";
  private static final String REQUESTOR = "requestor";
  private static final String APPROVER = "uamapprover";

  @Autowired private BulkService bulk;
  @Autowired private AccessBatchService batches;
  @Autowired private AccessRequestBatchRepository batchRepository;
  @Autowired private UserAdminService users;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private Api api;

  private BulkJob upload(String csv) {
    Long company =
        jdbc.queryForObject("select id from org_company order by id limit 1", Long.class);
    return as.run(
        REQUESTOR,
        () ->
            bulk.upload(
                new BulkUpload(
                    company,
                    AccessRequestBulkHandler.CODE,
                    "users.csv",
                    csv.getBytes(StandardCharsets.UTF_8),
                    Map.of())));
  }

  @Test
  void bulkBatchOfThreeEnrolmentsIsSubmittedApprovedAndApplied() {
    List<String> ids =
        List.of(AccessRequestIT.username(), AccessRequestIT.username(), AccessRequestIT.username());
    StringBuilder csv = new StringBuilder(HEADER);
    ids.forEach(
        id ->
            csv.append("ENROL,")
                .append(id)
                .append(",Bulk ")
                .append(id)
                .append(",,,,,,MKT_AO,,Joined\n"));
    csv.append("promote,").append(AccessRequestIT.username()).append(",X,,,,,,MKT_AO,,x\n");
    csv.append("ENROL,mkttl,Dup,,,,,,MKT_AO,,x\n");
    BulkJob job = upload(csv.toString());
    assertThat(job.getValidRows()).isEqualTo(3);
    List<BulkRowRecord> rows = bulk.rows(job.getId(), null, Pageable.ofSize(10)).getContent();
    assertThat(rows.get(3).getMessages()).contains("Action promote is not valid");
    assertThat(rows.get(4).getMessages()).contains("User mkttl already exists");

    as.run(REQUESTOR, () -> bulk.commit(job.getId()));
    AccessRequestBatch batch = batchRepository.findByBatchNo(job.getJobNo()).orElseThrow();
    assertThat(batch.getLines()).isEqualTo(3);
    as.run(REQUESTOR, () -> batches.submit(batch.getId(), APPROVER, "Three new officers"));
    assertThat(as.run(REQUESTOR, () -> batches.get(batch.getId())).getStatus())
        .isEqualTo(AccessBatchStatus.PENDING);

    BatchDecision decision = as.run(APPROVER, () -> batches.approve(batch.getId(), "ok"));
    assertThat(decision.batch().getStatus()).isEqualTo(AccessBatchStatus.APPROVED);
    assertThat(decision.lines())
        .extracting(LineOutcome::status)
        .containsOnly(AccessRequestStatus.APPROVED);
    assertThat(decision.lines()).allSatisfy(l -> assertThat(l.temporaryPassword()).isNotBlank());
    ids.forEach(id -> assertThat(users.getByUsername(id).isEnabled()).isTrue());
  }

  @Test
  void requestsAreDraftedSubmittedAndReadOverHttp() throws Exception {
    String name = AccessRequestIT.username();
    long id =
        api.read(
                api.doPost(
                        REQUESTOR,
                        "/api/v1/nbadmin/access-requests?draft=true",
                        Json.of(
                            "type",
                            "CREATE_USER",
                            "username",
                            name,
                            "fullName",
                            "Api Draft",
                            "roleCodes",
                            List.of("MKT_AO"),
                            "windowsId",
                            "WA" + name))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andExpect(jsonPath("$.details.windowsId").value("WA" + name)))
            .get("id")
            .asLong();
    String base = "/api/v1/nbadmin/access-requests/" + id;
    api.doPut(
            REQUESTOR,
            base,
            Json.of(
                "type",
                "CREATE_USER",
                "username",
                name,
                "fullName",
                "Api Draft",
                "roleCodes",
                List.of("MKT_AO"),
                "justification",
                "Joined Marketing"))
        .andExpect(jsonPath("$.justification").value("Joined Marketing"));
    api.doGet("badmin", base).andExpect(status().isForbidden());
    api.doGet(REQUESTOR, "/api/v1/nbadmin/approvers?subject=" + name)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.username == 'uamapprover')].fullName").exists());
    api.doPost(
            REQUESTOR, base + "/submit", Json.of("approvers", List.of(APPROVER), "remarks", "Go"))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.lifecycle.assignedApprover").value(APPROVER));
    api.doGet(REQUESTOR, "/api/v1/nbadmin/access-requests?scope=MINE&text=" + name)
        .andExpect(jsonPath("$.content[0].id").value(id));
    api.doGet(APPROVER, "/api/v1/nbadmin/access-requests?scope=ASSIGNED&text=" + name)
        .andExpect(jsonPath("$.content[0].id").value(id));
    api.doGet(REQUESTOR, base + "/history")
        .andExpect(jsonPath("$[0].action").value("SAVE"))
        .andExpect(jsonPath("$[2].action").value("SUBMIT"));
    api.doPost(REQUESTOR, base + "/approve", Json.of()).andExpect(status().isForbidden());
    api.doPost(APPROVER, base + "/cancel", Json.of("comment", "x"))
        .andExpect(jsonPath("$.code").value("ACCESS_NOT_REQUESTER"));
    api.doPost(REQUESTOR, base + "/cancel", Json.of("comment", "Wrong unit"))
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.lifecycle.cancelReason").value("Wrong unit"));
    api.doGet(REQUESTOR, "/api/v1/nbadmin/access-settings")
        .andExpect(jsonPath("$.directRoleEdit").value(false))
        .andExpect(jsonPath("$.roleApplyOnApproval").value(false));
    api.doGet("ao", "/api/v1/nbadmin/access-requests").andExpect(status().isForbidden());
  }

  @Test
  void batchesAreReadOverHttp() throws Exception {
    BulkJob job =
        upload(HEADER + "ENROL," + AccessRequestIT.username() + ",Http Bulk,,,,,,MKT_AO,,Joined\n");
    as.run(REQUESTOR, () -> bulk.commit(job.getId()));
    long id = batchRepository.findByBatchNo(job.getJobNo()).orElseThrow().getId();
    JsonNode list = api.read(api.doGet(REQUESTOR, "/api/v1/nbadmin/access-batches"));
    assertThat(list.get("content").findValuesAsText("batchNo")).contains(job.getJobNo());
    api.doGet(REQUESTOR, "/api/v1/nbadmin/access-batches/" + id)
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.lines").value(1));
    api.doGet(REQUESTOR, "/api/v1/nbadmin/access-batches/" + id + "/lines")
        .andExpect(jsonPath("$[0].status").value("DRAFT"));
    api.doPost(
            REQUESTOR,
            "/api/v1/nbadmin/access-batches/" + id + "/submit",
            Json.of("approver", APPROVER, "remarks", "One officer"))
        .andExpect(jsonPath("$.status").value("PENDING"));
    api.doPost(
            APPROVER,
            "/api/v1/nbadmin/access-batches/" + id + "/return",
            Json.of("comment", "Wrong branch"))
        .andExpect(jsonPath("$.status").value("RETURNED"));
    api.doPost(
            REQUESTOR,
            "/api/v1/nbadmin/access-batches/" + id + "/cancel",
            Json.of("comment", "Withdrawn"))
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    api.doGet("secapprover", "/api/v1/nbadmin/access-batches/" + id)
        .andExpect(status().isNotFound());
  }
}
