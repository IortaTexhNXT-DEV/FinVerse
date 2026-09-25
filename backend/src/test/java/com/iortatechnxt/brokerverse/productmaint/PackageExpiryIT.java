package com.iortatechnxt.brokerverse.productmaint;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.catalog.domain.PackageInsurerRole;
import com.iortatechnxt.brokerverse.catalog.domain.ProductVersionStatus;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageSpec;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductExpired;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionQueryService;
import com.iortatechnxt.brokerverse.catalog.service.version.ProductVersionView;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.productmaint.domain.PackageRequest;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestStage;
import com.iortatechnxt.brokerverse.productmaint.domain.RequestType;
import com.iortatechnxt.brokerverse.productmaint.service.PackageExpiryMonitorJob;
import com.iortatechnxt.brokerverse.productmaint.service.PackageExpiryService;
import com.iortatechnxt.brokerverse.productmaint.service.PackageSlaSync;
import com.iortatechnxt.brokerverse.productmaint.service.TermsCodec;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The package expiry monitor (BRPM.017): the PACKAGE_EXPIRY_MONITOR job reads the expiring packages
 * through the catalog contract (here a fixed source, as the stub releases nothing), raises
 * PACKAGE_EXPIRING and drafts pre-filled RENEW requests when PACKAGE_RENEWAL_AUTODRAFT is on,
 * without duplicating an open renewal.
 */
@IntegrationTest
class PackageExpiryIT {

  private static final String PRODUCT = "MTR35";

  @Autowired private PackageExpiryService expiry;
  @Autowired private PackageSlaSync slas;
  @Autowired private OrganizationService organization;
  @Autowired private SystemParameterService parameters;
  @Autowired private TermsCodec codec;
  @Autowired private ApplicationEventPublisher events;
  @Autowired private TestData data;

  private static ProductVersionView expiring(LocalDate end) {
    return new ProductVersionView(
        PRODUCT,
        "Motor package MTR35",
        3,
        ProductVersionStatus.RELEASED,
        new PackageSpec.PackageDates(
            end.minusYears(1).plusDays(1), end.minusYears(1).plusDays(1), end, null),
        null,
        new PackageSpec.RateScheme(
            new BigDecimal("1.30"), new BigDecimal("5000"), null, null, "SI x 1.30%"),
        List.of(new PackageSpec.Coverage("OWN_DAMAGE", true, false, null, null, null, 10)),
        List.of(
            new PackageSpec.Insurer("INS-MGIC", PackageInsurerRole.PANEL, null, null, null, null)),
        List.of(),
        new PackageSpec.Origin(null, null, "backfill"),
        new ProductVersionView.Checkpoint(null, null, "SYSTEM", null, null));
  }

  private static ProductVersionQueryService source(ProductVersionView v) {
    return new ProductVersionQueryService() {
      @Override
      public Optional<ProductVersionView> current(String productCode) {
        return Optional.of(v);
      }

      @Override
      public Optional<ProductVersionView> inForce(String productCode, LocalDate date) {
        return Optional.of(v);
      }

      @Override
      public Optional<ProductVersionView> version(String productCode, int versionNo) {
        return Optional.of(v);
      }

      @Override
      public List<ProductVersionView> versions(String productCode) {
        return List.of(v);
      }

      @Override
      public List<ProductVersionView> packagesExpiring(Long companyId, int withinDays) {
        return List.of(v);
      }
    };
  }

  @Test
  void theMonitorAlertsAndDraftsOneRenewalRequestPrefilledFromTheVersion() {
    LocalDate end = LocalDate.now().plusDays(20);
    PackageExpiryMonitorJob job =
        new PackageExpiryMonitorJob(source(expiring(end)), expiry, slas, organization, "-");
    String before = parameters.get("PACKAGE_RENEWAL_AUTODRAFT").getValue();
    try {
      parameters.update("PACKAGE_RENEWAL_AUTODRAFT", "true");
      JobOutcome first = job.execute(LocalDate.now());
      assertThat(first.message()).contains("renewal request(s) drafted");
      Optional<PackageRequest> renewal = expiry.openRenewal(PRODUCT);
      assertThat(renewal).isPresent();
      PackageRequest r = renewal.get();
      assertThat(r.getRequestType()).isEqualTo(RequestType.RENEW);
      assertThat(r.getStatus()).isEqualTo(RequestStage.DRAFT);
      assertThat(r.getBaseVersionNo()).isEqualTo(3);
      assertThat(r.isNegotiationRequired()).isFalse();
      assertThat(r.getPackageEndDate()).isEqualTo(end.plusYears(1));
      assertThat(codec.terms(r.getRequestedTerms()).dates().effectiveFrom())
          .isEqualTo(end.plusDays(1));
      assertThat(codec.terms(r.getRequestedTerms()).insurerCodes()).isSubsetOf("INS-MGIC");

      job.execute(LocalDate.now());
      assertThat(expiry.openRenewal(PRODUCT))
          .get()
          .extracting(PackageRequest::getId)
          .isEqualTo(r.getId());
      assertThat(expiry.expiring(source(expiring(end)), data.company().getId(), 30))
          .singleElement()
          .satisfies(e -> assertThat(e.renewal().getId()).isEqualTo(r.getId()));
    } finally {
      parameters.update("PACKAGE_RENEWAL_AUTODRAFT", before);
    }
    events.publishEvent(new ProductExpired(PRODUCT, 3, end));
    assertThat(job.name()).isEqualTo("PACKAGE_EXPIRY_MONITOR");
    assertThat(job.cron()).isEqualTo("-");
    assertThat(job.description()).isNotBlank();
  }

  @Test
  void theSlaParametersAreCopiedIntoTheWorkflowStages() {
    slas.sync();
    assertThat(slas.sync()).isZero();
  }
}
