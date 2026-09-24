package com.iortatechnxt.brokerverse.crm;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.crm.domain.Client;
import com.iortatechnxt.brokerverse.crm.domain.KycStatus;
import com.iortatechnxt.brokerverse.crm.service.ClientOnboardingService;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.crm.service.KycDueQuery;
import com.iortatechnxt.brokerverse.crm.service.KycReviewDueJob;
import com.iortatechnxt.brokerverse.crm.service.KycReviewService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class KycReviewIT {

  @Autowired private CrmFixtures fx;
  @Autowired private ClientService clients;
  @Autowired private ClientOnboardingService onboarding;
  @Autowired private KycReviewService reviews;
  @Autowired private KycReviewDueJob job;
  @Autowired private NotificationService notifications;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  @Test
  void overdueKycExpiresIsListedAndReviewedAgain() {
    Client c = fx.confirmed(CrmFixtures.person());
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    jdbc.update(
        "update crm_client set kyc_review_due = ? where id = ?", today.minusDays(1), c.getId());

    long unreadBefore = as.run("ao2", notifications::unreadCount);
    JobOutcome outcome = job.execute(today);
    assertThat(outcome.itemsProcessed()).isPositive();
    assertThat(outcome.message()).contains("KYC expired");
    assertThat(clients.get(c.getId()).getKycStatus()).isEqualTo(KycStatus.EXPIRED);
    assertThat(as.run("ao2", notifications::unreadCount)).isGreaterThan(unreadBefore);

    KycDueQuery nonBank = new KycDueQuery(fx.company(), null, false, null, null);
    assertThat(reviews.due(nonBank, Pageable.unpaged()).getContent())
        .extracting(Client::getId)
        .contains(c.getId());
    assertThat(
            reviews.due(new KycDueQuery(fx.company(), null, true, null, null), Pageable.unpaged()))
        .extracting(Client::getId)
        .doesNotContain(c.getId());
    assertThat(reviews.countDue(new KycDueQuery(fx.company(), null, null, "STANDARD", "CBG")))
        .isPositive();

    Client reviewed = as.run("mkttl", () -> onboarding.verifyKyc(c.getId(), "periodic review"));
    assertThat(reviewed.getKycStatus()).isEqualTo(KycStatus.VERIFIED);
    assertThat(reviewed.getKycReviewDue()).isAfter(today);
    assertThat(job.name()).isEqualTo("KYC_REVIEW_DUE");
    assertThat(job.cron()).isNotBlank();
    assertThat(job.description()).isNotBlank();
  }
}
