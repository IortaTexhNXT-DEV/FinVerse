package com.iortatechnxt.brokerverse.booking.service;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.SalesStamp;
import com.iortatechnxt.brokerverse.booking.domain.BusinessType;
import com.iortatechnxt.brokerverse.booking.domain.CommissionTerms;
import com.iortatechnxt.brokerverse.booking.domain.InsurerShare;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceDraft;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceFacts;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceFlags;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.booking.domain.PremiumComponents;
import com.iortatechnxt.brokerverse.booking.service.BookingRuleService.RuleFacts;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService;
import com.iortatechnxt.brokerverse.catalog.service.SalesOrganisationService.SalesAssignment;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.dimension.domain.DimensionType;
import com.iortatechnxt.brokerverse.dimension.service.DimensionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Component;

/**
 * Builds the invoices of an account about to be booked (BRNB.027): one per policy year of a
 * multi-year account (BRNB.112), with the premium components, commission, withholding tax, insurer
 * shares, flags and the mandatory cost center (BRNB.108).
 *
 * <p><b>Multi-year choice (Appendix A, Q35):</b> the account premium is the premium of one policy
 * year, so each year is its own invoice under the ARN with that year's policy number: year 1 is
 * booked now and the later years are scheduled and booked by the BOOKING_BATCH job when they start.
 * Commission is therefore recognised per year.
 */
@Component
public class InvoiceBuilder {

  /** Transaction number of the original booking (booking key with the ARN, BRNB.076). */
  public static final String ORIGINAL = "NB";

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final String SHARES_INVALID = "INSURER_SHARES_INVALID";

  private final BookingSettings settings;
  private final BookingRuleService rules;
  private final SalesOrganisationService salesOrganisation;
  private final InsurerService insurers;
  private final DimensionService dimensions;

  /**
   * Creates the builder.
   *
   * @param settings booking parameters
   * @param rules incentive rules
   * @param salesOrganisation default cost center of the account officer
   * @param insurers insurer panel
   * @param dimensions cost center validation
   */
  public InvoiceBuilder(
      BookingSettings settings,
      BookingRuleService rules,
      SalesOrganisationService salesOrganisation,
      InsurerService insurers,
      DimensionService dimensions) {
    this.settings = settings;
    this.rules = rules;
    this.salesOrganisation = salesOrganisation;
    this.insurers = insurers;
    this.dimensions = dimensions;
  }

  /**
   * Transaction number of a policy year.
   *
   * @param year policy year (1 = original booking)
   * @return transaction number
   */
  public static String transactionNo(int year) {
    return year == 1 ? ORIGINAL : ORIGINAL + "-Y" + year;
  }

  /**
   * The invoices of an account, first policy year first.
   *
   * @param account account (POLICY_ISSUED)
   * @param options booking choices
   * @param bookingDate booking date of the first year
   * @return one draft per policy year
   */
  public List<InvoiceDraft> drafts(Account account, BookingOptions options, LocalDate bookingDate) {
    requireBookable(account);
    InvoiceFacts facts = facts(account, options.costCenter());
    PremiumComponents premium = premiumOf(account.getPremium());
    AccountPremium p = account.getPremium();
    CommissionTerms commission =
        CommissionTerms.of(
            p.commissionRate(), p.commission(), p.vatOnCommission(), settings.wtaxRate());
    List<InsurerShare> shares = shares(account, options.shares());
    Long branchId = settings.branch(account.getCompanyId()).id();
    boolean cwt2 =
        options.cwt2Percent() != null
            ? options.cwt2Percent()
            : settings.cwt2Segments().contains(account.getMarketSegment());
    RuleFacts ruleFacts =
        new RuleFacts(
            account.getProductCode(), account.getMarketSegment(), account.getSourceChannel());
    List<InvoiceDraft> drafts = new ArrayList<>();
    int years = Math.max(1, account.getTermYears());
    for (int year = 1; year <= years; year++) {
      LocalDate inception = account.getPeriodFrom().plusYears(year - 1L);
      LocalDate expiry =
          year == years ? account.getPeriodTo() : account.getPeriodFrom().plusYears(year);
      LocalDate flagDate = year == 1 ? bookingDate : inception;
      InvoiceFlags flags =
          new InvoiceFlags(
              account.isDirectPayment(),
              cwt2,
              rules.incentiveEligible(account.getCompanyId(), ruleFacts, flagDate),
              BusinessType.NEW_BUSINESS);
      drafts.add(
          new InvoiceDraft(
              account.getCompanyId(),
              branchId,
              account.getArn(),
              account.getId(),
              transactionNo(year),
              InvoiceKind.BOOKING,
              year,
              policyNo(account, year),
              facts,
              account.getCurrency(),
              inception,
              expiry,
              premium,
              commission,
              flags,
              shares,
              null,
              null));
    }
    return drafts;
  }

