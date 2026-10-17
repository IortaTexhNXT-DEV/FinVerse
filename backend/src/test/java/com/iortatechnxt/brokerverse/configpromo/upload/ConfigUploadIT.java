package com.iortatechnxt.brokerverse.configpromo.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.domain.BulkJobStatus;
import com.iortatechnxt.brokerverse.bulk.domain.BulkRowRecord;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.service.SecurityCaches;
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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
  @Autowired private CacheManager caches;

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
    // The grants were just written (or restored after a test removed one): drop the cached ones.
    Cache grants = caches.getCache(SecurityCaches.ROLE_PERMISSIONS);
    if (grants != null) {
      grants.clear();
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
    assertThat(handlers).hasSize(32);
    assertThat(handlers)
        .extracting(ConfigUploadHandler::templateId)
        .contains("D0-01", "D0-15", "PM-02", "PM-10", "UA-02", "UA-06", "MD-01")
        .contains("MD-02", "MD-03", "TX-01", "TX-02")
        .doesNotHaveDuplicates();
    assertThat(as.run(MAKER, () -> uploads.available())).hasSize(32);
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

  @Test
  void aCurrencyAndItsRatesAreUploadedAndApprovedByAnotherUser() {
    String currencies = "Currency code,Name,Symbol,Decimal places,Active\nXTS,Test units,T,2,Y\n";
    BulkJob added = upload("CFG_CURRENCY", "currencies.csv", bytes(currencies));
    assertThat(rows(added)).extracting(BulkRowRecord::getAction).containsExactly("ADD");
    as.run(MAKER, () -> uploads.submit(added.getId()));
    as.run(CHECKER, () -> uploads.approve(added.getId(), "New currency"));
    assertThat(
            jdbc.queryForMap(
                "select name, decimal_places, active from cur_currency where code = 'XTS'"))
        .containsEntry("name", "Test units")
        .containsEntry("decimal_places", 2)
        .containsEntry("active", true);

    String rates =
        "Currency code,Rate type,Effective date,Rate\n"
            + "XTS,SPOT,05-Jan-2027,12.5\n"
            + "XTS,BOOK,05-Jan-2027,12.5\n"
            + "PHP,SPOT,05-Jan-2027,1\n"
            + "XTS,CLOSING,31-Jan-2027,0\n";
    BulkJob job = upload("CFG_EXCHANGE_RATE", "rates.csv", bytes(rates));
    List<BulkRowRecord> checked = rows(job);
    assertThat(job.getValidRows()).isEqualTo(1);
    assertThat(checked.get(1).getMessages()).startsWith("Rate type");
    assertThat(checked.get(2).getMessages()).contains("base currency");
    assertThat(checked.get(3).getMessages()).startsWith("Rate");
    as.run(MAKER, () -> uploads.submit(job.getId()));
    as.run(CHECKER, () -> uploads.approve(job.getId(), "Opening rate"));
    assertThat(
            jdbc.queryForObject(
                "select rate from cur_exchange_rate where currency_code = 'XTS' and rate_type = 'SPOT'",
                java.math.BigDecimal.class))
        .isEqualByComparingTo("12.5");

    String deactivate =
        "Currency code,Name,Symbol,Decimal places,Active\nPHP,Philippine peso,P,2,N\n";
    BulkJob refused = upload("CFG_CURRENCY", "base.csv", bytes(deactivate));
    assertThat(rows(refused).get(0).getMessages()).contains("base currency");
  }

  @Test
  void partyTaxProfilesAndTaxFormsFollowTheRulesOfTheirScreen() {
    String account =
        jdbc.queryForObject(
            "select min(code) from coa_account where company_id = ? and postable",
            String.class,
            company());
    String forms =
        "Form code,Name,Authority,Frequency,Worksheet,Months after period end,Due day,"
            + "Tax payable account,Credit account,Tracked from\n"
            + "CFU-1,Upload test return,BIR,QUARTERLY,VAT,0,25,"
            + account
            + ",,01-Jan-2027\n"
            + "CFU-2,Reminder only,BIR,MONTHLY,NONE,1,10,,,01-Jan-2027\n"
            + "CFU-3,Wrong values,CITY,WEEKLY,VAT,13,32,NOPE,,01-Jan-2027\n";
    BulkJob formJob = upload("CFG_TAX_FORM", "forms.csv", bytes(forms));
    assertThat(formJob.getValidRows()).isEqualTo(2);
    assertThat(rows(formJob).get(2).getMessages())
        .contains("Authority", "Frequency", "Months after period end", "Due day")
        .contains("Tax payable account");
    as.run(MAKER, () -> uploads.submit(formJob.getId()));
    as.run(CHECKER, () -> uploads.approve(formJob.getId(), "Filing calendar"));
    assertThat(
            jdbc.queryForList(
                "select track_filing from tax_form where code in ('CFU-1', 'CFU-2') order by code",
                Boolean.class))
        .containsExactly(true, false);

    String profiles =
        "Party code,TIN,Branch code,Payee class,Registered name,Last name,First name,"
            + "Middle name,Registered address,ZIP code,VAT treatment,Default ATC,"
            + "Withholding agent,Top withholding agent,Government payor,"
            + "Tax exemption certificate no.,Certificate valid from,Certificate valid to\n"
            + "C-000101,123-456-789-00000,,CORPORATE,Juan Dela Cruz Trading,,,,Makati,1226,"
            + "REGULAR,,N,Y,N,,,\n"
            + "C-000101,123456789,,INDIVIDUAL,Juan Dela Cruz,,,,,,,,N,N,N,,,\n"
            + "NO-SUCH-PARTY,12-34,ABC,CORPORATE,Nobody,,,,,,,,N,N,N,EX-1,01-Feb-2027,"
            + "01-Jan-2027\n";
    BulkJob job = upload("CFG_PARTY_TAX_PROFILE", "profiles.csv", bytes(profiles));
    List<BulkRowRecord> checked = rows(job);
    assertThat(job.getValidRows()).isEqualTo(1);
    assertThat(checked.get(1).getMessages()).contains("Duplicate");
    assertThat(checked.get(2).getMessages())
        .contains("Party code", "TIN", "Branch code", "Certificate valid to");
    as.run(MAKER, () -> uploads.submit(job.getId()));
    as.run(CHECKER, () -> uploads.approve(job.getId(), "Tax profiles"));
    assertThat(
            jdbc.queryForMap(
                "select tin, branch_code, withholding_agent, top_withholding_agent, record_status,"
                    + " authorized_by from tax_party_profile where party_code = 'C-000101'"
                    + " and company_id = ?",
                company()))
        .containsEntry("tin", "123456789")
        .containsEntry("branch_code", "00000")
        .containsEntry("withholding_agent", true)
        .containsEntry("top_withholding_agent", true)
        .containsEntry("record_status", "ACTIVE")
        .containsEntry("authorized_by", CHECKER);
  }

  private static byte[] bytes(String csv) {
    return csv.getBytes(StandardCharsets.UTF_8);
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
