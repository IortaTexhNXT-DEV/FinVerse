package com.iortatechnxt.brokerverse.submitted;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.submitted.domain.SbmDocStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFee;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFeeRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaaf;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewalRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRunRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTorRepository;
import com.iortatechnxt.brokerverse.submitted.seed.SubmittedSeedData;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Loads the seed profile (context and database shared with the other seed data tests) and checks
 * the Submitted Policies seed: the masterlist in its statuses after the seed run, the reviews with
 * an IAAF approved, a TOR, a renewal handed over, the handling fees, and loading again adds
 * nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "seed"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class SubmittedSeedDataIT {

  @Autowired private SubmittedSeedData loader;
  @Autowired private SbmPolicyRepository policies;
  @Autowired private SbmRunRepository runs;
  @Autowired private SbmIaafRepository iaafs;
  @Autowired private SbmTorRepository tors;
  @Autowired private SbmRenewalRepository renewals;
  @Autowired private SbmHandlingFeeRepository fees;

  @Test
  void theSeedFillsTheMasterlistOnce() throws Exception {
    List<SbmPolicy> all = policies.findAll();
    Set<SbmPolicyStatus> statuses =
        all.stream().map(SbmPolicy::getStatus).collect(Collectors.toSet());
    assertThat(all).hasSizeGreaterThanOrEqualTo(20);
    assertThat(statuses)
        .contains(
            SbmPolicyStatus.FOR_RENEWAL,
            SbmPolicyStatus.EXCLUDED,
            SbmPolicyStatus.RENEWAL_IN_PROGRESS);
    assertThat(all).anyMatch(SbmPolicy::isFallout);
    assertThat(runs.count()).isPositive();
    assertThat(iaafs.findAll())
        .extracting(SbmIaaf::getStatus)
        .contains(SbmDocStatus.APPROVED, SbmDocStatus.FOR_APPROVAL);
    assertThat(tors.count()).isPositive();
    assertThat(renewals.count()).isPositive();
    assertThat(fees.findAll())
        .extracting(SbmHandlingFee::getStatus)
        .contains(SbmHandlingFee.BILLED);

    long before = policies.count();
    loader.run(new DefaultApplicationArguments());
    assertThat(policies.count()).isEqualTo(before);
  }
}