  /**
   * Premium components of an Appendix A breakdown: basic = net premium, premium tax or VAT, and any
   * difference to the gross premium as other charges.
   *
   * @param p account premium
   * @return components
   */
  static PremiumComponents premiumOf(AccountPremium p) {
    BigDecimal basic = Money.nz(p.netPremium());
    BigDecimal dst = Money.nz(p.dst());
    BigDecimal taxVat = Money.nz(p.premiumTax()).add(Money.nz(p.vat()));
    BigDecimal lgt = Money.nz(p.lgt());
    BigDecimal fst = Money.nz(p.fst());
    BigDecimal other =
        Money.nz(p.grossPremium())
            .subtract(basic)
            .subtract(dst)
            .subtract(taxVat)
            .subtract(lgt)
            .subtract(fst);
    return new PremiumComponents(basic, dst, taxVat, lgt, fst, other);
  }

  private static void requireBookable(Account account) {
    if (!account.getPremium().isRated()) {
      throw new BusinessRuleException(
          "PREMIUM_NOT_RATED", "Account " + account.getArn() + " has no rated premium");
    }
    if (account.getInsurerCode() == null) {
      throw new BusinessRuleException(
          "ACCOUNT_INSURER_MISSING", "Account " + account.getArn() + " has no insurer");
    }
    if (account.getPeriodFrom() == null || account.getPeriodTo() == null) {
      throw new BusinessRuleException(
          "ACCOUNT_PERIOD_MISSING", "Account " + account.getArn() + " has no period of cover");
    }
  }

  private InvoiceFacts facts(Account account, String costCenterOverride) {
    return new InvoiceFacts(
        account.getClientId(),
        account.getClientCode(),
        account.getClientName(),
        account.getInsurerCode(),
        account.getProductCode(),
        account.getLineCode(),
        account.getMarketSegment(),
        account.getSourceChannel(),
        sales(account, SalesStamp::accountOfficer),
        sales(account, SalesStamp::team),
        sales(account, SalesStamp::department),
        costCenter(account, costCenterOverride));
  }

  /**
   * The cost center: the one chosen, else the account's, else the account officer's team in the
   * sales organisation; mandatory (BRNB.108).
   */
  private String costCenter(Account account, String override) {
    String center = blankToNull(override);
    if (center == null) {
      center = blankToNull(sales(account, SalesStamp::costCenter));
    }
    String officer = sales(account, SalesStamp::accountOfficer);
    if (center == null && officer != null) {
      center =
          salesOrganisation
              .assignmentOf(account.getCompanyId(), officer)
              .map(SalesAssignment::costCenter)
              .orElse(null);
    }
    if (center == null) {
      throw new BusinessRuleException(
          "COST_CENTER_REQUIRED",
          "Enter the cost center of account " + account.getArn() + " (BRNB.108)");
    }
    dimensions.validateOptional(account.getCompanyId(), DimensionType.COST_CENTER, center);
    return center;
  }

  private List<InsurerShare> shares(Account account, List<InsurerShare> requested) {
    if (requested.isEmpty()) {
      return List.of(new InsurerShare(account.getInsurerCode(), HUNDRED));
    }
    Set<String> seen = new HashSet<>();
    BigDecimal total = BigDecimal.ZERO;
    for (InsurerShare share : requested) {
      requireValidShare(share, seen);
      insurers.requireUsableInsurer(account.getCompanyId(), share.insurerCode());
      total = total.add(share.sharePct());
    }
    if (total.compareTo(HUNDRED) != 0 || !seen.contains(account.getInsurerCode())) {
      throw new BusinessRuleException(
          SHARES_INVALID,
          "Insurer shares must add up to 100% and include the lead insurer "
              + account.getInsurerCode());
    }
    return requested;
  }

  private static void requireValidShare(InsurerShare share, Set<String> seen) {
    boolean positive = share.sharePct() != null && share.sharePct().signum() > 0;
    if (!positive || !seen.add(share.insurerCode())) {
      throw new BusinessRuleException(
          SHARES_INVALID, "Each insurer appears once with a positive share");
    }
  }

  private static String policyNo(Account account, int year) {
    List<String> numbers = account.getPolicyNumbers();
    return numbers.size() >= year ? numbers.get(year - 1) : null;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String sales(Account account, Function<SalesStamp, String> value) {
    return Optional.ofNullable(account.getSales()).map(value).orElse(null);
  }
}
