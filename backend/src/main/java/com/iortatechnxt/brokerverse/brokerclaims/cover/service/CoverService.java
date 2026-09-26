package com.iortatechnxt.brokerverse.brokerclaims.cover.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.RiskItem;
import com.iortatechnxt.brokerverse.account.domain.SalesStamp;
import com.iortatechnxt.brokerverse.account.service.AccountQueryService;
import com.iortatechnxt.brokerverse.account.service.AccountSearch;
import com.iortatechnxt.brokerverse.booking.domain.BookingEndorsement;
import com.iortatechnxt.brokerverse.booking.service.BookingQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.PremiumRule.InvoiceState;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.PremiumRule.PremiumCheck;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.CoverSnapshot;
import com.iortatechnxt.brokerverse.catalog.domain.RiskItemKind;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The cover of a claim as Claims sees it, read-only (BRCLM.002/003/007/009/016/037/039;
 * CLAIMS_BROKING_DESIGN 3.1): covers found by ARN, policy number or assured with no portfolio or
 * branch restriction (BRCLM.003, CLQ27); a cover's policy years with their policy numbers and
 * periods, its locations, endorsements (cover versions) and ledger invoices; the snapshot a claim
 * keeps; the insurers and shares proposed for a claim; and the premium check of the Operations
 * invoice ledger (BRCLM.001). Claims never changes the account.
 *
 * <p>The cover is the account (ARN) and a policy year of its term (1 = first year), the unit of the
 * invoices and policy numbers (CLQ02).
 */
@Service
@Transactional(readOnly = true)
public class CoverService {

  /** Shortest search text (FR-CL-010). */
  public static final int MIN_SEARCH = 3;

  private static final int MAX_HITS = 50;
  private static final BigDecimal FULL_SHARE = new BigDecimal("100.0000");
  private static final String POLICY_SQL =
      "select distinct a.arn from acc_account a join acc_account_policy p on p.account_id = a.id"
          + " where a.company_id = ? and lower(p.policy_number) like ? order by a.arn limit "
          + MAX_HITS;

  private final AccountQueryService accounts;
  private final BookingQueryService bookings;
  private final InvoiceLedgerQueryService ledger;
  private final SystemParameterService parameters;
  private final JdbcTemplate jdbc;

  /**
   * Creates the service.
   *
   * @param accounts account reads
   * @param bookings endorsements (cover versions)
   * @param ledger Operations invoice ledger
   * @param parameters default currency
   * @param jdbc policy number look-up
   */
  public CoverService(
      AccountQueryService accounts,
      BookingQueryService bookings,
      InvoiceLedgerQueryService ledger,
      SystemParameterService parameters,
      JdbcTemplate jdbc) {
    this.accounts = accounts;
    this.bookings = bookings;
    this.ledger = ledger;
    this.parameters = parameters;
    this.jdbc = jdbc;
  }

  /**
   * Covers matching a search (FR-CL-010): every account of the company, whatever its branch or
   * account officer.
   *
   * @param companyId company
   * @param by what the text is: ARN, POLICY_NO or ASSURED
   * @param text search text, at least {@value #MIN_SEARCH} characters
   * @return accounts, loaded
   */
  public List<Account> search(Long companyId, SearchBy by, String text) {
    String term = text == null ? "" : text.strip();
    if (term.length() < MIN_SEARCH) {
      throw new BusinessRuleException("BCL_SEARCH_TOO_SHORT", "Enter at least 3 characters");
    }
    if (by == SearchBy.POLICY_NO) {
      return jdbc.queryForList(POLICY_SQL, String.class, companyId, like(term)).stream()
          .map(accounts::requireByArn)
          .toList();
    }
    AccountSearch search =
        new AccountSearch(
            companyId, term, null, null, null, null, null, null, null, null, null, null, null, null,
            false);
    return accounts.search(search, PageRequest.of(0, MAX_HITS)).stream()
        .filter(
            a -> by != SearchBy.ARN || a.getArn().toLowerCase(Locale.ROOT).contains(lower(term)))
        .map(a -> accounts.get(a.getId()))
        .toList();
  }

