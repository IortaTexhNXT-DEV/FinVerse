package com.iortatechnxt.brokerverse.cashiering.seed;

import com.iortatechnxt.brokerverse.cashiering.domain.CollectorRequest;
import com.iortatechnxt.brokerverse.cashiering.service.CollectorRequestService;
import com.iortatechnxt.brokerverse.cashiering.service.CollectorRequestService.Acceptance;
import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Cashiering's side of the unapplied-payments seed storyline (wave C1-C, seed profile only,
 * idempotent): after the collectors disposed of the seed payments ({@code
 * collections.seed.UnappliedSeedData}), the cashier accepts the queued application requests and
 * processes them at once, so the payment is applied to the invoice and Collections sees the request
 * applied. The refund request stays queued for the cashier to act on.
 */
@Component
@Profile("seed")
@Order(127)
public class CollectorRequestSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(CollectorRequestSeedData.class);
  private static final int PAGE = 50;

  private final CollectorRequestService requests;
  private final CompanyRepository companies;
  private final SeedUsers users;

  /**
   * Creates the loader.
   *
   * @param requests collector requests
   * @param companies companies
   * @param users seed sign-in
   */
  public CollectorRequestSeedData(
      CollectorRequestService requests, CompanyRepository companies, SeedUsers users) {
    this.requests = requests;
    this.companies = companies;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    Optional<Long> company = companies.findByCode("FVI").map(c -> c.getId());
    if (company.isEmpty()) {
      return;
    }
    List<CollectorRequest> queued =
        requests
            .list(
                company.get(),
                List.of(CollectorRequest.Status.QUEUED),
                null,
                PageRequest.of(0, PAGE))
            .getContent();
    for (CollectorRequest r : queued) {
      if ("APPLY_TO_INVOICE".equals(r.getAction())) {
        try {
          users.run(
              "cashier",
              () ->
                  requests.accept(
                      r.getId(),
                      new Acceptance(null, null, null, null, null, "Applied as requested", true)));
          LOG.info("Collector request {} applied", r.getRequestNo());
        } catch (RuntimeException ex) {
          LOG.warn("Collector request {} skipped: {}", r.getRequestNo(), ex.getMessage());
        }
      }
    }
  }
}
