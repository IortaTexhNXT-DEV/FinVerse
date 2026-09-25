package com.iortatechnxt.brokerverse.productmaint.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProduct;
import com.iortatechnxt.brokerverse.catalog.service.InsurerService;
import com.iortatechnxt.brokerverse.catalog.service.ProductCatalogService;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec.PackageDates;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequestRepository;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageTerms.Dates;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestScope;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Package expiry and renewal (BRPM.017, BRPM.006): the list of released packages by package end
 * date with their renewal status, the bulk "Generate Renewal Request" (a RENEW request pre-filled
 * from the version in force, dates rolled one term forward, no re-keying), and the daily monitor of
 * the PACKAGE_EXPIRY_MONITOR job: PACKAGE_EXPIRING alerts at the notice period and the reminder
 * days, and renewal requests drafted automatically when PACKAGE_RENEWAL_AUTODRAFT is on (PQ11).
 */
@Service
@Transactional
public class PackageExpiryService {

  /** Exception code of the expiry alert (V817). */
  public static final String ALERT = "PACKAGE_EXPIRING";

  private static final int DEFAULT_NOTICE_DAYS = 60;
  private static final Set<RequestStage> OPEN =
      EnumSet.complementOf(EnumSet.copyOf(RequestStage.CLOSED));

