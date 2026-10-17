package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.SalesStamp;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService.SalesAssignment;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseClient;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The sales facts of a case's client (SNSRP-303, 402, 404; design 9 "account (read)", "catalog"):
 * the marketing unit (sales department) and account officer of the client's latest account, else of
 * the user who registered the client, and the unit head of that unit ({@code
 * SalesOrganisationService.unitHead}). The account officer is excluded from investigating and
 * approving the case (conflict of interest, FR-SS-043).
 */
@Component
@Transactional(readOnly = true)
public class CaseClientFacts {

  private final AccountQueryService accounts;
  private final SalesOrganisationService sales;

  /**
   * Creates the lookup.
   *
   * @param accounts account reads
   * @param sales sales organisation
   */
  public CaseClientFacts(AccountQueryService accounts, SalesOrganisationService sales) {
    this.accounts = accounts;
    this.sales = sales;
  }

  /**
   * The client of a new case with its sales facts.
   *
   * @param client the client
   * @return the facts
   */
  public CaseClient of(Client client) {
    Long companyId = client.getCompanyId();
    Optional<SalesStamp> stamp =
        accounts.byClient(client.getId()).stream()
            .map(Account::getSales)
            .filter(Objects::nonNull)
            .filter(s -> s.department() != null || s.team() != null)
            .findFirst();
    String officer =
        stamp
            .map(SalesStamp::accountOfficer)
            .filter(Objects::nonNull)
            .orElse(client.getCreatedBy());
    Optional<SalesAssignment> assignment =
        stamp.isPresent() ? Optional.empty() : sales.assignmentOf(companyId, officer);
    String unit =
        stamp
            .map(s -> s.department() != null ? s.department() : s.team())
            .orElse(assignment.map(a -> first(a.department(), a.team())).orElse(null));
    String lowest =
        stamp
            .map(s -> first(s.team(), s.department()))
            .orElse(assignment.map(a -> first(a.team(), a.department())).orElse(null));
    String head = lowest == null ? null : sales.unitHead(companyId, lowest).orElse(null);
    return new CaseClient(
        companyId,
        client.getId(),
        client.getCode(),
        client.getDisplayName(),
        client.getClientType().name(),
        unit,
        head,
        officer);
  }

  /**
   * The sales team of a user (team scope of investigators, FR-SS-041 R1).
   *
   * @param companyId company
   * @param username user
   * @return the team code, empty when the user is in no team
   */
  public Optional<String> teamOf(Long companyId, String username) {
    return username == null
        ? Optional.empty()
        : sales.assignmentOf(companyId, username).map(SalesAssignment::team);
  }

  private static String first(String a, String b) {
    return a != null ? a : b;
  }
}
