package com.iortatechnxt.brokerverse.brokerclaims;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.BrokerClaimClientRecords;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.BrokerClaimRetentionProvider;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimOperationsSync;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimViewService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.feed.service.InAppClaimsFeed;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.nbadmin.service.RetentionCriteria;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.port.ClaimsFeed;
import com.iortatechnxt.brokerverse.opsledger.service.port.FeedItem;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RequestSource;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittance;
import com.iortatechnxt.brokerverse.remittance.service.SpecialRemittanceService;
import com.iortatechnxt.brokerverse.remittance.service.SpecialRemittanceService.NewRequest;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The claims special remittance confirmed in-app (OQ46, OQ25, MKTID.009; wave CL1-A exit criterion
 * 3): an invoice of a claim in status "With BDOI - For Premium Remittance" is in the {@code
 * CLAIMS_SPECIAL_REMIT} feed and its special remittance request is confirmed; an invoice without
 * such a claim is refused. Also the client 360 records, the retention candidates and the
 * notifications of the Operations events.
 */
@IntegrationTest
class ClaimsFeedIT {

  private static final String REMITTANCE_STATUS = "BDOI_PREMIUM_REMITTANCE";

  @Autowired private ClaimsFixtures fx;
  @Autowired private ClaimsFeed feed;
  @Autowired private SpecialRemittanceService specials;
  @Autowired private ClaimViewService views;
  @Autowired private ClaimOperationsSync sync;
  @Autowired private BrokerClaimClientRecords clientRecords;
  @Autowired private BrokerClaimRetentionProvider retention;
  @Autowired private ClientService clients;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private void awaitRemittance(Claim claim) {
    // The status engine is wave CL1-B's: the test sets the status the way it will.
    jdbc.update(
        "update bcl_claim set status_code = ?, status_since = now(), phase = 'IN_PROGRESS' where id = ?",
        REMITTANCE_STATUS,
        claim.getId());
  }

  @Test
  void aClaimsSpecialRemittanceIsConfirmedThroughTheFeed() {
    assertThat(feed).isInstanceOf(InAppClaimsFeed.class);
    OpsInvoice invoice = fx.motorInvoice();
    fx.pay(invoice);
    Claim claim = fx.motorClaim(invoice.getArn());
    LocalDate since = invoice.getBookingDate();
    assertThat(feed.fetch(fx.company(), InAppClaimsFeed.SPECIAL_REMIT, since))
        .noneMatch(i -> i.key().equals(invoice.getInvoiceNo()));

    awaitRemittance(claim);
    List<FeedItem> items = feed.fetch(fx.company(), InAppClaimsFeed.SPECIAL_REMIT, since);
    assertThat(items)
        .filteredOn(i -> i.key().equals(invoice.getInvoiceNo()))
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.fields()).containsEntry("claimNo", claim.getClaimNo());
              assertThat(i.fields()).containsEntry("status", REMITTANCE_STATUS);
              assertThat(i.fields()).containsEntry("handler", ClaimsFixtures.OFFICER);
            });
    assertThat(feed.fetch(fx.company(), "OTHER_FEED", since)).isEmpty();
    var view = views.view(fx.company(), claim.getId());
    assertThat(view.awaitingRemittance()).isTrue();
    assertThat(view.unremittedInvoices()).contains(invoice.getInvoiceNo());

    SpecialRemittance request =
        as.run(
            "mktcoll",
            () ->
                specials.request(
                    fx.company(),
                    new NewRequest(invoice.getInvoiceNo(), "CLAIMS", "Claim " + claim.getClaimNo()),
                    RequestSource.SCREEN));
    assertThat(request.getValidationNote()).contains("claim confirmed by the Claims system");

    OpsInvoice other = fx.motorInvoice();
    fx.pay(other);
    assertThatThrownBy(
            () ->
                as.run(
                    "mktcoll",
                    () ->
                        specials.request(
                            fx.company(),
                            new NewRequest(other.getInvoiceNo(), "CLAIMS", null),
                            RequestSource.SCREEN)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("the Claims system has no claim for it");

    assertThat(sync.invoiceRemitted(invoice.getInvoiceNo())).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = ? and entity_id = ?",
                Long.class,
                ClaimsFixtures.OFFICER,
                String.valueOf(claim.getId())))
        .isPositive();
    assertThat(sync.endorsementBooked(invoice.getInvoiceNo())).isZero();
    assertThat(sync.invoiceMoved("NO-SUCH-INVOICE")).isZero();
  }

  @Test
  void claimsAppearInTheClientViewAndTheRetentionReview() {
    OpsInvoice invoice = fx.motorInvoice();
    Claim claim = fx.motorClaim(invoice.getArn());
    Long clientId = clients.requireByCode(fx.company(), claim.getCover().getClientCode()).getId();
    assertThat(clientRecords.recordsOf(clientId))
        .anySatisfy(
            r -> {
              assertThat(r.reference()).isEqualTo(claim.getClaimNo());
              assertThat(r.link()).isEqualTo("/claims-handling/" + claim.getId());
            });

    assertThat(retention.recordType()).isEqualTo(ClaimCodes.RETENTION_RECORD_TYPE);
    jdbc.update(
        "update bcl_claim set phase = 'CLOSED', updated_at = now() - interval '12 years' where id = ?",
        claim.getId());
    RetentionCriteria criteria = new RetentionCriteria(Set.of("CLOSED"), LocalDate.now().minusYears(10));
    assertThat(retention.countEligible(criteria)).isPositive();
    assertThat(retention.eligible(criteria, 500))
        .anySatisfy(c -> assertThat(c.reference()).isEqualTo(claim.getClaimNo()));
    RetentionCriteria none = new RetentionCriteria(Set.of("ACTIVE"), LocalDate.now());
    assertThat(retention.countEligible(none)).isZero();
    assertThat(retention.eligible(none, 10)).isEmpty();
  }
}
