package com.iortatechnxt.brokerverse.renewal;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.seed.RenewalSeedData;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Loads the seed profile (context and database shared with the other seed data tests) and checks
 * the Renewal seed: the booked seed invoices and the migrated policies of the go-live window are
 * renewals spread over the lists, and loading again adds nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "seed"})
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
class RenewalSeedDataIT {

  @Autowired private RenewalSeedData loader;
  @Autowired private RenewalCandidateRepository candidates;

  @Test
  void theSeedFillsTheRenewalListsOnce() throws Exception {
    List<RenewalCandidate> all = candidates.findAll();
    Set<RenewalStage> stages =
        all.stream().map(RenewalCandidate::getStage).collect(Collectors.toSet());
    assertThat(all).hasSizeGreaterThanOrEqualTo(8);
    assertThat(stages)
        .contains(
            RenewalStage.UNASSIGNED,
            RenewalStage.FOR_TL_REVIEW,
            RenewalStage.TRANSFER_PENDING,
            RenewalStage.LETTER_PENDING);
    // the insurer batch of the seed leaves one renewal with the insurer for the response screen
    assertThat(stages).contains(RenewalStage.WITH_INSURER, RenewalStage.RA_READY);
    assertThat(all).anyMatch(c -> c.getBucket() == Bucket.EXCEPTION);
    assertThat(all).anyMatch(c -> c.getFlags().isUrgent());
    long count = candidates.count();
    loader.run(null);
    assertThat(candidates.count()).isEqualTo(count);
  }
}
