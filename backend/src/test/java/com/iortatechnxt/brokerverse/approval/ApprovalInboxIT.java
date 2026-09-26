package com.iortatechnxt.brokerverse.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.accounting.api.dto.RuleLineDto;
import com.iortatechnxt.brokerverse.accounting.api.dto.RuleRequest;
import com.iortatechnxt.brokerverse.accounting.domain.AccountingRule;
import com.iortatechnxt.brokerverse.accounting.service.AccountingRuleService;
import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.coa.api.dto.GlAccountRequest;
import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import com.iortatechnxt.brokerverse.coa.domain.AccountLevel;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.coa.domain.GlAccount;
import com.iortatechnxt.brokerverse.coa.domain.SubLedgerType;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.organization.api.dto.BranchRequest;
import com.iortatechnxt.brokerverse.organization.api.dto.CompanyRequest;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.party.api.dto.PartyRequest;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.JournalFixtures;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class ApprovalInboxIT {

  @Autowired private ApprovalInboxService inbox;
  @Autowired private JournalFixtures journals;
  @Autowired private OrganizationService organization;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private PartyService parties;
  @Autowired private AccountingRuleService rules;
  @Autowired private AsUser as;
  @Autowired private TestData data;
  @Autowired private MockMvc mvc;

  private static List<String> references(List<PendingApproval> items) {
    return items.stream().map(PendingApproval::reference).toList();
  }

  @Test
  void submittedJournalReachesCheckersButNotItsMaker() {
    JournalBatch batch =
        journals.submitted(journals.request("5603", "1111", "1234.00", LocalDate.now()));

    List<PendingApproval> checker = as.run("checker", () -> inbox.inbox(null));
    assertThat(references(checker)).contains(batch.getBatchNo());
    PendingApproval item =
        checker.stream()
            .filter(i -> i.reference().equals(batch.getBatchNo()))
            .findFirst()
            .orElseThrow();
    assertThat(item.module()).isEqualTo("GL");
    assertThat(item.link()).isEqualTo("/gl/journals/" + batch.getId());
    assertThat(item.currency()).isEqualTo("PHP");
    assertThat(item.submittedBy()).isEqualTo("accountant");

    assertThat(references(as.run("accountant", () -> inbox.inbox(null))))
        .doesNotContain(batch.getBatchNo());
    // The underwriter authorizes underwriting documents only: no journals.
    assertThat(as.run("uw", () -> inbox.inbox(null))).noneMatch(i -> "GL".equals(i.module()));
    assertThat(references(inbox.pendingAll())).contains(batch.getBatchNo());
    Long otherCompany = data.company().getId() + 1000;
    assertThat(references(as.run("checker", () -> inbox.inbox(otherCompany))))
        .doesNotContain(batch.getBatchNo());
  }

  @Test
  void journalsAboveTheCheckersLimitAreNotOffered() {
    JournalBatch big =
        journals.submitted(journals.request("5603", "1111", "6000000.00", LocalDate.now()));
    assertThat(references(as.run("checker", () -> inbox.inbox(null))))
        .doesNotContain(big.getBatchNo());
    assertThat(references(as.run("fmanager", () -> inbox.inbox(null)))).contains(big.getBatchNo());
  }

  @Test
  void pendingMasterDataIsOfferedToAuthorizersOtherThanTheMaker() {
    var branch =
        as.run(
            "accountant",
            () ->
                organization.createBranch(
                    new BranchRequest(
                        data.company().getId(),
                        "APR1",
                        "Approval Test Branch",
                        null,
                        null,
                        LocalDate.of(2026, 1, 1),
                        false,
                        false,
                        null,
                        null,
                        null,
                        null)));
    List<PendingApproval> fmanager = as.run("fmanager", () -> inbox.inbox(data.company().getId()));
    assertThat(fmanager)
        .anySatisfy(
            i -> {
              assertThat(i.reference()).isEqualTo(branch.getCode());
              assertThat(i.module()).isEqualTo("MASTER_DATA");
              assertThat(i.link()).isEqualTo("/setup/branches");
            });
    // Seed supplier pending authorization (V902) is offered too.
    assertThat(references(fmanager)).contains("S-000901");
    assertThat(references(as.run("accountant", () -> inbox.inbox(null))))
        .doesNotContain("APR1", "S-000901");

    var counts = as.run("fmanager", () -> inbox.counts(null));
    assertThat(counts.total()).isPositive();
    assertThat(counts.byModule()).containsKey("MASTER_DATA");
  }

  @Test
  void masterDataLeavesTheInboxOnceAuthorized() {
    Long company = data.company().getId();
    GlAccountRequest accountRequest =
        new GlAccountRequest(
            company,
            "1698",
            "Inbox test receivable",
            null,
            AccountClass.ASSET,
            AccountLevel.SUB,
            "1600",
            "RECV",
            false,
            SubLedgerType.NONE,
            true,
            false,
            false,
            false,
            false,
            false,
            null,
            "Other Assets",
            LocalDate.of(2026, 1, 1),
            Set.of(),
            Set.of(),
            Set.of());
    GlAccount account = as.run("accountant", () -> accounts.create(accountRequest));
    Party party =
        as.run(
            "accountant",
            () ->
                parties.create(
                    new PartyRequest(
                        company,
                        "S-APR-01",
                        "Inbox Test Supplier",
                        PartyType.SUPPLIER,
                        null,
                        null,
                        null,
                        null,
                        "PHP",
                        30,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));
    Company newCompany =
        as.run(
            "accountant",
            () ->
                organization.createCompany(
                    new CompanyRequest(
                        "APRC", "Approval Test Co", "PHP", null, null, 1, 30, 5, null)));
    AccountingRule rule =
        as.run(
            "fmanager",
            () ->
                rules.create(
                    new RuleRequest(
                        company,
                        "POLICY_ISSUE",
                        "Inbox test rule",
                        null,
                        null,
                        900,
                        LocalDate.of(2030, 1, 1),
                        LocalDate.of(2030, 1, 31),
                        List.of(
                            new RuleLineDto(BalanceSide.DEBIT, "1201", "TOTAL_DUE", true, null),
                            new RuleLineDto(
                                BalanceSide.CREDIT, "2205", "TOTAL_DUE", false, null)))));

    List<String> pending = references(as.run("checker", () -> inbox.inbox(null)));
    assertThat(pending).contains("1698", "S-APR-01", "APRC", "POLICY_ISSUE #" + rule.getId());
    assertThat(references(as.run("fmanager", () -> inbox.inbox(null))))
        .doesNotContain("POLICY_ISSUE #" + rule.getId())
        .contains("1698");

    as.run("accountant", () -> accounts.update(account.getId(), accountRequest));
    as.run("checker", () -> accounts.authorize(account.getId()));
    as.run("checker", () -> parties.authorize(party.getId()));
    as.run("checker", () -> organization.authorizeCompany(newCompany.getId()));
    as.run("checker", () -> rules.authorize(rule.getId()));
    assertThat(references(as.run("checker", () -> inbox.inbox(null))))
        .doesNotContain("1698", "S-APR-01", "APRC", "POLICY_ISSUE #" + rule.getId());
  }

  @Test
  void viewerRules() {
    ApprovalViewer system = ApprovalViewer.system();
    assertThat(system.can("ANYTHING")).isTrue();
    assertThat(system.mayApproveItemOf("x")).isTrue();
    ApprovalViewer user = ApprovalViewer.user("carla", Set.of("JOURNAL_AUTHORIZE"));
    assertThat(user.can("JOURNAL_AUTHORIZE")).isTrue();
    assertThat(user.can("MASTER_AUTHORIZE")).isFalse();
    assertThat(user.mayApproveItemOf("carla")).isFalse();
    assertThat(user.mayApproveItemOf("andy")).isTrue();
    assertThat(inbox.inbox(null)).isEmpty();
  }

  @Test
  @WithUserDetails("checker")
  void inboxEndpointsRespond() throws Exception {
    mvc.perform(get("/api/v1/approvals/inbox")).andExpect(status().isOk());
    mvc.perform(get("/api/v1/approvals/counts?companyId=" + data.company().getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").isNumber());
  }
}
