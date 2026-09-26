package com.iortatechnxt.brokerverse.account;

import static com.iortatechnxt.brokerverse.account.AccountFixtures.draft;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.token;
import static com.iortatechnxt.brokerverse.account.AccountFixtures.vehicle;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.account.api.dto.AccountResponse;
import com.iortatechnxt.brokerverse.account.api.dto.AccountSummaryResponse;
import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountOrigin;
import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import com.iortatechnxt.brokerverse.account.service.AccountCreateBulkHandler;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountSearch;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Shared work item BT0 (cross-BRD decision D1; BRNB.097, BRID-022.01): business type, renewal link
 * and origin on the account, the search filter, the RENEWAL request and the bulk columns.
 */
@IntegrationTest
class AccountBusinessTypeIT {

  private static final String CLIENT = "CL-2026-000001";

  @Autowired private AccountService accounts;
  @Autowired private AccountQueryService queries;
  @Autowired private AccountCreateBulkHandler bulkCreate;
  @Autowired private AccountFixtures fx;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private AccountDraft motorDraft() {
    Client client = fx.confirmed(CLIENT);
    return draft(client.getId(), "MTR10", "CBG", List.of(vehicle(token(), "1000000")));
  }

  @Test
  void existingFactoriesCreateNewBusinessAndRenewalCarriesItsLink() {
    AccountDraft nbDraft = motorDraft();
    Account nb = as.run("ao", () -> accounts.createDraft(NewAccount.direct(fx.company(), nbDraft)));
    String expiring = "ARN-2025-" + token();
    AccountDraft renewalDraft = motorDraft();
    Account renewal =
        as.run(
            "ao",
            () ->
                accounts.createDraft(
                    NewAccount.renewal(
                        fx.company(),
                        AccountOrigin.EMPLOYEE_BENEFITS,
                        renewalDraft,
                        expiring,
                        null)));

    assertThat(nb.getBusinessType()).isEqualTo(BusinessType.NEW_BUSINESS);
    assertThat(nb.getClassification().origin()).isEqualTo(AccountOrigin.DIRECT);
    assertThat(nb.getClassification().renewalOfRef()).isNull();
    Account loaded = queries.get(renewal.getId());
    assertThat(loaded.getBusinessType()).isEqualTo(BusinessType.RENEWAL);
    assertThat(loaded.getClassification().renewalOfRef()).isEqualTo(expiring);
    assertThat(loaded.getClassification().origin()).isEqualTo(AccountOrigin.EMPLOYEE_BENEFITS);
    assertThat(loaded.getPremium().grossPremium()).isNotNull();

    AccountResponse response = AccountResponse.from(loaded);
    assertThat(response.businessType()).isEqualTo(BusinessType.RENEWAL);
    assertThat(response.renewalOfRef()).isEqualTo(expiring);
    assertThat(response.origin()).isEqualTo(AccountOrigin.EMPLOYEE_BENEFITS);
    assertThat(AccountSummaryResponse.from(loaded).businessType()).isEqualTo(BusinessType.RENEWAL);
    assertThat(
            jdbc.queryForObject(
                "select business_type || '/' || origin from acc_account where id = ?",
                String.class,
                renewal.getId()))
        .isEqualTo("RENEWAL/EMPLOYEE_BENEFITS");

    List<String> renewals = search(BusinessType.RENEWAL);
    assertThat(renewals).contains(renewal.getArn()).doesNotContain(nb.getArn());
    assertThat(search(BusinessType.NEW_BUSINESS))
        .contains(nb.getArn())
        .doesNotContain(renewal.getArn());
    assertThat(search(null)).contains(nb.getArn(), renewal.getArn());
  }

  @Test
  void renewalKeepsTheExpiringPackageVersionOverload() {
    AccountDraft renewalDraft = motorDraft();
    Account renewal =
        as.run(
            "ao",
            () ->
                accounts.createDraft(
                    NewAccount.renewal(
                        fx.company(),
                        AccountOrigin.RENEWAL,
                        renewalDraft,
                        "ARN-2025-" + token(),
                        "ao2",
                        null)));
    assertThat(renewal.getBusinessType()).isEqualTo(BusinessType.RENEWAL);
    assertThat(renewal.getSales().accountOfficer()).isEqualTo("ao2");
  }

  @Test
  void theBulkUploadTakesTheOptionalBusinessTypeColumns() {
    String id = token();
    BulkContext context =
        new BulkContext(
            fx.company(), "BLK-BT0", LocalDate.of(2026, 9, 15), Map.of("product", "MTR10"));
    Map<String, String> cells = new HashMap<>();
    cells.put("Client Code", CLIENT);
    cells.put("Market Segment", "CBG");
    cells.put("Period From", "2026-10-01");
    cells.put("Period To", "2027-10-01");
    cells.put("Sum Insured", "950000");
    cells.put("Plate No", "P" + id);
    cells.put("Engine No", "E" + id);
    cells.put("Chassis No", "C" + id);
    cells.put("Make", "Toyota");
    cells.put("Model", "Vios");
    cells.put("Year Model", "2025");
    cells.put("Business Type", "sideways");
    assertThat(bulkCreate.validate(new BulkRow(1, cells), context))
        .contains("Business Type must be NEW_BUSINESS or RENEWAL");

    cells.put("Business Type", "renewal");
    cells.put("Renewal Of", "POL-OLD-" + id);
    BulkRow row = new BulkRow(2, cells);
    assertThat(bulkCreate.validate(row, context)).isEmpty();
    String arn = as.run("ao", () -> bulkCreate.commit(row, context));
    Account created = queries.requireByArn(arn);
    assertThat(created.getBusinessType()).isEqualTo(BusinessType.RENEWAL);
    assertThat(created.getClassification().renewalOfRef()).isEqualTo("POL-OLD-" + id);
    assertThat(bulkCreate.columns())
        .extracting(c -> c.header())
        .contains("Business Type", "Renewal Of");
  }

  private List<String> search(BusinessType type) {
    AccountSearch all = AccountSearch.all(fx.company());
    AccountSearch criteria =
        new AccountSearch(
            all.companyId(),
            null,
            null,
            null,
            null,
            "MTR10",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "ao",
            false,
            type);
    return queries
        .search(criteria, PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "createdAt", "id")))
        .getContent()
        .stream()
        .map(Account::getArn)
        .toList();
  }
}
