package com.iortatechnxt.brokerverse.csf;

import static com.iortatechnxt.brokerverse.csf.CsfFixtures.AGENT;
import static com.iortatechnxt.brokerverse.csf.CsfFixtures.SUPERVISOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.csf.service.CsfDocumentService;
import com.iortatechnxt.brokerverse.csf.service.CsfDocumentService.Upload;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.AccountLine;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.ClientHitView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.DocumentView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.EpolicyView;
import com.iortatechnxt.brokerverse.csf.service.CsfViews.SearchView;
import com.iortatechnxt.brokerverse.csf.service.CustomerSearchService;
import com.iortatechnxt.brokerverse.csf.service.ResendService;
import com.iortatechnxt.brokerverse.csf.service.ResendService.Kind;
import com.iortatechnxt.brokerverse.csf.service.ResendService.ResendRequest;
import com.iortatechnxt.brokerverse.csf.service.SearchKey;
import com.iortatechnxt.brokerverse.csf.service.ServicingViewService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService;
import com.iortatechnxt.brokerverse.issuance.service.EpolicyService.ReceivedFile;
import com.iortatechnxt.brokerverse.lov.service.LovCaches;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.PlacementTestData;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

/**
 * Customer Search, Servicing View, documents and resends of the Customer Servicing Facility
 * (FR-CSF-010 to 013, 030 to 033, 042; test plan TC-CSF-010 to 013, 030 to 033).
 */
@IntegrationTest
class CsfServicingIT {

  @Autowired private CsfFixtures fx;
  @Autowired private PlacementTestData placement;
  @Autowired private CustomerSearchService search;
  @Autowired private ServicingViewService views;
  @Autowired private CsfDocumentService documents;
  @Autowired private ResendService resends;
  @Autowired private ClientService clients;
  @Autowired private EpolicyService epolicies;
  @Autowired private DocumentComposer composer;
  @Autowired private CacheManager caches;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private SearchView find(SearchKey key, String value) {
    return as.run(AGENT, () -> search.search(fx.company(), key, value));
  }

  private static List<String> arns(SearchView view) {
    return view.clients().stream()
        .flatMap(c -> c.accounts().stream())
        .map(AccountLine::arn)
        .toList();
  }

  private Long clientIdOf(String code) {
    return clients.requireByCode(fx.company(), code).getId();
  }

