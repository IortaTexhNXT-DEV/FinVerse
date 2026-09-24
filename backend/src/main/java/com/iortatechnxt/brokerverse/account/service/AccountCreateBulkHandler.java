package com.iortatechnxt.brokerverse.account.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.Account.Origin;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.service.AccountBulkSupport.Headers;
import com.iortatechnxt.brokerverse.account.service.AccountRules.Resolved;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.ProductRuleService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails;
import com.iortatechnxt.brokerverse.crm.domain.ClientDetails.PersonName;
import com.iortatechnxt.brokerverse.crm.domain.ClientType;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * Bulk account creation (BRNB.039/064/066): one account with one risk item per row, for the product
 * chosen on screen. The client is found by client (or prospect) code, or by name and birth date,
 * and created as a prospect when unknown (BRNB.065). Rows are checked against the minimum fields
 * and the duplicate fall-out, the message naming the existing ARN; accounts are created in DRAFT or
 * submitted directly (parameter {@code submit}).
 */
@Component
public class AccountCreateBulkHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "ACCOUNT_CREATE";

  private static final String CLIENT_ID = "clientId";

  private final AccountService accounts;
  private final AccountRules rules;
  private final AccountChecks checks;
  private final ProductCatalogService catalog;
  private final ProductRuleService productRules;
  private final ClientService clients;
  private final AccountBulkSupport support;

  /**
   * Creates the handler.
   *
   * @param accounts accounts
   * @param rules draft resolution
   * @param checks minimum fields and duplicates
   * @param catalog products
   * @param productRules mandatory documents
   * @param clients clients
   * @param support shared bulk helpers
   */
  public AccountCreateBulkHandler(
      AccountService accounts,
      AccountRules rules,
      AccountChecks checks,
      ProductCatalogService catalog,
      ProductRuleService productRules,
      ClientService clients,
      AccountBulkSupport support) {
    this.accounts = accounts;
    this.rules = rules;
    this.checks = checks;
    this.catalog = catalog;
    this.productRules = productRules;
    this.clients = clients;
    this.support = support;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Bulk account creation";
  }

  @Override
  public String permission() {
    return "ACCOUNT_MAINTAIN";
  }

  @Override
  public String instructions() {
    return "Choose the product (and optionally the market segment) on screen. One row = one"
        + " account with one risk item. Identify the client by Client Code, or give the Client"
        + " Name (individuals as 'Last, First' with their Birth Date; companies by name) to find or"
        + " create a prospect. Rows duplicating a live account are rejected with its ARN.";
  }

  @Override
  public List<BulkColumn> columns() {
    List<BulkColumn> columns = new ArrayList<>();
    columns.add(
        BulkColumn.optional(Headers.CLIENT_CODE, "Client or prospect code", "CL-2026-000012"));
    columns.add(
        BulkColumn.optional(Headers.CLIENT_NAME, "Prospect name when no code", "Dela Cruz, Juan"));
    columns.add(
        AccountBulkSupport.date(Headers.BIRTH_DATE, "Birth date (individual prospect)", false));
    columns.add(BulkColumn.optional(Headers.SEGMENT, "Market segment (default: on screen)", "CBG"));
    columns.add(AccountBulkSupport.date(Headers.PERIOD_FROM, "Period from", true));
    columns.add(AccountBulkSupport.date(Headers.PERIOD_TO, "Period to", true));
    columns.addAll(AccountBulkSupport.itemColumns());
    columns.add(BulkColumn.optional(Headers.INSURER, "Insurer party code", ""));
    columns.add(BulkColumn.optional(Headers.BRANCH, "Insurer branch code (LGT)", ""));
    columns.add(BulkColumn.optional(Headers.MORTGAGEE, "Mortgagee bank (list MORTGAGEE_BANK)", ""));
    columns.add(BulkColumn.optional(Headers.LOAN, "Loan application number", ""));
    columns.add(BulkColumn.optional(Headers.PN, "PN numbers separated by ;", ""));
    columns.add(BulkColumn.optional(Headers.QUOTATION, "Quotation reference", ""));
    columns.add(AccountBulkSupport.date(Headers.FFY_START, "Free First Year start", false));
    columns.add(
        new BulkColumn(
            Headers.DIRECT_PAYMENT,
            "Paid directly to the insurer",
            false,
            BulkColumn.Type.YES_NO,
            "N"));
    return columns;
  }

  @Override
  public String sanitize(String header, String value) {
    return AccountBulkSupport.sanitize(header, BulkImportHandler.super.sanitize(header, value));
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return AccountBulkSupport.riskKey(row);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    return support.safely(() -> check(row, context));
  }

  private List<String> check(BulkRow row, BulkContext context) {
    RiskProduct product = product(context);
    if (AccountBulkSupport.yes(context.parameter(AccountBulkSupport.SUBMIT))
        && !productRules.requiredDocuments(product).isEmpty()) {
      return List.of(
          "Product "
              + product.getCode()
              + " needs documents before submission; upload in DRAFT and submit each account");
    }
    Optional<Client> client = existingClient(row, context);
    Resolved resolved;
    if (client.isPresent()) {
      resolved = rules.resolve(context.companyId(), draft(row, context, client.get().getId()));
    } else {
      requireProspectName(row);
      resolved =
          rules.resolveForNewClient(
              context.companyId(), draft(row, context, null), row.text(Headers.CLIENT_NAME));
    }
    AccountCheck check = checks.checkData(context.companyId(), resolved.data(), product);
    List<String> errors = new ArrayList<>();
    check
        .fieldErrors()
        .forEach(
            (field, message) -> {
              if (!CLIENT_ID.equals(field)) {
                errors.add(message);
              }
            });
    check.duplicates().forEach(d -> errors.add(d.message()));
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    Client client = existingClient(row, context).orElseGet(() -> createProspect(row, context));
    Account account =
        accounts.createDraft(
            new NewAccount(
                context.companyId(),
                null,
                new Origin(row.text(Headers.QUOTATION), null),
                draft(row, context, client.getId()),
                null,
                null));
    if (AccountBulkSupport.yes(context.parameter(AccountBulkSupport.SUBMIT))) {
      accounts.submit(account.getId(), "Bulk upload " + context.jobNo());
    }
    return account.getArn();
  }

  private RiskProduct product(BulkContext context) {
    String code = context.parameter(AccountBulkSupport.PRODUCT);
    if (code == null || code.isBlank()) {
      throw new BusinessRuleException("BULK_PRODUCT_REQUIRED", "Choose the product of the upload");
    }
    return catalog.requireUsableProduct(code);
  }

  private Optional<Client> existingClient(BulkRow row, BulkContext context) {
    String code = row.text(Headers.CLIENT_CODE);
    if (code != null) {
      return Optional.of(clients.requireByCode(context.companyId(), code));
    }
    String name = row.text(Headers.CLIENT_NAME);
    if (name == null) {
      throw new BusinessRuleException(
          "BULK_CLIENT_REQUIRED", "Give the Client Code or the Client Name");
    }
    String display = displayName(name, row.date(Headers.BIRTH_DATE) != null);
    return clients.lookup(context.companyId(), display).stream()
        .filter(c -> String.CASE_INSENSITIVE_ORDER.compare(c.getDisplayName(), display) == 0)
        .filter(
            c ->
                row.date(Headers.BIRTH_DATE) == null
                    || row.date(Headers.BIRTH_DATE).equals(c.getBirthDate()))
        .findFirst();
  }

  private static void requireProspectName(BulkRow row) {
    if (row.date(Headers.BIRTH_DATE) != null && !row.text(Headers.CLIENT_NAME).contains(",")) {
      throw new BusinessRuleException(
          "BULK_CLIENT_NAME_FORMAT", "Enter an individual's name as 'Last, First'");
    }
  }

  private Client createProspect(BulkRow row, BulkContext context) {
    requireProspectName(row);
    String name = row.text(Headers.CLIENT_NAME);
    boolean individual = row.date(Headers.BIRTH_DATE) != null;
    PersonName person =
        individual
            ? new PersonName(
                name.substring(0, name.indexOf(',')).strip(),
                name.substring(name.indexOf(',') + 1).strip(),
                null,
                null,
                null)
            : new PersonName(null, null, null, null, name);
    return clients.createProspect(
        context.companyId(),
        new ClientDetails(
            individual ? ClientType.INDIVIDUAL : ClientType.CORPORATE,
            person,
            row.date(Headers.BIRTH_DATE),
            null,
            null,
            segment(row, context),
            false,
            null));
  }

  private static String displayName(String name, boolean individual) {
    if (!individual || !name.contains(",")) {
      return name.strip();
    }
    return name.substring(0, name.indexOf(',')).strip()
        + ", "
        + name.substring(name.indexOf(',') + 1).strip();
  }

  private static String segment(BulkRow row, BulkContext context) {
    String given = row.text(Headers.SEGMENT);
    return given != null ? given : context.parameter(AccountBulkSupport.SEGMENT);
  }

  private static AccountDraft draft(BulkRow row, BulkContext context, Long clientId) {
    String insurer = row.text(Headers.INSURER);
    return new AccountDraft(
        clientId,
        context.parameter(AccountBulkSupport.PRODUCT),
        segment(row, context),
        "UPLOAD",
        insurer,
        insurer == null ? null : row.text(Headers.BRANCH),
        row.date(Headers.PERIOD_FROM),
        row.date(Headers.PERIOD_TO),
        false,
        1,
        null,
        row.yes(Headers.DIRECT_PAYMENT)
            ? PaymentArrangement.DIRECT_TO_INSURER
            : PaymentArrangement.VIA_BDOI,
        new Mortgage(
            row.text(Headers.MORTGAGEE),
            row.text(Headers.LOAN),
            AccountBulkSupport.list(row.text(Headers.PN))),
        null,
        Stream.of(AccountBulkSupport.item(row)).toList(),
        null,
        null,
        row.date(Headers.FFY_START));
  }
}
