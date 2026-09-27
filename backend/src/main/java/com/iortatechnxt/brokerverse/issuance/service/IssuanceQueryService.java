package com.iortatechnxt.brokerverse.issuance.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountSearch;
import com.iortatechnxt.brokerverse.issuance.domain.Epolicy;
import com.iortatechnxt.brokerverse.issuance.domain.EpolicyRepository;
import com.iortatechnxt.brokerverse.issuance.domain.EpolicyStatus;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdvice;
import com.iortatechnxt.brokerverse.issuance.domain.InsuranceAdviceRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issuance reads: the policy record of an account (contract {@link #policyFor}), the Issuance
 * Workbench tabs and their counts.
 */
@Service
@Transactional(readOnly = true)
public class IssuanceQueryService {

  private static final List<EpolicyStatus> TO_REVIEW =
      List.of(EpolicyStatus.RECEIVED, EpolicyStatus.REVIEW);
  private static final List<AccountStatus> ADVISABLE =
      List.of(AccountStatus.PLACED, AccountStatus.POLICY_ISSUED, AccountStatus.BOOKED);
  private static final int SCAN_PAGE = 500;
  private static final int MAX_SCAN = 2000;

  private final EpolicyRepository epolicies;
  private final InsuranceAdviceRepository advices;
  private final AccountQueryService accounts;

  /**
   * Creates the service.
   *
   * @param epolicies e-policies
   * @param advices insurance advices
   * @param accounts account reads
   */
  public IssuanceQueryService(
      EpolicyRepository epolicies,
      InsuranceAdviceRepository advices,
      AccountQueryService accounts) {
    this.epolicies = epolicies;
    this.advices = advices;
    this.accounts = accounts;
  }

  /**
   * The policy record of an account (contract for booking and reports): policy numbers and issue
   * date on the account, e-policies received and Insurance Advices.
   *
   * @param arn Account Reference Number
   * @return policy record
   */
  public PolicyRecord policyFor(String arn) {
    Account account = accounts.requireByArn(arn);
    return new PolicyRecord(
        arn,
        account.getStatus(),
        account.getPolicyNumbers(),
        account.getLifecycle().getPolicyIssueDate(),
        epolicies.findByArnOrderByIdDesc(arn),
        advices.findByArnOrderByIdDesc(arn));
  }

  /**
   * One page of a workbench tab.
   *
   * @param companyId company
   * @param tab tab
   * @param text ARN (or client for account tabs) fragment
   * @param pageable page
   * @return rows
   */
  public Page<IssuanceRow> workbench(
      Long companyId, IssuanceTab tab, String text, Pageable pageable) {
    String arn = text == null ? "" : text.strip();
    return switch (tab) {
      case AWAITING_POLICY ->
          accounts
              .search(search(companyId, List.of(AccountStatus.PLACED), arn), pageable)
              .map(IssuanceQueryService::accountRow);
      case REVIEW ->
          epolicies
              .findByCompanyIdAndStatusInAndArnContainingIgnoreCaseOrderByIdAsc(
                  companyId, TO_REVIEW, arn, pageable)
              .map(this::row);
      case READY_TO_DISPATCH ->
          epolicies
              .findByCompanyIdAndStatusAndDispatchCountAndArnContainingIgnoreCaseOrderByIdAsc(
                  companyId, EpolicyStatus.CONFIRMED, 0, arn, pageable)
              .map(this::row);
      case IA_TO_GENERATE -> adviceCandidates(companyId, arn, pageable);
    };
  }

  /**
   * Counts of the workbench tiles.
   *
   * @param companyId company
   * @return counts
   */
  public IssuanceCounts counts(Long companyId) {
    return new IssuanceCounts(
        accounts
            .search(search(companyId, List.of(AccountStatus.PLACED), null), PageRequest.of(0, 1))
            .getTotalElements(),
        epolicies.countByCompanyIdAndStatusIn(companyId, TO_REVIEW),
        epolicies.countByCompanyIdAndStatusAndDispatchCount(companyId, EpolicyStatus.CONFIRMED, 0),
        withoutAdvice(companyId, null).size());
  }

  private Page<IssuanceRow> adviceCandidates(Long companyId, String text, Pageable pageable) {
    List<Account> all = withoutAdvice(companyId, text);
    int from = (int) Math.min(pageable.getOffset(), all.size());
    int to = Math.min(from + pageable.getPageSize(), all.size());
    return new PageImpl<>(
        all.subList(from, to).stream().map(IssuanceQueryService::accountRow).toList(),
        pageable,
        all.size());
  }

  private List<Account> withoutAdvice(Long companyId, String text) {
    List<Account> mortgaged = new ArrayList<>();
    AccountSearch search = search(companyId, ADVISABLE, text);
    int page = 0;
    Page<Account> slice;
    do {
      slice = accounts.search(search, PageRequest.of(page++, SCAN_PAGE, Sort.by("id")));
      slice.getContent().stream().filter(a -> a.getMortgageeBank() != null).forEach(mortgaged::add);
    } while (slice.hasNext() && page * SCAN_PAGE < MAX_SCAN);
    if (mortgaged.isEmpty()) {
      return mortgaged;
    }
    Set<Long> advised = advices.withAdvice(mortgaged.stream().map(Account::getId).toList());
    return mortgaged.stream().filter(a -> !advised.contains(a.getId())).toList();
  }

  private static IssuanceRow accountRow(Account account) {
    account.getPolicyNumbers();
    return new IssuanceRow(account, null, null);
  }

  private IssuanceRow row(Epolicy e) {
    return new IssuanceRow(accounts.requireByArn(e.getArn()), e, null);
  }

  private static AccountSearch search(Long companyId, List<AccountStatus> statuses, String text) {
    return new AccountSearch(
        companyId,
        text == null || text.isBlank() ? null : text,
        null,
        null,
        null,
        null,
        null,
        null,
        statuses,
        null,
        null,
        null,
        null,
        null,
        false);
  }

  /**
   * The policy record of an account.
   *
   * @param arn Account Reference Number
   * @param status account status
   * @param policyNumbers policy numbers (one per year)
   * @param issueDate issue date
   * @param epolicies e-policies received, newest first
   * @param advices Insurance Advices, newest first
   */
  public record PolicyRecord(
      String arn,
      AccountStatus status,
      List<String> policyNumbers,
      LocalDate issueDate,
      List<Epolicy> epolicies,
      List<InsuranceAdvice> advices) {

    /** Defensive copies. */
    public PolicyRecord {
      policyNumbers = List.copyOf(policyNumbers);
      epolicies = List.copyOf(epolicies);
      advices = List.copyOf(advices);
    }
  }

  /**
   * A workbench row.
   *
   * @param account account
   * @param epolicy e-policy of the row (review and dispatch tabs), may be null
   * @param advice advice, may be null
   */
  public record IssuanceRow(Account account, Epolicy epolicy, InsuranceAdvice advice) {}

  /**
   * Workbench tile counts.
   *
   * @param awaitingPolicy placed, awaiting the e-policy
   * @param toReview e-policies to review
   * @param readyToDispatch e-policies to send to the client
   * @param adviceToGenerate mortgaged accounts without an Insurance Advice
   */
  public record IssuanceCounts(
      long awaitingPolicy, long toReview, long readyToDispatch, long adviceToGenerate) {}
}
