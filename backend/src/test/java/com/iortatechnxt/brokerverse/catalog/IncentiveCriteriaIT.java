package com.iortatechnxt.brokerverse.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.alert.domain.AlertRepository;
import com.iortatechnxt.brokerverse.approval.service.ApprovalViewer;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingRuleService;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.booking.service.InvoiceBooked;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveCriteria.Details;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveScope;
import com.iortatechnxt.brokerverse.catalog.domain.IncentiveValueBasis;
import com.iortatechnxt.brokerverse.catalog.service.CatalogApprovalSource;
import com.iortatechnxt.brokerverse.catalog.service.CatalogKind;
import com.iortatechnxt.brokerverse.catalog.service.CatalogRecords;
import com.iortatechnxt.brokerverse.catalog.service.IncentiveCriteriaService;
import com.iortatechnxt.brokerverse.catalog.service.IncentiveCriteriaService.IncentiveFacts;
import com.iortatechnxt.brokerverse.catalog.service.version.IncentiveCriteriaChanged;
import com.iortatechnxt.brokerverse.catalog.service.version.PackageIncentiveReview;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Incentive criteria on the products matrix (PMADD07/08): validated, maker-checker, amended by a
 * successor, deactivated, matched at booking and flagged when their package is no longer sold.
 */
