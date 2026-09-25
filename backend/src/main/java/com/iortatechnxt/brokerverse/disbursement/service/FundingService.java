package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.FundingStage;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequest.FundingTerms;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequestRepository;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.service.BankAccountQueryService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Funding of the main BDOIR account through BDO Business Online Banking (DIS 2.17.0-2.17.4, AQ10;
 * workflow {@code DISB_FUNDING}): the maker creates the request ({@code FND-<yyyy>-nnnnnn}), a team
 * leader other than the maker verifies it, two different approvers approve it, and the second
 * approval posts the transfer {@code DISB_FUND_TRANSFER} (target bank against source bank). Return,
 * decline and cancel run from the workflow panel. BOB itself is an external link (DIS 2.17.1); the
 * BOB reference is recorded on the request.
 */
@Service
@Transactional
public class FundingService {

  private final FundingRequestRepository fundings;
  private final BankAccountQueryService banks;
  private final DocumentNumberService numbers;
  private final WorkflowService workflow;
  private final AccountingEventPublisher publisher;
  private final BookRates rates;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param fundings funding requests
   * @param banks bank accounts
   * @param numbers numbers
   * @param workflow funding workflow
   * @param publisher accounting engine
   * @param rates BOOK rates
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public FundingService(
      FundingRequestRepository fundings,
      BankAccountQueryService banks,
      DocumentNumberService numbers,
      WorkflowService workflow,
      AccountingEventPublisher publisher,
      BookRates rates,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.fundings = fundings;
    this.banks = banks;
    this.numbers = numbers;
    this.workflow = workflow;
    this.publisher = publisher;
    this.rates = rates;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Creates a funding request (DIS 2.17.0).
   *
   * @param companyId company
   * @param terms accounts, amount and purpose
   * @return the request
   */
  public FundingRequest create(Long companyId, FundingTerms terms) {
    validate(terms);
    FundingRequest saved =
        fundings.save(
            new FundingRequest(
                companyId,
                numbers.next(DisbursementSettings.series("FND", LocalDate.now(clock))),
                terms));
    workflow.start(
        new StartCase(
            companyId,
            DisbursementSettings.WF_FUNDING,
            new CaseRecord(
                DisbursementSettings.FUNDING,
                saved.getId().toString(),
                saved.getFundingNo(),
                "Funding " + saved.getCurrency() + " " + saved.getAmount(),
                DisbursementSettings.fundingLink(saved.getId()),
                null),
            null));
    audit.record(
        DisbursementSettings.FUNDING,
        saved.getFundingNo(),
        AuditAction.CREATE,
        "Funding " + saved.getCurrency() + " " + saved.getAmount() + ": " + saved.getPurpose());
    return saved;
  }

  /**
   * Changes a request still with the maker.
   *
   * @param id request
   * @param terms terms
   * @return the request
   */
  public FundingRequest update(Long id, FundingTerms terms) {
    validate(terms);
    FundingRequest f = get(id);
    f.update(terms);
    audit.record(DisbursementSettings.FUNDING, f.getFundingNo(), AuditAction.UPDATE, "Updated");
    return f;
  }

  /**
   * Submits the request for verification (DIS 2.17.2).
   *
   * @param id request
   * @return the request
   */
  public FundingRequest submit(Long id) {
    return move(get(id), "submit", null);
  }

  /**
   * A team leader other than the maker verifies it (DIS 2.17.3).
   *
   * @param id request
   * @param comment comment
   * @return the request
   */
  public FundingRequest verify(Long id, String comment) {
    FundingRequest f = get(id);
    f.verifiedBy(currentUser.username());
    return move(f, "verify", comment);
  }

  /**
   * First or second approval (DIS 2.17.4); the second posts the transfer.
   *
   * @param id request
   * @param comment remarks
   * @param bobReference BOB reference of the transfer, may be null
   * @return the request
   */
  public FundingRequest approve(Long id, String comment, String bobReference) {
    FundingRequest f = get(id);
    f.approvedBy(currentUser.username());
    if (bobReference != null && !bobReference.isBlank()) {
      f.bobReference(bobReference.strip());
    }
    move(f, "approve", comment);
    if (f.getStage() == FundingStage.APPROVED) {
      f.posted(post(f));
    }
    return f;
  }

  private String post(FundingRequest f) {
    BankAccount target = banks.get(f.getTargetBankAccountId());
    BankAccount source = banks.get(f.getSourceBankAccountId());
    return publisher
        .publish(
            rates.price(
                new BusinessEvent(
                    DisbursementSettings.EVENT_FUNDING,
                    f.getCompanyId(),
                    target.getBranchId(),
                    f.getValueDate(),
                    f.getCurrency(),
                    DisbursementSettings.MODULE,
                    "FND:" + f.getFundingNo(),
                    f.getBobReference() == null ? f.getFundingNo() : f.getBobReference(),
                    null,
                    null,
                    null,
                    "Funding " + f.getFundingNo() + " - " + f.getPurpose(),
                    Map.of("AMOUNT", f.getAmount()),
                    Map.of(
                        "TARGET_BANK", target.getGlAccountCode(),
                        "SOURCE_BANK", source.getGlAccountCode()))))
        .getBatchNo();
  }

  private FundingRequest move(FundingRequest f, String action, String comment) {
    workflow.transition(
        DisbursementSettings.FUNDING,
        f.getId().toString(),
        action,
        TransitionNote.comment(comment));
    audit.record(
        DisbursementSettings.FUNDING,
        f.getFundingNo(),
        AuditAction.UPDATE,
        action + " -> " + f.getStage() + (comment == null ? "" : ": " + comment));
    return f;
  }

  private void validate(FundingTerms terms) {
    if (!Money.isPositive(terms.amount())) {
      throw new BusinessRuleException("FUNDING_AMOUNT", "The amount must be positive");
    }
    BankAccount source = banks.requireActive(terms.sourceBankAccountId());
    BankAccount target = banks.requireActive(terms.targetBankAccountId());
    if (!source.getCurrency().equals(terms.currency())
        || !target.getCurrency().equals(terms.currency())) {
      throw new BusinessRuleException(
          "FUNDING_CURRENCY", "Both accounts must be in " + terms.currency());
    }
  }

  /**
   * A funding request.
   *
   * @param id id
   * @return request
   */
  @Transactional(readOnly = true)
  public FundingRequest get(Long id) {
    return fundings
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(DisbursementSettings.FUNDING, id));
  }

  /**
   * Funding requests in some stages, newest first.
   *
   * @param companyId company
   * @param stages stages (empty = all)
   * @param pageable page
   * @return requests
   */
  @Transactional(readOnly = true)
  public Page<FundingRequest> list(Long companyId, List<FundingStage> stages, Pageable pageable) {
    List<FundingStage> filter =
        stages == null || stages.isEmpty() ? List.of(FundingStage.values()) : stages;
    return fundings.findByCompanyIdAndStageInOrderByIdDesc(companyId, filter, pageable);
  }
}
