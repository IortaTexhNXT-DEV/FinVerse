package com.iortatechnxt.brokerverse.brokerclaims;

import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.LEAD;
import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.OFFICER;
import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.PremiumCheckService;
import com.iortatechnxt.brokerverse.brokerclaims.demo.BrokerClaimsDemoData;
import com.iortatechnxt.brokerverse.brokerclaims.domain.BrokerClaimRepository;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimNoBulkHandler;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerUpdateBulkHandler;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerUpdateService;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService.LocationPick;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.LocationRefBulkHandler;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.LocationRefService;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.opsledger.demo.DemoUsers;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * The Claims bulk uploads (design 9.5; BRCLM.041/042/043): insurer updates found by claim number or
 * by insurer claim number, insurer claim numbers and insurer location references, each row
 * validated before it is applied; and the demo storyline of wave CL1-A run through the services.
 */
@IntegrationTest
class ClaimsBulkIT {

  @Autowired private ClaimsFixtures fx;
  @Autowired private ClaimRecordingService recording;
  @Autowired private InsurerClaimService insurers;
  @Autowired private InsurerUpdateService updates;
  @Autowired private InsurerUpdateBulkHandler updateHandler;
  @Autowired private InsurerClaimNoBulkHandler numberHandler;
  @Autowired private LocationRefBulkHandler refHandler;
  @Autowired private LocationRefService refs;
  @Autowired private PremiumCheckService premiums;
  @Autowired private BrokerClaimRepository claims;
  @Autowired private CompanyRepository companies;
  @Autowired private UserDetailsService users;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;

  private BulkContext context() {
    return new BulkContext(
        fx.company(), "BLK-TEST-" + System.nanoTime(), LocalDate.now(), Map.of());
  }

  private static BulkRow row(int no, String... pairs) {
    Map<String, String> values = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) {
      if (pairs[i + 1] != null) {
        values.put(pairs[i], pairs[i + 1]);
      }
    }
    return new BulkRow(no, values);
  }

  private Claim property(OpsInvoice invoice) {
    return as.run(
        OFFICER,
        () ->
            recording.record(
                fx.company(),
                ClaimsFixtures.request(
                    invoice.getArn(),
                    ClaimsFixtures.loss("FIRE"),
                    List.of(new LocationPick(1, null)),
                    List.of())));
  }

  @Test
  void insurerClaimNumbersAndUpdatesLoadByUpload() {
    Claim claim = property(fx.propertyInvoice(1));
    String number = "UPL-" + System.nanoTime();
    BulkContext ctx = context();
    String today = LocalDate.now().toString();

    BulkRow numberRow =
        row(
            1,
            "Claim No.",
            claim.getClaimNo(),
            "Insurer",
            LEAD,
            "Insurer Claim No.",
            number,
            "Reported To Insurer On",
            today);
    assertThat(numberHandler.validate(numberRow, ctx)).isEmpty();
    assertThat(numberHandler.duplicateKey(numberRow)).contains(number);
    assertThat(
            numberHandler.validate(
                row(
                    2,
                    "Claim No.",
                    "BCL-0000-000000",
                    "Insurer",
                    LEAD,
                    "Insurer Claim No.",
                    "X",
                    "Reported To Insurer On",
                    "2999-01-01"),
                ctx))
        .hasSize(2);
    as.run(OFFICER, () -> numberHandler.commit(numberRow, ctx));
    assertThat(insurers.ofClaim(claim.getId())).anyMatch(l -> number.equals(l.getInsurerClaimNo()));

    BulkRow byNumber =
        row(
            1,
            "Insurer",
            LEAD,
            "Insurer Claim No.",
            number,
            "Update Date",
            today,
            "Source",
            "EMAIL",
            "Remarks",
            "Bordereau line");
    assertThat(updateHandler.validate(byNumber, ctx)).isEmpty();
    BulkRow unmatched =
        row(
            37,
            "Insurer",
            LEAD,
            "Insurer Claim No.",
            "C-INSB-0000",
            "Update Date",
            today,
            "Source",
            "EMAIL",
            "Remarks",
            "x");
    assertThat(updateHandler.validate(unmatched, ctx))
        .containsExactly("No claim found for " + LEAD + " C-INSB-0000");
    assertThat(
            updateHandler.validate(
                row(3, "Update Date", today, "Source", "NOPE", "Remarks", "x"), ctx))
        .hasSize(2);
    as.run(OFFICER, () -> updateHandler.commit(byNumber, ctx));
    as.run(
        OFFICER,
        () ->
            updateHandler.commit(
                row(
                    4,
                    "Claim No.",
                    claim.getClaimNo(),
                    "Update Date",
                    today,
                    "Source",
                    "LETTER",
                    "Remarks",
                    "Letter"),
                ctx));
    assertThat(updates.ofClaim(claim.getId()))
        .hasSize(2)
        .allSatisfy(u -> assertThat(u.getUploadRef()).isEqualTo(ctx.jobNo()));
    assertThat(updateHandler.columns()).hasSize(7);
    assertThat(numberHandler.columns()).hasSize(4);
  }

  @Test
  void locationReferencesLoadByUpload() {
    OpsInvoice invoice = fx.propertyInvoice(2);
    BulkContext ctx = context();
    BulkRow good =
        row(
            1,
            "ARN",
            invoice.getArn(),
            "Item No.",
            "2",
            "Insurer",
            LEAD,
            "Insurer Location Ref",
            "UPL-LOC-2",
            "Effective From",
            "2026-09-01");
    assertThat(refHandler.validate(good, ctx)).isEmpty();
    assertThat(refHandler.validate(row(2, "ARN", "ARN-NONE", "Item No.", "1"), ctx)).hasSize(1);
    assertThat(refHandler.validate(row(3, "ARN", invoice.getArn(), "Item No.", "9"), ctx))
        .hasSize(1);
    assertThat(refHandler.duplicateKey(good)).contains(invoice.getArn());
    as.run(OFFICER, () -> refHandler.commit(good, ctx));
    assertThat(refs.ofCover(fx.company(), invoice.getArn()))
        .singleElement()
        .satisfies(r -> assertThat(r.getInsurerLocationRef()).isEqualTo("UPL-LOC-2"));
    assertThat(refHandler.permission()).isEqualTo("BCL_LOCATION_REF_MAINTAIN");
  }

  @Test
  void theDemoStorylineRecordsClaimsThroughTheServices() {
    long before = claims.count();
    var demo =
        new BrokerClaimsDemoData(
            companies, claims, recording, premiums, insurers, updates, new DemoUsers(users), clock);
    demo.load();
    assertThat(claims.count()).isGreaterThan(before);
    assertThat(
            jdbc.queryForList(
                "select distinct created_by from bcl_claim where arn like 'ARN-2026-9400%'",
                String.class))
        .isNotEmpty()
        .allMatch(u -> u.startsWith("clmofficer"));
    demo.run(new DefaultApplicationArguments());
  }
}