  /**
   * One cover (account), loaded with its items and policy numbers.
   *
   * @param companyId company
   * @param arn account reference number
   * @return the account
   */
  public Account account(Long companyId, String arn) {
    Account account = accounts.requireByArn(arn == null ? "" : arn.strip());
    if (!account.getCompanyId().equals(companyId)) {
      throw new ResourceNotFoundException("Cover", arn);
    }
    return account;
  }

  /**
   * The policy years of an account's term with their policy numbers and periods.
   *
   * @param account account
   * @return years, first first
   */
  public static List<PolicyYear> policyYears(Account account) {
    int years = Math.max(1, account.getTermYears());
    List<String> numbers = account.getPolicyNumbers();
    List<PolicyYear> result = new ArrayList<>();
    for (int year = 1; year <= years; year++) {
      LocalDate from =
          account.getPeriodFrom() == null ? null : account.getPeriodFrom().plusYears(year - 1L);
      LocalDate to =
          year == years || account.getPeriodFrom() == null
              ? account.getPeriodTo()
              : account.getPeriodFrom().plusYears(year);
      String policyNo = numbers.size() >= year ? numbers.get(year - 1) : null;
      result.add(new PolicyYear(year, policyNo, from, to));
    }
    return result;
  }

  /**
   * One policy year of an account.
   *
   * @param account account
   * @param policyYear policy year
   * @return the year
   */
  public static PolicyYear policyYear(Account account, int policyYear) {
    return policyYears(account).stream()
        .filter(y -> y.year() == policyYear)
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "BCL_POLICY_YEAR",
                    "Policy year " + policyYear + " is not in the term of " + account.getArn()));
  }

  /**
   * The insured locations of a cover (risk items of kind location, BRCLM.037).
   *
   * @param account account
   * @return location items by item number
   */
  public static List<RiskItem> locations(Account account) {
    return account.getItems().stream()
        .filter(i -> i.getKind() == RiskItemKind.PROPERTY_LOCATION)
        .toList();
  }

  /**
   * Snapshot of a cover for a claim (BRCLM.003/007/009/016/039).
   *
   * @param account account
   * @param policyYear policy year
   * @param lossDate loss date (cover version at that date)
   * @return snapshot
   */
  public CoverSnapshot snapshot(Account account, int policyYear, LocalDate lossDate) {
    return CoverSnapshot.of(
        policy(account, policyYear),
        version(account.getArn(), policyYear, lossDate),
        sales(account, policyYear));
  }

  /**
   * Current policy facts of a cover (refresh of a claim's snapshot).
   *
   * @param account account
   * @param policyYear policy year
   * @return policy facts
   */
  public CoverSnapshot.Policy policy(Account account, int policyYear) {
    PolicyYear year = policyYear(account, policyYear);
    String currency =
        account.getCurrency() == null
            ? parameters.text(ClaimCodes.PARAM_DEFAULT_CURRENCY, "PHP")
            : account.getCurrency();
    return new CoverSnapshot.Policy(
        account.getArn(),
        account.getId(),
        policyYear,
        year.policyNo(),
        account.getProductCode(),
        account.getLineCode(),
        account.getClientCode(),
        account.getClientName(),
        account.getInsurerCode(),
        year.from(),
        year.to(),
        account.getTotalSumInsured(),
        currency);
  }

  /**
   * Current sales stamp and invoicing branch of a cover (BRCLM.016).
   *
   * @param account account
   * @param policyYear policy year (its invoices give the branch)
   * @return sales facts
   */
  public CoverSnapshot.Sales sales(Account account, int policyYear) {
    SalesStamp stamp = account.getSales();
    Long branch =
        invoices(account.getArn(), policyYear).stream()
            .map(OpsInvoice::getBranchId)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    return stamp == null
        ? new CoverSnapshot.Sales(null, null, null, null, null, branch)
        : new CoverSnapshot.Sales(
            stamp.region(),
            stamp.department(),
            stamp.team(),
            stamp.accountOfficer(),
            stamp.costCenter(),
            branch);
  }

  /**
   * Cover version at a date (BRCLM.039): number of booked endorsements of the policy year effective
   * on or before the date, with the last one; null date gives the latest version.
   *
   * @param arn account reference number
   * @param policyYear policy year
   * @param at date, or null for the latest version
   * @return version
   */
  public CoverSnapshot.Version version(String arn, int policyYear, LocalDate at) {
    List<BookingEndorsement> list =
        bookings.endorsements(arn).stream()
            .filter(e -> e.getPolicyYear() == policyYear)
            .filter(e -> at == null || !e.getEffectiveDate().isAfter(at))
            .sorted(Comparator.comparing(BookingEndorsement::getEffectiveDate))
            .toList();
    if (list.isEmpty()) {
      return new CoverSnapshot.Version(0, null, null);
    }
    BookingEndorsement last = list.get(list.size() - 1);
    return new CoverSnapshot.Version(list.size(), last.getEndorsementNo(), last.getEffectiveDate());
  }

  /**
   * Endorsements of a cover (Cover Lookup).
   *
   * @param arn account reference number
   * @return endorsements, oldest first
   */
  public List<BookingEndorsement> endorsements(String arn) {
    return bookings.endorsements(arn);
  }

  /**
   * Ledger invoices of a cover and policy year, cancelled ones included.
   *
   * @param arn account reference number
   * @param policyYear policy year
   * @return invoices, oldest first
   */
  public List<OpsInvoice> invoices(String arn, int policyYear) {
    return ledger.forArn(arn).stream().filter(i -> i.getPolicyYear() == policyYear).toList();
  }

  /**
   * Every ledger invoice of a cover (Cover Lookup).
   *
   * @param arn account reference number
   * @return invoices, oldest first
   */
  public List<OpsInvoice> invoices(String arn) {
    return ledger.forArn(arn);
  }

  /**
   * Premium check of a cover and policy year (BRCLM.001).
   *
   * @param arn account reference number
   * @param policyYear policy year
   * @return result with the invoices not fully paid
   */
  public PremiumCheck premium(String arn, int policyYear) {
    return PremiumRule.evaluate(
        invoices(arn, policyYear).stream().map(CoverService::state).toList());
  }

  /**
   * Insurers and shares proposed for a claim (BRCLM.043): the shares of the first live invoice of
   * the policy year, else the lead insurer of the account at 100 %.
   *
   * @param account account
   * @param policyYear policy year
   * @return shares, lead first
   */
  public List<OpsInvoiceShare> shares(Account account, int policyYear) {
    return invoices(account.getArn(), policyYear).stream()
        .filter(i -> !i.isCancelled() && !i.getShares().isEmpty())
        .findFirst()
        .map(i -> i.getShares().stream().sorted(Comparator.comparing(s -> !s.lead())).toList())
        .orElseGet(
            () ->
                account.getInsurerCode() == null
                    ? List.of()
                    : List.of(new OpsInvoiceShare(account.getInsurerCode(), FULL_SHARE, true)));
  }

  private static InvoiceState state(OpsInvoice i) {
    return new InvoiceState(
        i.getInvoiceNo(),
        i.getKind().name(),
        i.getPaymentStatus(),
        i.isDpFlag(),
        i.isCancelled(),
        i.premiumBalance(),
        i.getCurrency());
  }

  private static String lower(String text) {
    return text.toLowerCase(Locale.ROOT);
  }

  private static String like(String text) {
    return "%" + lower(text) + "%";
  }

  /** What a cover search text is (FR-CL-010). */
  public enum SearchBy {
    /** Account reference number. */
    ARN,
    /** Policy number of any policy year. */
    POLICY_NO,
    /** Assured (client) name or code. */
    ASSURED
  }

  /**
   * A policy year of a cover.
   *
   * @param year policy year of the term (1 = first year)
   * @param policyNo policy number, null while not issued
   * @param from start of the year
   * @param to end of the year
   */
  public record PolicyYear(int year, String policyNo, LocalDate from, LocalDate to) {}
}