  private final ProductVersionQueryService versions;
  private final PackageRequestRepository requests;
  private final PackageRequestService requestService;
  private final ProductCatalogService catalog;
  private final InsurerService insurers;
  private final AlertService alerts;
  private final SystemParameterService parameters;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param versions catalog package versions
   * @param requests package requests
   * @param requestService request creation
   * @param catalog products
   * @param insurers insurer panel of the company
   * @param alerts exception alerts
   * @param parameters business parameters
   * @param clock clock
   */
  public PackageExpiryService(
      ProductVersionQueryService versions,
      PackageRequestRepository requests,
      PackageRequestService requestService,
      ProductCatalogService catalog,
      InsurerService insurers,
      AlertService alerts,
      SystemParameterService parameters,
      Clock clock) {
    this.versions = versions;
    this.requests = requests;
    this.requestService = requestService;
    this.catalog = catalog;
    this.insurers = insurers;
    this.alerts = alerts;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * Released packages ending within a number of days, soonest first, with their renewal status.
   *
   * @param companyId company
   * @param withinDays look-ahead in days
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<ExpiringPackage> expiring(Long companyId, int withinDays) {
    return expiring(versions, companyId, withinDays);
  }

  /**
   * The same list read through another version source (the job passes the one it was built with).
   *
   * @param source version reads
   * @param companyId company
   * @param withinDays look-ahead in days
   * @return rows
   */
  @Transactional(readOnly = true)
  public List<ExpiringPackage> expiring(
      ProductVersionQueryService source, Long companyId, int withinDays) {
    LocalDate today = LocalDate.now(clock);
    return source.packagesExpiring(companyId, Math.max(0, withinDays)).stream()
        .map(
            v ->
                new ExpiringPackage(
                    v,
                    ChronoUnit.DAYS.between(today, v.dates().packageEndDate()),
                    openRenewal(v.productCode()).orElse(null)))
        .toList();
  }

  /**
   * Generates RENEW requests for packages (bulk "Generate Renewal Request", BRPM.017); a package
   * that already has an open renewal request keeps it.
   *
   * @param companyId company
   * @param productCodes packages to renew
   * @return one result per package
   */
  public List<RenewalResult> generateRenewals(Long companyId, List<String> productCodes) {
    if (productCodes == null || productCodes.isEmpty()) {
      throw new BusinessRuleException("PKG_RENEWAL_NONE", "Select the packages to renew");
    }
    List<RenewalResult> results = new ArrayList<>();
    for (String code : productCodes.stream().distinct().toList()) {
      ProductVersionView v =
          versions
              .current(code)
              .orElseThrow(() -> new ResourceNotFoundException("Released package", code));
      results.add(renew(companyId, v));
    }
    return results;
  }

  /**
   * Drafts the RENEW request of one version unless an open renewal exists; insurers that are not
   * active on the company's panel are left out of the pre-filled terms.
   *
   * @param companyId company
   * @param v version in force
   * @return result
   */
  RenewalResult renew(Long companyId, ProductVersionView v) {
    Optional<PackageRequest> open = openRenewal(v.productCode());
    if (open.isPresent()) {
      return new RenewalResult(v.productCode(), open.get(), false);
    }
    RiskProduct product = catalog.requireProduct(v.productCode());
    PackageTerms version = VersionTerms.of(v);
    Set<String> usable =
        insurers.insurers(companyId).stream()
            .filter(InsurerProfile::isActive)
            .map(InsurerProfile::getPartyCode)
            .collect(Collectors.toSet());
    PackageTerms current =
        version.withInsurers(
            version.insurers().stream().filter(i -> usable.contains(i.insurerCode())).toList());
    PackageRequest created =
        requestService.create(
            companyId,
            new RequestDraft(
                RequestType.RENEW,
                RequestScope.GENERIC,
                v.productName() + " renewal",
                null,
                product.getLineCode(),
                product.getCoverTypeCode(),
                v.productCode(),
                v.versionNo(),
                product.getMarketSegmentList(),
                "PACKAGE_EXPIRY",
                "Package ends on " + v.dates().packageEndDate(),
                null,
                current.withScheme(current.scheme(), rolled(v.dates()))));
    return new RenewalResult(v.productCode(), created, true);
  }

  /**
   * The dates of the next package term: it starts the day after the current end and lasts as long
   * as the current term (one year when unknown).
   *
   * @param d current dates
   * @return next dates
   */
  static Dates rolled(PackageDates d) {
    LocalDate end = d.packageEndDate();
    LocalDate start = end.plusDays(1);
    long days =
        d.packageStartDate() == null ? 0 : ChronoUnit.DAYS.between(d.packageStartDate(), end);
    LocalDate nextEnd = days > 0 ? start.plusDays(days) : start.plusYears(1).minusDays(1);
    LocalDate anniversary = d.anniversaryDate() == null ? null : d.anniversaryDate().plusYears(1);
    return new Dates(start, start, nextEnd, anniversary);
  }

  /**
   * The open renewal (or reactivation) request of a package.
   *
   * @param productCode product
   * @return newest open request
   */
  @Transactional(readOnly = true)
  public Optional<PackageRequest> openRenewal(String productCode) {
    return requests
        .findByTargetProductCodeAndRequestTypeInAndStatusInOrderByIdDesc(
            productCode, EnumSet.of(RequestType.RENEW, RequestType.REACTIVATE), OPEN)
        .stream()
        .findFirst();
  }

  /**
   * The daily monitor of one company (job PACKAGE_EXPIRY_MONITOR): alerts at the notice period and
   * the reminder days, and drafts renewal requests when PACKAGE_RENEWAL_AUTODRAFT is on.
   *
   * @param source version reads
   * @param companyId company
   * @return alerts raised and renewals drafted
   */
  public MonitorRun monitor(ProductVersionQueryService source, Long companyId) {
    int notice = parameters.intValue("PACKAGE_EXPIRY_NOTICE_DAYS", DEFAULT_NOTICE_DAYS);
    boolean autodraft = Boolean.parseBoolean(parameters.text("PACKAGE_RENEWAL_AUTODRAFT", "false"));
    List<Integer> reminders = reminderDays();
    int alerted = 0;
    int drafted = 0;
    for (ExpiringPackage e : expiring(source, companyId, notice)) {
      int bucket = bucket(e.daysLeft(), notice, reminders);
      if (alert(companyId, e, bucket)) {
        alerted++;
      }
      if (autodraft && e.renewal() == null && renew(companyId, e.version()).created()) {
        drafted++;
      }
    }
    return new MonitorRun(alerted, drafted);
  }

  private List<Integer> reminderDays() {
    List<Integer> days = new ArrayList<>();
    for (String item : parameters.items("PACKAGE_EXPIRY_REMINDER_DAYS")) {
      try {
        days.add(Integer.parseInt(item.strip()));
      } catch (NumberFormatException e) {
        throw new IllegalStateException("PACKAGE_EXPIRY_REMINDER_DAYS holds " + item, e);
      }
    }
    return days;
  }

  /**
   * The notice bucket of a package: the smallest threshold (notice period or reminder day) that the
   * days left have reached, so an alert is raised once per threshold.
   *
   * @param daysLeft days until the package end date
   * @param notice notice period
   * @param reminders reminder days
   * @return threshold in days
   */
  static int bucket(long daysLeft, int notice, List<Integer> reminders) {
    int bucket = notice;
    for (int r : reminders) {
      if (r < bucket && daysLeft <= r) {
        bucket = r;
      }
    }
    return bucket;
  }

  private boolean alert(Long companyId, ExpiringPackage e, int bucket) {
    ProductVersionView v = e.version();
    return alerts
        .raise(
            ALERT,
            new AlertFacts(
                companyId,
                null,
                "Product",
                v.productCode(),
                "Package "
                    + v.productCode()
                    + " version "
                    + v.versionNo()
                    + " ends on "
                    + v.dates().packageEndDate()
                    + " ("
                    + e.daysLeft()
                    + " days)"
                    + (e.renewal() == null
                        ? "; no renewal request yet"
                        : "; renewal " + e.renewal().getRequestNo()),
                null,
                ALERT + ":" + v.productCode() + ":" + v.versionNo() + ":" + bucket))
        .isPresent();
  }

  /**
   * A released package reaching its end date.
   *
   * @param version version in force
   * @param daysLeft days until the package end date
   * @param renewal open renewal request, null when none
   */
  public record ExpiringPackage(
      ProductVersionView version, long daysLeft, PackageRequest renewal) {}

  /**
   * Outcome of a renewal generation.
   *
   * @param productCode package
   * @param request the new or the existing open renewal request
   * @param created whether it was created now
   */
  public record RenewalResult(String productCode, PackageRequest request, boolean created) {}

  /**
   * Outcome of the monitor for a company.
   *
   * @param alerted alerts raised
   * @param drafted renewal requests drafted
   */
  public record MonitorRun(int alerted, int drafted) {}
}
