package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSearchService.ReceiptCriteria;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedInquiry;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedInquiry.Filter;
import com.iortatechnxt.brokerverse.cashiering.service.UnappliedInquiry.Row;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The list of the unapplied payments (Operations Cashiering FRS v3.2: FRS.CSH.06.01.03 to
 * 06.01.05): the type and text filters, the columns, fully applied payments out of the list, and a
 * Marketing user limited to the records of his or her marketing unit.
 */
@IntegrationTest
class UnappliedInquiryIT {

  @Autowired private CashFixtures fx;
  @Autowired private UnappliedInquiry inquiry;
  @Autowired private ReceiptSearchService search;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private Unapplied unmatched(String reference, String unit) {
    Unapplied u = fx.pay(reference, new BigDecimal("75.00")).unapplied();
    jdbc.update("update csh_unapplied set sales_unit = ? where id = ?", unit, u.getId());
    return u;
  }

  private List<Long> ids(String user, Filter f) {
    return as.run(user, () -> inquiry.search(fx.company(), f, Pageable.ofSize(200)))
        .map(Row::id)
        .getContent();
  }

  private static Filter text(String part, String type) {
    return new Filter(type, null, null, null, null, null, null, part, true);
  }

  @Test
  void unmatchedPaymentsAreFoundByTypeAndPartOfTheirReferenceInAnyCase() {
    String tag = "Unk-" + System.nanoTime();
    Unapplied u = unmatched(tag, "T-CBG1");

    assertThat(ids("cashier", text(tag.toLowerCase(java.util.Locale.ROOT), "UNBOOKED")))
        .contains(u.getId());
    assertThat(ids("cashier", text(tag, "EXCESS"))).doesNotContain(u.getId());
    Row row =
        as.run("cashier", () -> inquiry.search(fx.company(), text(tag, null), Pageable.ofSize(5)))
            .getContent()
            .get(0);
    assertThat(row.type()).isEqualTo("UNBOOKED");
    assertThat(row.outstanding()).isEqualByComparingTo("75.00");
    assertThat(row.paidOn()).isNotNull();

    jdbc.update("update csh_unapplied set balance = 0 where id = ?", u.getId());
    assertThat(ids("cashier", text(tag, null))).doesNotContain(u.getId());
  }

  @Test
  void theSearchLogListsTheSearchesOfAUserWithTheCriteriaInWords() {
    String payor = "logged" + System.nanoTime();
    as.run(
        "cashier",
        () ->
            search.search(
                new ReceiptCriteria(
                    fx.company(),
                    null,
                    null,
                    null,
                    payor,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null),
                Pageable.ofSize(5)));
    var log =
        as.run(
            "cashtl", () -> search.log(fx.company(), "cashier", null, null, Pageable.ofSize(50)));
    assertThat(log.getContent())
        .anySatisfy(r -> assertThat(r.criteria()).isEqualTo("Payor: " + payor));
  }

  @Test
  void aMarketingUserSeesTheRecordsOfHisOrHerMarketingUnitOnly() {
    String tag = "Mkt-" + System.nanoTime();
    Unapplied own = unmatched(tag + "-A", "T-OWN" + tag.hashCode() % 1000);
    Unapplied other = unmatched(tag + "-B", "T-OTHER");
    jdbc.update(
        "update sec_user set business_unit_code = ? where username = 'ao'",
        "T-OWN" + tag.hashCode() % 1000);

    assertThat(ids("ao", text(tag, null))).contains(own.getId()).doesNotContain(other.getId());
    assertThat(ids("cashier", text(tag, null))).contains(own.getId(), other.getId());
  }
}
