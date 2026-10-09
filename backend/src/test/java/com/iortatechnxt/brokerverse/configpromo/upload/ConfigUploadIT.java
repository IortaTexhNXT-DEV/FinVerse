package com.iortatechnxt.brokerverse.configpromo.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The uploads of the configuration screens: the current data of every screen downloads in the
 * layout of its template and uploads again without a change; a new row is applied only when a
 * second user approves the upload; the messages name the column.
 */
@IntegrationTest
class ConfigUploadIT {

  private static final String MAKER = "cfu.maker";
  private static final String CHECKER = "cfu.checker";

  @Autowired private ConfigUploads uploads;
  @Autowired private List<ConfigUploadHandler> handlers;
  @Autowired private AsUser as;
  @Autowired private TestData data;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void users() {
    for (String user : List.of(MAKER, CHECKER)) {
      String role = user.toUpperCase().replace('.', '_');
      jdbc.update(
          "insert into sec_role (code, name, created_at, created_by) select ?, ?, now(), 'test'"
              + " where not exists (select 1 from sec_role where code = ?)",
          role,
          "Upload test " + user,
          role);
      for (Permission p : Permission.values()) {
        jdbc.update(
            "insert into sec_role_permission (role_id, permission) select r.id, ? from sec_role r where r.code = ?"
                + " and not exists (select 1 from sec_role_permission x where x.role_id = r.id and x.permission = ?)",
            p.name(),
            role,
            p.name());
      }
      jdbc.update(
          "insert into sec_user (username, full_name, password_hash, created_at, created_by)"
              + " select ?, ?, '!', now(), 'test' where not exists (select 1 from sec_user where username = ?)",
          user,
          "Upload Tester " + user.substring(4),
          user);
      jdbc.update(
          "insert into sec_user_role (user_id, role_id) select u.id, r.id from sec_user u, sec_role r"
              + " where u.username = ? and r.code = ? and not exists"
              + " (select 1 from sec_user_role x where x.user_id = u.id and x.role_id = r.id)",
          user,
          role);
    }
  }

  private Long company() {
    return data.company().getId();
  }

  private BulkJob upload(String type, String fileName, byte[] content) {
    return as.run(MAKER, () -> uploads.upload(company(), type, fileName, content));
  }

  private List<BulkRowRecord> rows(BulkJob job) {
    return as.run(MAKER, () -> uploads.rows(job.getId(), null, Pageable.ofSize(500)).getContent());
  }

  @Test
  void everyScreenHasAnUploadWithItsWorkbookTab() {
    assertThat(handlers).hasSize(28);
    assertThat(handlers)
        .extracting(ConfigUploadHandler::templateId)
        .contains("D0-01", "D0-15", "PM-02", "PM-10", "UA-02", "UA-06", "MD-01")
        .doesNotHaveDuplicates();
    assertThat(as.run(MAKER, () -> uploads.available())).hasSize(28);
    for (ConfigUploadHandler h : handlers) {
      byte[] template = as.run(MAKER, () -> uploads.template(h.code(), company()));
      assertThat(template).as(h.templateId()).isNotEmpty();
    }
  }

  @Test
  void theCurrentDataUploadsAgainWithoutAnyChange() {
    List<String> problems = new ArrayList<>();
    for (ConfigUploadHandler h : handlers) {
      List<Map<String, String>> before = as.run(MAKER, () -> h.exportRows(company()));
      if (before.isEmpty()) {
        continue;
      }
      byte[] file = as.run(MAKER, () -> uploads.export(h.code(), company()));
      BulkJob job = upload(h.code(), h.templateId() + ".xlsx", file);
      // Records added by other tests without a value the workbook requires are refused as
      // "mandatory"; any other message is a round-trip problem.
      rows(job).stream()
          .filter(r -> r.getMessages() != null && !r.getMessages().isBlank())
          .filter(r -> !onlyMandatory(r.getMessages()))
          .limit(3)
          .forEach(
              r -> problems.add(h.templateId() + " row " + r.getRowNo() + ": " + r.getMessages()));
      if (job.getValidRows() == 0) {
        continue;
      }
      assertThat(rows(job).stream().filter(r -> r.getAction() != null))
          .as(h.templateId())
          .extracting(BulkRowRecord::getAction)
          .containsOnly("UPDATE");
      as.run(MAKER, () -> uploads.submit(job.getId()));
      BulkJob done = as.run(CHECKER, () -> uploads.approve(job.getId(), "Round trip"));
      assertThat(done.getStatus()).as(h.templateId()).isEqualTo(BulkJobStatus.COMPLETED);
      assertThat(done.getFailedRows()).as(h.templateId()).isZero();
      assertThat(as.run(MAKER, () -> h.exportRows(company()))).as(h.templateId()).isEqualTo(before);
    }
    assertThat(problems).isEmpty();
  }

