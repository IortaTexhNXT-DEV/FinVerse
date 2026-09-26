package com.iortatechnxt.brokerverse.reserves.seed;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.reserves.domain.ReserveParameter;
import com.iortatechnxt.brokerverse.reserves.domain.TakafulTerms;
import com.iortatechnxt.brokerverse.reserves.domain.ValuationRun;
import com.iortatechnxt.brokerverse.reserves.service.ReserveParameterService;
import com.iortatechnxt.brokerverse.reserves.service.TakafulSettingService;
import com.iortatechnxt.brokerverse.reserves.service.ValuationRunService;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.service.ProductService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * SEED PROFILE ONLY: actuarial reserves of the seed company FVI, created through the services after
 * the operational seed data (underwriting 10, claims 20, reinsurance 30, payables 40, planning 60,
 * assets 70):
 *
 * <ul>
 *   <li>authorized reserve parameters for every line of business of the seed products, mixing the
 *       rate and chain-ladder IBNR methods;
 *   <li>takaful surplus enabled for the personal accident product;
 *   <li>valuation runs January to August 2026 prepared by the accountant, approved and posted by
 *       the finance manager, and the September run submitted and waiting for approval.
 * </ul>
 *
 * <p>Idempotent (existing parameters and runs are kept) and independent of the claims and
 * reinsurance modules: without them OSLR, chain-ladder IBNR and the reinsurers' shares are zero.
 */
@Component
@Profile("seed")
@Order(90)
public class ReservesSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(ReservesSeedData.class);
  private static final String SEED_COMPANY = "FVI";
  private static final String MAKER = "accountant";
  private static final String CHECKER = "fmanager";
  private static final LocalDate EFFECTIVE = LocalDate.of(2026, 1, 1);
  private static final YearMonth FIRST_MONTH = YearMonth.of(2026, 1);
  private static final YearMonth PENDING_MONTH = YearMonth.of(2026, 9);
  private static final String TAKAFUL_PRODUCTS = "PA-IND";
  private static final String COST_CENTER = "UW";

  private final OrganizationService organization;
  private final ProductService products;
  private final ReserveParameterService parameters;
  private final TakafulSettingService takaful;
  private final ValuationRunService runs;
  private final UserDetailsService users;

  /**
   * Creates the loader.
   *
   * @param organization companies
   * @param products underwriting products (lines of business)
   * @param parameters reserve parameters
   * @param takaful takaful settings
   * @param runs valuation runs
   * @param users SIT/UAT users (maker and checker)
   */
  public ReservesSeedData(
      OrganizationService organization,
      ProductService products,
      ReserveParameterService parameters,
      TakafulSettingService takaful,
      ValuationRunService runs,
      UserDetailsService users) {
    this.organization = organization;
    this.products = products;
    this.parameters = parameters;
    this.takaful = takaful;
    this.runs = runs;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    Optional<Company> company =
        organization.listCompanies().stream()
            .filter(c -> SEED_COMPANY.equals(c.getCode()))
            .findFirst();
    if (company.isEmpty()) {
      LOG.info("Reserves seed data skipped: company {} not found", SEED_COMPANY);
      return;
    }
    load(company.get().getId());
  }

  /**
   * Loads the seed reserves of a company.
   *
   * @param companyId company
   */
  public void load(Long companyId) {
    Authentication previous = SecurityContextHolder.getContext().getAuthentication();
    try {
      parameters(companyId);
      takaful(companyId);
      for (YearMonth m = FIRST_MONTH; m.isBefore(PENDING_MONTH); m = m.plusMonths(1)) {
        postedRun(companyId, m.atEndOfMonth());
      }
      pendingRun(companyId, PENDING_MONTH.atEndOfMonth());
      LOG.info("Actuarial reserves seed data ready");
    } catch (BusinessRuleException ex) {
      LOG.warn("Reserves seed data incomplete: {}", ex.getMessage());
    } finally {
      SecurityContextHolder.getContext().setAuthentication(previous);
    }
  }

  private void parameters(Long companyId) {
    if (!parameters.list(companyId).isEmpty()) {
      return;
    }
    Set<String> lines = new TreeSet<>(SeedReserveParameters.BY_LINE.keySet());
    products.list(companyId).stream().map(Product::getBusinessLine).forEach(lines::add);
    for (String line : lines) {
      signIn(MAKER);
      ReserveParameter p =
          parameters.create(
              companyId,
              line,
              EFFECTIVE,
              SeedReserveParameters.BY_LINE.getOrDefault(
                  line, SeedReserveParameters.BY_LINE.get("PA")));
      signIn(CHECKER);
      parameters.authorize(p.getId());
    }
  }

  private void takaful(Long companyId) {
    if (takaful.find(companyId).isPresent()) {
      return;
    }
    signIn(MAKER);
    takaful.save(
        companyId,
        new TakafulTerms(
            true, TAKAFUL_PRODUCTS, new BigDecimal("70"), new BigDecimal("5"), COST_CENTER));
    signIn(CHECKER);
    takaful.authorize(companyId);
  }

  private void postedRun(Long companyId, LocalDate date) {
    if (runs.forMonth(companyId, date).isPresent()) {
      return;
    }
    ValuationRun run = prepare(companyId, date);
    signIn(CHECKER);
    runs.approve(run.getId());
    runs.post(run.getId());
  }

  private void pendingRun(Long companyId, LocalDate date) {
    if (runs.forMonth(companyId, date).isEmpty()) {
      prepare(companyId, date);
    }
  }

  private ValuationRun prepare(Long companyId, LocalDate date) {
    signIn(MAKER);
    ValuationRun run = runs.create(companyId, date);
    return runs.submit(run.getId());
  }

  private void signIn(String username) {
    UserDetails details = users.loadUserByUsername(username);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities()));
  }
}
