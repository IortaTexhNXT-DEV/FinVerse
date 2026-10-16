package com.iortatechnxt.brokerverse.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog;
import com.iortatechnxt.brokerverse.audit.service.MasterChangeLog.Entry;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccount;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccount.AccountPurpose;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccount.BankAccountDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContact;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContact.ContactDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContact.ContactRole;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.InsurerDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.RemittanceCycle;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.RemittanceMode;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.RemittanceTerms;
import com.iortatechnxt.brokerverse.catalog.domain.PlacementChannel;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.catalog.service.InsurerPageService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService.PartyContact;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
class InsurerPageIT {

  @Autowired private InsurerService insurers;
  @Autowired private InsurerPageService page;
  @Autowired private CatalogRecords records;
  @Autowired private MasterChangeLog log;
  @Autowired private AsUser as;
  @Autowired private TestData data;
  @Autowired private MockMvc mvc;

  private InsurerProfile newInsurer() {
    String code = "INS-PG" + Long.toString(System.nanoTime() % 1_000_000L, 36).toUpperCase();
    InsurerProfile insurer =
        as.run(
            "badmin",
            () ->
                insurers.create(
                    data.company().getId(),
                    code,
                    new PartyContact(null, null, null, null),
                    new InsurerDetails(
                        "Page Mutual",
                        null,
                        null,
                        LocalDate.of(2027, 12, 31),
                        PlacementChannel.EMAIL,
                        List.of(),
                        30)));
    as.run("approver", () -> records.authorize(CatalogKind.INSURER, insurer.getId()));
    return insurer;
  }

  @Test
  void contactsBankAccountsAndRemittanceFollowMakerChecker() {
    InsurerProfile insurer = newInsurer();
    InsurerContact contact =
        as.run(
            "badmin",
            () ->
                page.addContact(
                    insurer.getId(),
                    new ContactDetails(
                        ContactRole.REMITTANCE, "Remittance Desk", null, "desk@x.example", null)));
    InsurerBankAccount account =
        as.run(
            "approver",
            () ->
                page.addBankAccount(
                    insurer.getId(),
                    new BankAccountDetails(
                        AccountPurpose.REMITTANCE,
                        "Test Bank",
                        null,
                        "0001-2345",
                        "Page Mutual",
                        "PHP",
                        true)));
    assertThat(contact.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThatThrownBy(
            () ->
                as.run(
                    "approver",
                    () -> records.authorize(CatalogKind.INSURER_BANK_ACCOUNT, account.getId())))
        .extracting("code")
        .isEqualTo("MAKER_CHECKER_VIOLATION");
    as.run("approver", () -> records.authorize(CatalogKind.INSURER_CONTACT, contact.getId()));
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        page.addBankAccount(
                            insurer.getId(),
                            new BankAccountDetails(
                                AccountPurpose.REFUND, "B", null, "0001-2345", "N", "PHP", false))))
        .isInstanceOf(RuntimeException.class);

    InsurerProfile changed =
        as.run(
            "badmin",
            () ->
                page.updateRemittance(
                    insurer.getId(),
                    new RemittanceTerms(
                        "30 days from statement",
                        RemittanceCycle.MONTHLY,
                        RemittanceMode.FUND_TRANSFER,
                        "remit@x.example",
                        "PHP")));
    assertThat(changed.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThat(log.history("InsurerProfile", insurer.getId()))
        .extracting(Entry::field)
        .contains("remittanceCycle", "remittanceMode", "paymentTerms");
    assertThat(page.bankAccounts(insurer.getId()))
        .singleElement()
        .extracting(InsurerBankAccount::isActive)
        .isEqualTo(false);
    assertThat(page.contacts(insurer.getId()))
        .singleElement()
        .extracting(InsurerContact::getAuthorizedBy)
        .isEqualTo("approver");
    assertThat(page.linkedProducts(insurer.getId())).isEmpty();
  }

  @Test
  @WithMockUser(username = "viewer", authorities = "MASTER_VIEW")
  void viewersCannotChangeAnInsurer() throws Exception {
    mvc.perform(
            post("/api/v1/catalog/insurers/1/contacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"CLAIMS\",\"name\":\"X\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "viewer", authorities = "MASTER_VIEW")
  void theDetailCarriesTheWholePage() throws Exception {
    InsurerProfile insurer = newInsurer();
    mvc.perform(get("/api/v1/catalog/insurers/" + insurer.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contacts").isArray())
        .andExpect(jsonPath("$.bankAccounts").isArray())
        .andExpect(jsonPath("$.products").isArray())
        .andExpect(jsonPath("$.insurer.remittance").exists());
  }
}
