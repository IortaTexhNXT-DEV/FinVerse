package com.iortatechnxt.brokerverse.tax;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.payables.service.SeedActor;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.tax.domain.Certificate2307;
import com.iortatechnxt.brokerverse.tax.domain.ReturnStatus;
import com.iortatechnxt.brokerverse.tax.domain.TaxReturn;
import com.iortatechnxt.brokerverse.tax.seed.TaxSeedData;
import com.iortatechnxt.brokerverse.tax.seed.TaxSeedMasters;
import com.iortatechnxt.brokerverse.tax.service.Certificate2307Service;
import com.iortatechnxt.brokerverse.tax.service.IcMappingService;
import com.iortatechnxt.brokerverse.tax.service.PartyTaxProfileService;
import com.iortatechnxt.brokerverse.tax.service.TaxCalendarService;
import com.iortatechnxt.brokerverse.tax.service.TaxCodeService;
import com.iortatechnxt.brokerverse.tax.service.TaxReturnService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Runs the seed-profile tax loader against the test database twice (idempotency): masters, Q1-Q2
 * returns filed and paid with remittances, Q3 drafts and the 2307 batches of Q1 and Q2.
 */
@IntegrationTest
class TaxSeedDataIT {

  @Autowired private OrganizationService organization;
  @Autowired private TaxSeedMasters masters;
  @Autowired private TaxCalendarService calendar;
  @Autowired private TaxReturnService returns;
  @Autowired private Certificate2307Service certificates;
  @Autowired private TaxCodeService codes;
  @Autowired private PartyTaxProfileService profiles;
  @Autowired private IcMappingService mappings;
  @Autowired private SeedActor actor;
  @Autowired private TaxFixtures fixtures;

  @Test
  void loadsIdempotentTaxSeedData() {
    fixtures.masters();
    fixtures.firePolicy(LocalDate.of(2026, 2, 16), "40000");
    fixtures.supplierInvoice(LocalDate.of(2026, 5, 11), "8000");
    TaxSeedData loader =
        new TaxSeedData(organization, masters, calendar, returns, certificates, actor);
    loader.run(null);
    loader.run(null);

    Long company = fixtures.companyId();
    assertThat(codes.list(company)).hasSizeGreaterThanOrEqualTo(20);
    assertThat(profiles.list(company)).extracting("partyCode").contains("S-0002", "B-0001");
    assertThat(mappings.list(company)).hasSizeGreaterThanOrEqualTo(30);

    List<TaxReturn> all = returns.list(company, 2026, null, null);
    assertThat(all)
        .filteredOn(r -> r.getPeriodEnd().isBefore(LocalDate.of(2026, 7, 1)))
        .isNotEmpty()
        .allMatch(r -> r.getStatus() == ReturnStatus.PAID);
    assertThat(returns.list(company, 2026, "2550Q", null))
        .extracting(TaxReturn::getStatus)
        .containsExactly(ReturnStatus.PAID, ReturnStatus.PAID, ReturnStatus.DRAFT);
    assertThat(returns.list(company, 2026, "0619-E", ReturnStatus.PAID)).hasSize(4);
    TaxReturn q1Vat = returns.list(company, 2026, "2550Q", null).get(0);
    assertThat(returns.remittance(q1Vat.getId())).isPresent();

    List<Certificate2307> issued = certificates.register(company, 2026);
    assertThat(issued).extracting(c -> c.getPeriodStart().getMonthValue()).contains(1, 4);
    assertThat(issued).extracting(Certificate2307::getPartyCode).contains("B-0001", "S-0002");
    assertThat(certificates.batchPdf(issued.get(0).getBatch().getId())).isNotEmpty();
  }
}
