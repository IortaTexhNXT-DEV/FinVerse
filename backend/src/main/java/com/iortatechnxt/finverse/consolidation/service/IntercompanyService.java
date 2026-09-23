package com.iortatechnxt.finverse.consolidation.service;

import com.iortatechnxt.finverse.audit.domain.AuditAction;
import com.iortatechnxt.finverse.audit.service.AuditTrailService;
import com.iortatechnxt.finverse.coa.domain.BalanceSide;
import com.iortatechnxt.finverse.coa.domain.GlAccount;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.finverse.consolidation.api.dto.IntercompanyTransactionRequest;
import com.iortatechnxt.finverse.consolidation.api.dto.RelationshipRequest;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyRelationship;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyRelationship.Side;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyRelationshipRepository;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyTransaction;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyTransactionRepository;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyTransactionType;
import com.iortatechnxt.finverse.consolidation.domain.IntercompanyValues;
import com.iortatechnxt.finverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.finverse.journal.domain.JournalBatch;
import com.iortatechnxt.finverse.journal.domain.JournalType;
import com.iortatechnxt.finverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.finverse.journal.service.SystemJournalService;
import com.iortatechnxt.finverse.organization.domain.Branch;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inter-company relationships and transactions.
 *
 * <p>An inter-company transaction posts two mirror system journals (one per company, same value
 * date, amount and {@code IC-} reference) in one database transaction: both post or neither does.
 */
@Service
@Transactional
public class IntercompanyService {

  /** Source module recorded on inter-company journals. */
  public static final String SOURCE = "INTERCOMPANY";

  private static final String RELATIONSHIP = "Inter-company relationship";

  private final IntercompanyRelationshipRepository relationships;
  private final IntercompanyTransactionRepository transactions;
  private final SystemJournalService journals;
  private final ChartOfAccountsService accounts;
  private final OrganizationService organization;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param relationships relationship repository
   * @param transactions transaction repository
   * @param journals system journal service
   * @param accounts chart of accounts
   * @param organization organization service
   * @param numbers document numbering
   * @param audit audit trail
   */
  public IntercompanyService(
      IntercompanyRelationshipRepository relationships,
      IntercompanyTransactionRepository transactions,
      SystemJournalService journals,
      ChartOfAccountsService accounts,
      OrganizationService organization,
      DocumentNumberService numbers,
      AuditTrailService audit) {
    this.relationships = relationships;
    this.transactions = transactions;
    this.journals = journals;
    this.accounts = accounts;
    this.organization = organization;
    this.numbers = numbers;
    this.audit = audit;
  }

  /**
   * Lists relationships.
   *
   * @param companyId company, or null for all
   * @return relationships
   */
  @Transactional(readOnly = true)
  public List<IntercompanyRelationship> relationships(Long companyId) {
    return companyId == null
        ? relationships.findAllByOrderById()
        : relationships.findInvolving(companyId);
  }

  /**
   * Creates a relationship after checking both companies and their accounts.
   *
   * @param r request
   * @return relationship
   */
  public IntercompanyRelationship createRelationship(RelationshipRequest r) {
    Side a = new Side(r.companyAId(), r.aDueFromAccount().trim(), r.aDueToAccount().trim());
    Side b = new Side(r.companyBId(), r.bDueFromAccount().trim(), r.bDueToAccount().trim());
    requireAccounts(a);
    requireAccounts(b);
    if (!relationships.findPair(a.companyId(), b.companyId()).isEmpty()) {
      throw new DuplicateResourceException(RELATIONSHIP, a.companyId() + "/" + b.companyId());
    }
    IntercompanyRelationship saved = relationships.save(new IntercompanyRelationship(a, b));
    audit.record(
        RELATIONSHIP, saved.getId(), AuditAction.CREATE, "Created inter-company relationship");
    return saved;
  }