  @Test
  void aSearchByPnApplicationOrAccountNumberFindsTheClientOfTheAccount() {
    Account account = placement.awaitingPayment(placement.fire());
    String pn =
        jdbc.queryForObject(
            "select pn_number from acc_account_pn where account_id = ? and pn_index = 0",
            String.class,
            account.getId());
    String application =
        jdbc.queryForObject(
            "select loan_application_no from acc_account where id = ?",
            String.class,
            account.getId());
    SearchView byPn = find(SearchKey.PN_NO, pn);
    assertThat(byPn.clients())
        .extracting(ClientHitView::code)
        .containsExactly(PlacementTestData.CBG_CLIENT);
    assertThat(arns(byPn)).containsExactly(account.getArn());
    assertThat(byPn.clients().get(0).accounts().get(0).csfStatus()).isEqualTo("AWAITING");
    assertThat(arns(find(SearchKey.APPLICATION_NO, application.toLowerCase(java.util.Locale.ROOT))))
        .containsExactly(account.getArn());
    assertThat(arns(find(SearchKey.ACCOUNT_NO, account.getArn())))
        .containsExactly(account.getArn());
    SearchView none = find(SearchKey.ACCOUNT_NO, "ARN-1999-000000");
    assertThat(none.clients()).isEmpty();
    assertThat(none.legacy()).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from csf_activity where agent = ? and action = 'SEARCH'"
                    + " and detail like ?",
                Long.class,
                AGENT,
                "PN No.: " + pn + "%"))
        .isEqualTo(1L);
  }

  @Test
  void aSearchByNameOrClientIdHasItsRules() {
    SearchView byName = find(SearchKey.NAME, "Pacific Harbor");
    assertThat(byName.clients()).extracting(ClientHitView::code).contains("CL-2026-000003");
    assertThat(find(SearchKey.CLIENT_ID, "CL-2026-000005").clients())
        .extracting(ClientHitView::name)
        .containsExactly("Garcia, Antonio Luis Dizon");
    assertThat(find(SearchKey.CLIENT_ID, "0111-5550105-3").clients())
        .extracting(ClientHitView::code)
        .containsExactly("CL-2026-000005");
    assertThatThrownBy(() -> find(SearchKey.NAME, "Pa"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Enter at least 3 characters");
    assertThatThrownBy(() -> find(SearchKey.PN_NO, "  ")).hasMessage("Enter the value to search");
  }

  @Test
  void theServicingViewShowsEveryAccountWithItsMappedStatus() {
    Account account = placement.awaitingPayment(placement.motor());
    Long clientId = account.getClientId();
    var summary = as.run(AGENT, () -> views.summary(fx.company(), clientId));
    assertThat(summary.code()).isEqualTo(PlacementTestData.CBG_CLIENT);
    assertThat(summary.accounts()).isPositive();
    AccountLine line =
        as.run(AGENT, () -> views.accounts(fx.company(), clientId)).stream()
            .filter(l -> l.arn().equals(account.getArn()))
            .findFirst()
            .orElseThrow();
    assertThat(line.stage()).isEqualTo("AWAITING_PAYMENT");
    assertThat(line.csfStatus()).isEqualTo("AWAITING");
    try {
      jdbc.update(
          "update lov_value set parent_code = 'PENDING' where type_code = 'CSF_STATUS_MAP'"
              + " and code = 'AWAITING_PAYMENT'");
      Objects.requireNonNull(caches.getCache(LovCaches.VALUES)).clear();
      assertThat(
              as.run(AGENT, () -> views.accounts(fx.company(), clientId)).stream()
                  .filter(l -> l.arn().equals(account.getArn()))
                  .findFirst()
                  .orElseThrow()
                  .csfStatus())
          .isEqualTo("PENDING");
    } finally {
      jdbc.update(
          "update lov_value set parent_code = 'AWAITING' where type_code = 'CSF_STATUS_MAP'"
              + " and code = 'AWAITING_PAYMENT'");
      Objects.requireNonNull(caches.getCache(LovCaches.VALUES)).clear();
    }
    var history = as.run(AGENT, () -> views.payments(fx.company(), clientId, null, null));
    assertThat(history.months()).isEqualTo(12);
    assertThatThrownBy(() -> as.run(AGENT, () -> views.payments(fx.company(), clientId, 0, null)))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> as.run(AGENT, () -> views.summary(Long.MAX_VALUE, clientId)))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from csf_activity where action = 'VIEW' and client_id = ?",
                Long.class,
                clientId))
        .isPositive();
  }

  @Test
  void aHeifPhotoIsStoredOnTheAccountAndARenamedExecutableIsRefused() {
    Account account = placement.awaitingPayment(placement.motor());
    Long clientId = account.getClientId();
    byte[] heic = new byte[16];
    System.arraycopy("ftypheic".getBytes(StandardCharsets.US_ASCII), 0, heic, 4, 8);
    DocumentView photo =
        as.run(
            AGENT,
            () ->
                documents.upload(
                    fx.company(),
                    clientId,
                    new Upload(
                        account.getId(),
                        "CLIENT_PHOTO",
                        "Photo of the unit",
                        new UploadedFile("unit.heic", heic))));
    assertThat(photo.recordType()).isEqualTo("Account");
    assertThat(photo.reference()).isEqualTo(account.getArn());
    assertThat(as.run(AGENT, () -> documents.list(fx.company(), clientId)))
        .extracting(DocumentView::id)
        .contains(photo.id());
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        documents.upload(
                            fx.company(),
                            clientId,
                            new Upload(
                                null,
                                "CLIENT_REQUEST",
                                null,
                                new UploadedFile("letter.pdf", new byte[] {'M', 'Z', 0, 0})))))
        .isInstanceOf(BusinessRuleException.class)
        .extracting(e -> ((BusinessRuleException) e).getCode())
        .isEqualTo("ATTACHMENT_CONTENT_MISMATCH");
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        documents.upload(
                            fx.company(),
                            clientId,
                            new Upload(null, "EPOLICY", null, new UploadedFile("e.heic", heic)))))
        .isInstanceOf(BusinessRuleException.class);
    as.run(AGENT, () -> documents.download(fx.company(), clientId, photo.id()));
    assertThat(as.run(AGENT, () -> documents.zip(fx.company(), clientId, List.of(photo.id()))))
        .isNotEmpty();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from csf_activity where client_id = ? and action in ('UPLOAD', 'DOWNLOAD')",
                Long.class,
                clientId))
        .isGreaterThanOrEqualTo(3L);
  }

  @Test
  void aRenewalAdviceIsResentToTheRegisteredEmailAndOnlyASupervisorChangesTheRecipient() {
    Client c = fx.client(true);
    Attachment ra =
        fx.renewalAdvice(new AttachmentTarget("Client", String.valueOf(c.getId())), c.getId());
    assertThat(as.run(AGENT, () -> documents.ofType(fx.company(), c.getId(), "RENEWAL_ADVICE")))
        .extracting(DocumentView::id)
        .containsExactly(ra.getId());
    var preview =
        as.run(AGENT, () -> resends.preview(fx.company(), c.getId(), Kind.RA, ra.getId()));
    assertThat(preview.registeredEmail()).isEqualTo(c.getEmail());
    assertThat(preview.otherAllowed()).isFalse();
    var sent =
        as.run(
            AGENT,
            () ->
                resends.resendAdvice(
                    fx.company(), c.getId(), new ResendRequest(ra.getId(), null, null)));
    assertThat(sent.recipient()).isEqualTo(c.getEmail());
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_outbound where purpose = 'CSF_RESEND_RA' and entity_id = ?",
                Long.class,
                String.valueOf(c.getId())))
        .isGreaterThanOrEqualTo(1L);
    ResendRequest other = new ResendRequest(ra.getId(), "other@csf-client.ph", "Client abroad");
    assertThatThrownBy(
            () -> as.run(AGENT, () -> resends.resendAdvice(fx.company(), c.getId(), other)))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () ->
                as.run(
                    SUPERVISOR,
                    () ->
                        resends.resendAdvice(
                            fx.company(),
                            c.getId(),
                            new ResendRequest(ra.getId(), "other@csf-client.ph", " "))))
        .hasMessage("Enter the reason for sending to another address");
    assertThat(
            as.run(SUPERVISOR, () -> resends.resendAdvice(fx.company(), c.getId(), other))
                .recipient())
        .isEqualTo("other@csf-client.ph");

    Client noEmail = fx.client(false);
    Attachment ra2 =
        fx.renewalAdvice(
            new AttachmentTarget("Client", String.valueOf(noEmail.getId())), noEmail.getId());
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        resends.resendAdvice(
                            fx.company(),
                            noEmail.getId(),
                            new ResendRequest(ra2.getId(), null, null))))
        .hasMessage("The client has no registered e-mail. Update the contact details first");
  }

  @Test
  void onlyAConfirmedEpolicyIsResentThroughTheDispatchService() {
    String arn = placement.placed(placement.liability());
    Long clientId = clientIdOf(PlacementTestData.CORPORATE_CLIENT);
    byte[] pdf =
        composer.pdf(
            new DocumentSpec(
                "Insurer",
                "E-policy",
                arn,
                List.of(new DocumentSpec.Text("Policy", "Account Reference Number: " + arn)),
                List.of(),
                null));
    Epolicy received =
        as.run(
            "proc",
            () -> epolicies.receive(new ReceivedFile(fx.company(), "e.pdf", pdf, arn, null)));
    Long receivedId = received.getId();
    assertThatThrownBy(
            () ->
                as.run(
                    AGENT,
                    () ->
                        resends.resendEpolicy(
                            fx.company(), clientId, new ResendRequest(receivedId, null, null))))
        .hasMessage("The account has no confirmed e-policy");
    String policyNo = "POL-" + CsfFixtures.digits(6);
    as.run("proc", () -> epolicies.confirm(receivedId, List.of(policyNo), null));
    assertThat(as.run(AGENT, () -> views.epolicies(fx.company(), clientId)))
        .filteredOn(e -> e.id().equals(receivedId))
        .singleElement()
        .extracting(EpolicyView::resendable)
        .isEqualTo(true);
    var sent =
        as.run(
            AGENT,
            () ->
                resends.resendEpolicy(
                    fx.company(), clientId, new ResendRequest(receivedId, null, null)));
    assertThat(sent.recipient()).isEqualTo(clients.get(clientId).getEmail());
    assertThat(epolicies.get(receivedId).getDispatchCount()).isEqualTo(1);
  }
}
