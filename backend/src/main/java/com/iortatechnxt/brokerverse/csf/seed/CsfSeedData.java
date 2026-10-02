package com.iortatechnxt.brokerverse.csf.seed;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.attachment.domain.Attachment;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientRepository;
import com.iortatechnxt.brokerverse.csf.service.CsfCodes;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Stores the renewal advice of the Customer Servicing Facility storyline through the real document
 * services (seed profile only, idempotent; the rest of the storyline is V1940): a renewal advice
 * PDF of the first account of client CL-2026-000001, attached to the account and linked to the
 * client as the Renewal module does (cross-BRD decision D3), so the Renewal Advice tab and the
 * resend have a document.
 */
@Component
@Profile("seed")
@Order(160)
public class CsfSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CsfSeedData.class);
  private static final String CLIENT_CODE = "CL-2026-000001";

  private final CompanyRepository companies;
  private final ClientRepository clients;
  private final AccountQueryService accounts;
  private final DocumentService documents;
  private final DocumentComposer composer;
  private final TransactionTemplate tx;
  private final Clock clock;

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param clients clients
   * @param accounts accounts of the client
   * @param documents document links
   * @param composer PDF composer
   * @param transactions transaction manager
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // the services the seed goes through
  public CsfSeedData(
      CompanyRepository companies,
      ClientRepository clients,
      AccountQueryService accounts,
      DocumentService documents,
      DocumentComposer composer,
      PlatformTransactionManager transactions,
      Clock clock) {
    this.companies = companies;
    this.clients = clients;
    this.accounts = accounts;
    this.documents = documents;
    this.composer = composer;
    this.tx = new TransactionTemplate(transactions);
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    try {
      tx.executeWithoutResult(s -> load());
    } catch (RuntimeException e) {
      LOG.warn("Customer Servicing Facility seed data not loaded: {}", e.getMessage());
    }
  }

  private void load() {
    Company company = companies.findByCode("FVI").orElse(null);
    Client client =
        company == null ? null : clients.findByCode(company.getId(), CLIENT_CODE).orElse(null);
    if (client != null) {
      attachAdvice(company, client);
    }
  }

  private void attachAdvice(Company company, Client client) {
    AttachmentTarget clientTarget =
        new AttachmentTarget(CsfCodes.ENTITY_CLIENT, String.valueOf(client.getId()));
    boolean present =
        documents.all(clientTarget).stream()
            .map(Attachment::getDocumentType)
            .anyMatch(CsfCodes.DOC_RENEWAL_ADVICE::equals);
    List<Account> owned = accounts.byClient(client.getId());
    if (!present && !owned.isEmpty()) {
      store(company, client, clientTarget, owned.get(owned.size() - 1));
    }
  }

  private void store(
      Company company, Client client, AttachmentTarget clientTarget, Account account) {
    LocalDate today = BusinessClock.today(clock);
    byte[] pdf = composer.pdf(spec(company, client, account, today));
    Attachment ra =
        documents
            .upload(
                new AttachmentTarget(CsfCodes.ENTITY_ACCOUNT, String.valueOf(account.getId())),
                List.of(new UploadedFile(account.getArn() + "_RENEWAL_ADVICE.pdf", pdf)),
                new UploadOptions(CsfCodes.DOC_RENEWAL_ADVICE, false, null, "Renewal advice"))
            .get(0);
    documents.link(ra.getId(), List.of(clientTarget));
    LOG.info("Customer Servicing Facility seed data: renewal advice of {}", account.getArn());
  }

  private static DocumentSpec spec(
      Company company, Client client, Account account, LocalDate today) {
    LocalDate expiry = account.getPeriodTo() == null ? today.plusMonths(2) : account.getPeriodTo();
    return new DocumentSpec(
        company.getName(),
        "Renewal Advice",
        account.getArn(),
        List.of(
            new DocumentSpec.Fields(
                "Policy",
                List.of(
                    new DocumentSpec.Field("Client", client.getDisplayName()),
                    new DocumentSpec.Field("Account", account.getArn()),
                    new DocumentSpec.Field("Product", account.getProductCode()),
                    new DocumentSpec.Field("Expiry", DisplayFormat.date(expiry)))),
            new DocumentSpec.Text(
                "Renewal",
                "Your policy expires on "
                    + DisplayFormat.date(expiry)
                    + ". Please confirm the renewal with your account officer or reply to this"
                    + " advice.")),
        List.of(),
        null);
  }
}
