package com.iortatechnxt.brokerverse.quotation.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.Account.Origin;
import com.iortatechnxt.brokerverse.account.domain.AccountData.Mortgage;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.account.service.NewAccount;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.PeriodBasis;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.quotation.domain.Quotation;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationItem;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Client acceptance and conversion into accounts (BRNB.045/102, MKTID.011). Acceptance needs the
 * client's acceptance e-mail attached (document type CLIENT_ACCEPTANCE) and may cover only some
 * risk groups. Conversion creates one draft account per accepted risk group through {@code
 * AccountService.createDraft}, for a confirmed client (BRNB.029), with the quotation's premium,
 * insurer and direct-payment flag.
 *
 * <p>ARN convention: a quotation that yields one account passes its own ARN to the account; one
 * that yields several accounts passes {@code <ARN>-01}, {@code <ARN>-02}... in risk-group order, so
 * every account still carries the quotation's ARN (BRNB.102).
 */
@Service
@Transactional
public class QuotationAcceptanceService {

  /** Document type of the client's acceptance e-mail. */
  public static final String ACCEPTANCE_DOCUMENT = "CLIENT_ACCEPTANCE";

  private final QuotationService service;
  private final QuotationVersions versions;
  private final QuotationPricing pricing;
  private final ProductCatalogService catalog;
  private final ClientService clients;
  private final AccountService accounts;
  private final DocumentService documents;
  private final WorkflowService workflow;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param service quotation reads
   * @param versions version store
   * @param pricing premium per risk group
   * @param catalog products
   * @param clients clients
   * @param accounts account creation
   * @param documents documents of the quotation
   * @param workflow workflow engine
   * @param audit audit trail
   * @param clock clock
   */
  public QuotationAcceptanceService(
      QuotationService service,
      QuotationVersions versions,
      QuotationPricing pricing,
      ProductCatalogService catalog,
      ClientService clients,
      AccountService accounts,
      DocumentService documents,
      WorkflowService workflow,
      AuditTrailService audit,
      Clock clock) {
    this.service = service;
    this.versions = versions;
    this.pricing = pricing;
    this.catalog = catalog;
    this.clients = clients;
    this.accounts = accounts;
    this.documents = documents;
    this.workflow = workflow;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Records the client's acceptance (BRNB.045).
   *
   * @param id quotation
   * @param groups accepted risk groups; empty for all
   * @param comment comment
   * @return the quotation, now ACCEPTED
   */
  public Quotation accept(Long id, List<Integer> groups, String comment) {
    boolean attached =
        documents
            .documentTypesOf(new AttachmentTarget(QuotationService.ENTITY, String.valueOf(id)))
            .contains(ACCEPTANCE_DOCUMENT);
    if (!attached) {
      throw new BusinessRuleException(
          "ACCEPTANCE_EMAIL_REQUIRED",
          "Attach the client's acceptance e-mail (document type Client acceptance e-mail) first");
    }
    return acceptListed(id, groups, comment);
  }

  /**
   * Records an acceptance listed in a bulk upload (BRNB.024): the uploaded list, kept with the bulk
   * job, is the evidence; the comment names the job.
   *
   * @param id quotation
   * @param groups accepted risk groups; empty for all
   * @param comment comment naming the upload
   * @return the quotation, now ACCEPTED
   */
  public Quotation acceptListed(Long id, List<Integer> groups, String comment) {
    Quotation q = service.get(id);
    List<Integer> offered = versions.current(q).groups();
    List<Integer> accepted =
        groups == null || groups.isEmpty() ? offered : groups.stream().distinct().sorted().toList();
    if (!offered.containsAll(accepted)) {
      throw new BusinessRuleException(
          "ACCEPTANCE_GROUP_UNKNOWN", "The quotation offers risk groups " + offered + " only");
    }
    workflow.transition(
        QuotationService.ENTITY, String.valueOf(id), "accept", TransitionNote.comment(comment));
    q.markAccepted(accepted, clock.instant());
    audit.record(
        QuotationService.ENTITY,
        q.getQuotationNo(),
        AuditAction.UPDATE,
        "Accepted by the client, risk group(s) " + accepted);
    return q;
  }

  /**
   * Creates the accounts of an accepted quotation: one per accepted risk group.
   *
   * @param id quotation
   * @param comment comment
   * @return the quotation, now CONVERTED, with the account ARNs
   */
  public Quotation createAccounts(Long id, String comment) {
    Quotation q = service.get(id);
    clients.requireConfirmed(q.getClientId());
    QuotationContent content = versions.current(q);
    RiskProduct product = catalog.requireUsableProduct(q.getProductCode());
    List<Integer> groups = q.getAcceptedGroupList();
    if (groups.isEmpty()) {
      throw new BusinessRuleException(
          "QUOTATION_NOT_ACCEPTED", "Record the client's acceptance first");
    }
    List<String> arns = new ArrayList<>();
    for (int index = 0; index < groups.size(); index++) {
      int group = groups.get(index);
      String arn =
          groups.size() == 1
              ? q.getArn()
              : q.getArn() + String.format(Locale.ROOT, "-%02d", index + 1);
      Account account =
          accounts.createDraft(
              new NewAccount(
                  q.getCompanyId(),
                  arn,
                  new Origin(q.getQuotationNo(), null),
                  draft(q, content, group),
                  pricing.rateGroup(q.getCompanyId(), product, content, group),
                  q.getCreatedBy()));
      arns.add(account.getArn());
    }
    q.linkAccounts(arns);
    workflow.transition(
        QuotationService.ENTITY,
        String.valueOf(id),
        "create_accounts",
        TransitionNote.comment(
            "Accounts " + String.join(", ", arns) + (comment == null ? "" : " - " + comment)));
    audit.record(
        QuotationService.ENTITY,
        q.getQuotationNo(),
        AuditAction.UPDATE,
        "Accounts created: " + String.join(", ", arns));
    return q;
  }

  private static AccountDraft draft(Quotation q, QuotationContent c, int group) {
    return new AccountDraft(
        q.getClientId(),
        q.getProductCode(),
        q.getMarketSegment(),
        q.getSourceChannel(),
        c.insurerCode(),
        c.insurerBranch(),
        c.periodFrom(),
        c.periodTo(),
        false,
        1,
        q.getCurrency(),
        c.directPayment() ? PaymentArrangement.DIRECT_TO_INSURER : PaymentArrangement.VIA_BDOI,
        Mortgage.NONE,
        null,
        c.itemsOf(group).stream().map(QuotationItem::data).toList(),
        c.ratingBasis() == null ? null : PeriodBasis.valueOf(c.ratingBasis()),
        null,
        null);
  }
}
