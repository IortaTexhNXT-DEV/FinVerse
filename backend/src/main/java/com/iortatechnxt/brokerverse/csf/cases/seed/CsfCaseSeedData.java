package com.iortatechnxt.brokerverse.csf.cases.seed;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCase.Category;
import com.iortatechnxt.brokerverse.csf.cases.domain.CsfCaseStore;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseService;
import com.iortatechnxt.brokerverse.csf.cases.service.CsfCaseService.LogRequest;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Logs the cases of the case management storyline through the real case services, signed in as the
 * SIT/UAT users of the personas (seed profile only, idempotent): for client CL-2026-000001 a
 * request waiting with the fulfilment unit, a problem returned to the agent, an inquiry resolved on
 * the call and a complaint escalated by the supervisor and past its due date.
 */
@Component
@Profile("seed")
@Order(161)
public class CsfCaseSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CsfCaseSeedData.class);
  private static final String CLIENT_CODE = "CL-2026-000001";
  private static final String AGENT = "csfagent";
  private static final String SUPERVISOR = "csfsup";
  private static final String FULFILMENT = "csffu";

  private final CompanyRepository companies;
  private final ClientRepository clients;
  private final AccountQueryService accounts;
  private final CsfCaseService cases;
  private final CsfCaseStore store;
  private final UserDetailsService users;
  private final NamedParameterJdbcTemplate jdbc;
  private final TransactionTemplate tx;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param clients clients
   * @param accounts accounts of the client
   * @param cases case actions
   * @param store cases of the client
   * @param users user directory (sign-in of the personas)
   * @param jdbc dates of the overdue case
   * @param transactions transaction manager
   */
  @SuppressWarnings("java:S107") // the services the seed goes through
  public CsfCaseSeedData(
      CompanyRepository companies,
      ClientRepository clients,
      AccountQueryService accounts,
      CsfCaseService cases,
      CsfCaseStore store,
      UserDetailsService users,
      NamedParameterJdbcTemplate jdbc,
      PlatformTransactionManager transactions) {
    this.companies = companies;
    this.clients = clients;
    this.accounts = accounts;
    this.cases = cases;
    this.store = store;
    this.users = users;
    this.jdbc = jdbc;
    this.tx = new TransactionTemplate(transactions);
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      tx.executeWithoutResult(s -> load());
    } catch (RuntimeException e) {
      LOG.warn("Case management seed data not loaded: {}", e.getMessage());
    }
  }

  private void load() {
    Company company = companies.findByCode("FVI").orElse(null);
    Client client =
        company == null ? null : clients.findByCode(company.getId(), CLIENT_CODE).orElse(null);
    if (client == null || !store.ofClient(company.getId(), client.getId()).isEmpty()) {
      return;
    }
    Long companyId = company.getId();
    List<Account> owned = accounts.byClient(client.getId());
    Long account = owned.isEmpty() ? null : owned.get(0).getId();
    openCases(companyId, client, account);
    closedAndEscalated(companyId, client);
    LOG.info("Case management seed data: four cases of {}", CLIENT_CODE);
  }

  private void openCases(Long companyId, Client client, Long account) {
    as(
        AGENT,
        () ->
            cases.log(
                companyId,
                request(
                    client,
                    account,
                    "REQUEST",
                    "OR_REQUEST",
                    "PROCESSING",
                    "Copy of the official receipt",
                    "The client asks for a copy of the official receipt of the last payment.")));
    CsfCase problem =
        as(
            AGENT,
            () ->
                cases.log(
                    companyId,
                    request(
                        client,
                        account,
                        "PROBLEM",
                        "BILLING_INQUIRY",
                        "PROCESSING",
                        "Premium billed twice",
                        "The client was billed twice for the same instalment.")));
    as(
        FULFILMENT,
        () ->
            cases.returnToAgent(
                problem.id(),
                "INCOMPLETE_DETAILS",
                "Please give the dates and amounts of both bills."));
  }

  private void closedAndEscalated(Long companyId, Client client) {
    as(
        AGENT,
        () ->
            cases.log(
                companyId,
                new LogRequest(
                    client.getId(),
                    null,
                    null,
                    null,
                    category("INQUIRY", "COVERAGE_INQUIRY", "PROCESSING"),
                    "Coverage of flood damage",
                    "The client asked whether the policy covers flood damage.",
                    true,
                    "Explained the flood extension of the policy and its deductible.",
                    "INFORMATION_PROVIDED")));
    CsfCase complaint =
        as(
            AGENT,
            () ->
                cases.log(
                    companyId,
                    request(
                        client,
                        null,
                        "COMPLAINT",
                        "CLAIM_STATUS",
                        "CLAIMS",
                        "Claim settlement delayed",
                        "The client complains that the settlement of the claim is delayed.")));
    jdbc.update(
        "update csf_case set created_at = created_at - interval '10 days', due_date = due_date -"
            + " 10 where id = :id",
        Map.of("id", complaint.id()));
    as(
        SUPERVISOR,
        () -> cases.escalate(complaint.id(), "DUE_FOR_RESOLUTION", "Client called twice"));
  }

  private static LogRequest request(
      Client client,
      Long account,
      String type,
      String nature,
      String unit,
      String subject,
      String description) {
    return new LogRequest(
        client.getId(),
        account,
        null,
        null,
        category(type, nature, unit),
        subject,
        description,
        false,
        null,
        null);
  }

  private static Category category(String type, String nature, String unit) {
    return new Category("PHONE", type, nature, "MOTOR", "CONSUMER_1", unit, "NORMAL");
  }

  private <T> T as(String username, Supplier<T> work) {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    UserDetails details = users.loadUserByUsername(username);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
    try {
      return work.get();
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }
}