@com.iortatechnxt.brokerverse.support.IntegrationTest
@RecordApplicationEvents
class IncentiveCriteriaIT {

  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);

  @Autowired private PackageFixtures fx;
  @Autowired private IncentiveCriteriaService criteria;
  @Autowired private CatalogRecords records;
  @Autowired private CatalogApprovalSource approvals;
  @Autowired private PackageIncentiveReview review;
  @Autowired private AlertRepository alerts;
  @Autowired private BookingFixtures booking;
  @Autowired private BookingService bookings;
  @Autowired private BookingRuleService bookingRules;
  @Autowired private ApplicationEvents events;
  @Autowired private AsUser as;

  private static Details details(String product, LocalDate from, LocalDate to) {
    return new Details(
        "Test incentive",
        "CAMPAIGN",
        IncentiveValueBasis.RATE,
        new BigDecimal("1.5"),
        "{\"minimumPremium\": 1000}",
        "Test",
        List.of(new IncentiveScope(product, null, "CBG", null, null)),
        from,
        to);
  }

  private static IncentiveFacts facts(String product, LocalDate date) {
    return new IncentiveFacts(product, "COMPREHENSIVE", "CBG", "EMAIL", "INS-MGIC", date);
  }

  @Test
  void criteriaAreValidatedAuthorisedAmendedAndDeactivated() {
    String product = fx.released();
    Long company = fx.company();
    String code = "T" + PackageFixtures.code();

    assertThatThrownBy(
            () ->
                as.run(
                    "mbs",
                    () ->
                        criteria.create(
                            company,
                            code,
                            new Details(
                                "x",
                                "CAMPAIGN",
                                IncentiveValueBasis.RULE,
                                null,
                                null,
                                null,
                                List.of(),
                                FROM,
                                null))))
        .extracting("code")
        .isEqualTo("INCENTIVE_PRODUCTS_REQUIRED");
    assertThatThrownBy(
            () ->
                as.run("mbs", () -> criteria.create(company, code, details("MOP07X", FROM, null))))
        .hasMessageContaining("MOP07X");
    assertThatThrownBy(
            () -> as.run("ao", () -> criteria.create(company, code, details(product, FROM, null))))
        .isInstanceOf(AccessDeniedException.class);

    IncentiveCriteria created =
        as.run("mbs", () -> criteria.create(company, code, details(product, FROM, null)));
    assertThat(created.getRecordStatus()).isEqualTo(RecordStatus.PENDING_AUTHORIZATION);
    assertThat(criteria.matching(company, facts(product, fx.today()))).doesNotContain(code);
    assertThatThrownBy(
            () ->
                as.run(
                    "mbs",
                    () -> criteria.create(company, code, details(product, FROM.plusDays(5), null))))
        .extracting("code")
        .isEqualTo("INCENTIVE_PERIOD_OVERLAP");
    assertThat(approvals.pendingFor(ApprovalViewer.user("approver", Set.of("PRODUCT_AUTHORIZE"))))
        .anyMatch(a -> a.reference().startsWith(code));
    assertThatThrownBy(
            () ->
                as.run(
                    "mbs",
                    () -> records.authorize(CatalogKind.INCENTIVE_CRITERIA, created.getId())))
        .isInstanceOf(AccessDeniedException.class);
    as.run("approver", () -> records.authorize(CatalogKind.INCENTIVE_CRITERIA, created.getId()));
    assertThat(criteria.matching(company, facts(product, fx.today()))).contains(code);
    assertThat(criteria.matching(company, facts("MOP07", fx.today()))).doesNotContain(code);
    assertThat(events.stream(IncentiveCriteriaChanged.class))
        .anyMatch(
            e -> e.code().equals(code) && e.change() == IncentiveCriteriaChanged.Change.ACTIVATED);

    LocalDate change = fx.today().plusDays(10);
    IncentiveCriteria successor =
        as.run("mbs", () -> criteria.amend(created.getId(), details(product, change, null)));
    assertThat(successor.getSuccessorOf()).isEqualTo(created.getId());
    as.run("approver", () -> records.authorize(CatalogKind.INCENTIVE_CRITERIA, successor.getId()));
    IncentiveCriteria ended = criteria.get(created.getId());
    assertThat(ended.getEffectiveTo()).isEqualTo(change.minusDays(1));
    assertThat(criteria.history(company, code)).hasSize(2);
    assertThat(criteria.matching(company, facts(product, change.plusDays(1)))).contains(code);
    assertThat(events.stream(IncentiveCriteriaChanged.class))
        .anyMatch(
            e -> e.code().equals(code) && e.change() == IncentiveCriteriaChanged.Change.AMENDED);

    as.run("mbs", () -> criteria.deactivate(successor.getId(), change.plusDays(30)));
    assertThat(criteria.get(successor.getId()).getRecordStatus()).isEqualTo(RecordStatus.INACTIVE);
    assertThat(criteria.matching(company, facts(product, change.plusDays(1)))).doesNotContain(code);
  }

  @Test
  void bookingStampsTheMatchedCriteriaCodes() {
    var account = booking.motor();
    assertThat(account.getProductVersionNo()).isEqualTo(1);
    BookedInvoice invoice =
        as.run(
            "proc",
            () ->
                bookings.book(
                    account.getArn(),
                    BookingOptions.of(BookingFixtures.BOOKED_ON, null),
                    BookingSource.INDIVIDUAL));
    assertThat(invoice.getFlags().incentiveEligible()).isTrue();
    assertThat(invoice.getFlags().incentiveCriteriaCodes()).contains("CPC2");
    assertThat(invoice.getFacts().productVersionNo()).isEqualTo(1);
    InvoiceBooked event = InvoiceBooked.of(invoice);
    assertThat(event.incentiveCriteria()).contains("CPC2");
    assertThat(event.productVersionNo()).isEqualTo(1);
    assertThatThrownBy(() -> bookingRules.createIncentiveRule(fx.company(), null))
        .extracting("code")
        .isEqualTo("INCENTIVE_RULES_FROZEN");
  }

  @Test
  void criteriaOfAPackageThatIsNoLongerSoldAreFlagged() {
    String product = fx.released();
    String code = "T" + PackageFixtures.code();
    IncentiveCriteria created =
        as.run("mbs", () -> criteria.create(fx.company(), code, details(product, FROM, null)));
    as.run("approver", () -> records.authorize(CatalogKind.INCENTIVE_CRITERIA, created.getId()));
    List<IncentiveCriteria> flagged = review.productInactive(product, "retired");
    assertThat(flagged).extracting(IncentiveCriteria::getCode).containsExactly(code);
    assertThat(alerts.findAll())
        .anyMatch(a -> String.valueOf(created.getId()).equals(a.getEntityId()));
  }
}