  @Test
  void aNewValueIsAppliedOnlyWhenAnotherUserApproves() {
    String csv =
        "List,Code,Label,Sort order,Effective from\n"
            + "UAM_USER_LEVEL,CFU_TEAM_LEAD,Team lead,90,04-Jan-2027\n";
    BulkJob job = upload("CFG_ACCESS_LIST", "levels.csv", csv.getBytes(StandardCharsets.UTF_8));
    assertThat(job.getValidRows()).isEqualTo(1);
    assertThat(rows(job)).extracting(BulkRowRecord::getAction).containsExactly("ADD");
    assertThat(count("CFU_TEAM_LEAD")).isZero();

    as.run(MAKER, () -> uploads.submit(job.getId()));
    assertThatThrownBy(() -> as.run(MAKER, () -> uploads.approve(job.getId(), "self")))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(count("CFU_TEAM_LEAD")).isZero();

    as.run(CHECKER, () -> uploads.approve(job.getId(), "Agreed"));
    Map<String, Object> value =
        jdbc.queryForMap(
            "select label, sort_order, created_by, authorized_by, record_status from lov_value"
                + " where type_code = 'UAM_USER_LEVEL' and code = 'CFU_TEAM_LEAD'");
    assertThat(value)
        .containsEntry("label", "Team lead")
        .containsEntry("sort_order", 90)
        .containsEntry("created_by", MAKER)
        .containsEntry("authorized_by", CHECKER)
        .containsEntry("record_status", "ACTIVE");

    BulkJob again =
        upload("CFG_ACCESS_LIST", "levels-again.csv", csv.getBytes(StandardCharsets.UTF_8));
    assertThat(rows(again)).extracting(BulkRowRecord::getAction).containsExactly("UPDATE");
  }

  @Test
  void theMessagesNameTheColumnAndNothingIsApplied() {
    String csv =
        "Parameter,Value as delivered,Value wanted,Reason,Approved by\n"
            + "PASSWORD_MAX_AGE_DAYS,90,ninety,Policy,Information Security\n"
            + "NOT_A_PARAMETER,1,2,,Information Security\n";
    BulkJob job =
        upload("CFG_SECURITY_PARAMETER", "parameters.csv", csv.getBytes(StandardCharsets.UTF_8));
    assertThat(job.getValidRows()).isZero();
    List<BulkRowRecord> rows = rows(job);
    assertThat(rows.get(0).getMessages()).contains("Value wanted");
    assertThat(rows.get(1).getMessages()).contains("Parameter");
    assertThatThrownBy(() -> as.run(MAKER, () -> uploads.submit(job.getId())))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void anApproverOfAnotherAreaCannotApprove() {
    String csv =
        "List,Code,Label,Sort order,Effective from\nUAM_USER_LEVEL,CFU_OTHER,Other,95,04-Jan-2027\n";
    BulkJob job = upload("CFG_ACCESS_LIST", "other.csv", csv.getBytes(StandardCharsets.UTF_8));
    as.run(MAKER, () -> uploads.submit(job.getId()));
    jdbc.update(
        "delete from sec_role_permission where permission = 'MASTER_AUTHORIZE' and role_id ="
            + " (select id from sec_role where code = 'CFU_CHECKER')");
    try {
      assertThatThrownBy(() -> as.run(CHECKER, () -> uploads.approve(job.getId(), "no")))
          .isInstanceOf(RuntimeException.class);
      assertThat(count("CFU_OTHER")).isZero();
    } finally {
      users();
    }
  }

  private static boolean onlyMandatory(String messages) {
    return java.util.Arrays.stream(messages.split(";"))
        .map(String::trim)
        .allMatch(m -> m.endsWith("is mandatory"));
  }

  private int count(String code) {
    return jdbc.queryForObject(
        "select count(*) from lov_value where type_code = 'UAM_USER_LEVEL' and code = ?",
        Integer.class,
        code);
  }
}