  /**
   * Activates or deactivates a relationship.
   *
   * @param id id
   * @param active new state
   * @return relationship
   */
  public IntercompanyRelationship setActive(Long id, boolean active) {
    IntercompanyRelationship rel =
        relationships
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(RELATIONSHIP, id));
    rel.setActive(active);
    audit.record(RELATIONSHIP, id, AuditAction.UPDATE, active ? "Activated" : "Deactivated");
    return rel;
  }

  /**
   * Lists transactions of a company.
   *
   * @param companyId company
   * @return transactions, newest first
   */
  @Transactional(readOnly = true)
  public List<IntercompanyTransaction> transactions(Long companyId) {
    return transactions.findInvolving(companyId);
  }

  /**
   * Posts an inter-company transaction as two mirror journals.
   *
   * @param request request
   * @return recorded transaction
   */
  public IntercompanyTransaction post(IntercompanyTransactionRequest request) {
    IntercompanyValues v = request.values();
    IntercompanyRelationship rel = activePair(v.creditorCompanyId(), v.debtorCompanyId());
    Side creditor = rel.sideOf(v.creditorCompanyId());
    Side debtor = rel.sideOf(v.debtorCompanyId());
    String reference = numbers.next("IC-" + v.valueDate().getYear());
    boolean charge = v.type() == IntercompanyTransactionType.CHARGE;
    Dims dims = new Dims(request.costCenter(), request.businessLine());
    JournalBatch creditorJournal =
        postSide(
            v,
            reference,
            v.creditorCompanyId(),
            List.of(
                line(creditor.dueFromAccount(), charge, v, reference, Dims.NONE),
                line(v.creditorAccount(), !charge, v, reference, dims)));
    JournalBatch debtorJournal =
        postSide(
            v,
            reference,
            v.debtorCompanyId(),
            List.of(
                line(v.debtorAccount(), charge, v, reference, dims),
                line(debtor.dueToAccount(), !charge, v, reference, Dims.NONE)));
    IntercompanyTransaction saved =
        transactions.save(
            new IntercompanyTransaction(
                reference,
                rel.getId(),
                v,
                creditorJournal.getBatchNo(),
                debtorJournal.getBatchNo()));
    audit.record(
        "IntercompanyTransaction",
        reference,
        AuditAction.POST,
        v.type()
            + " "
            + v.currency()
            + " "
            + v.amount().toPlainString()
            + " posted as "
            + creditorJournal.getBatchNo()
            + " / "
            + debtorJournal.getBatchNo());
    return saved;
  }

  private IntercompanyRelationship activePair(Long creditorId, Long debtorId) {
    return relationships.findPair(creditorId, debtorId).stream()
        .filter(IntercompanyRelationship::isActive)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "IC_RELATIONSHIP_MISSING",
                    "No active inter-company relationship exists between the two companies"));
  }

  private JournalBatch postSide(
      IntercompanyValues v, String reference, Long companyId, List<JournalLineRequest> lines) {
    return journals.post(
        new SystemJournalRequest(
            companyId,
            headOffice(companyId).getId(),
            JournalType.CONSOLIDATION,
            v.valueDate(),
            v.currency(),
            "Inter-company " + v.type().name().toLowerCase(Locale.ROOT) + ": " + v.narration(),
            reference,
            SOURCE,
            reference + ":" + companyId,
            lines));
  }

  private static JournalLineRequest line(
      String account, boolean debit, IntercompanyValues v, String reference, Dims dims) {
    return new JournalLineRequest(
        account,
        debit ? BalanceSide.DEBIT : BalanceSide.CREDIT,
        v.amount(),
        v.currency(),
        null,
        null,
        dims.costCenter(),
        dims.businessLine(),
        null,
        reference,
        v.narration());
  }

  /**
   * Head office branch of a company (inter-company journals are booked there).
   *
   * @param companyId company
   * @return head office, or the first active branch
   */
  Branch headOffice(Long companyId) {
    return organization.listBranches(companyId).stream()
        .filter(Branch::isActive)
        .min(Comparator.comparing((Branch b) -> !b.isHeadOffice()).thenComparing(Branch::getCode))
        .orElseThrow(() -> new BusinessRuleException("NO_BRANCH", "Company has no active branch"));
  }

  private void requireAccounts(Side side) {
    organization.requireActiveCompany(side.companyId());
    for (String code : List.of(side.dueFromAccount(), side.dueToAccount())) {
      GlAccount account = accounts.getByCode(side.companyId(), code);
      if (!account.isPostable() || !account.getAccountClass().isBalanceSheet()) {
        throw new BusinessRuleException(
            "INVALID_IC_ACCOUNT", "Account " + code + " must be a postable balance sheet account");
      }
    }
  }

  /** Optional dimensions of the counter-account lines. */
  private record Dims(String costCenter, String businessLine) {
    static final Dims NONE = new Dims(null, null);
  }
}
