package com.iortatechnxt.brokerverse.cashiering;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecord;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender;
import com.iortatechnxt.brokerverse.cashiering.service.OtherIncomeItems;
import com.iortatechnxt.brokerverse.cashiering.service.OtherIncomeItems.Item;
import com.iortatechnxt.brokerverse.cashiering.service.RecordValidation.Draft;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The OR of an incentive payment settles a Valid for Collection SOA of the insurer, with its
 * outstanding amount (Operations Cashiering FRS v3.2: FRS.CSH.02.02.08).
 */
@IntegrationTest
class OtherIncomeItemsIT {

  @Autowired private CashFixtures fx;
  @Autowired private RecordFixtures records;
  @Autowired private OtherIncomeItems items;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private String postedIncentiveRun(String insurer, BigDecimal incentive) {
    String runNo = "IR-T" + System.nanoTime() % 1_000_000_000L;
    Long scheme =
        jdbc
            .queryForList(
                "select id from cmr_incentive_scheme where company_id = ? order by id",
                Long.class,
                fx.company())
            .stream()
            .findFirst()
            .orElseGet(
                () ->
                    jdbc.queryForObject(
                        "insert into cmr_incentive_scheme (company_id, code, name, scheme_type,"
                            + " calculation, period_type, beneficiary, active, created_at,"
                            + " created_by) values (?, 'T-SCHEME', 'Test scheme', 'OTHER',"
                            + " 'FIXED_PER_POLICY', 'MONTHLY', 'BDOI', true, now(), 'test')"
                            + " returning id",
                        Long.class,
                        fx.company()));
    Long run =
        jdbc.queryForObject(
            "insert into cmr_incentive_run (company_id, run_no, scheme_id, period_from, period_to,"
                + " status, created_at, created_by) values (?, ?, ?, date '2026-09-01',"
                + " date '2026-09-30', 'POSTED', now(), 'test') returning id",
            Long.class,
            fx.company(),
            runNo,
            scheme);
    jdbc.update(
        "insert into cmr_incentive_run_line (run_id, invoice_no, insurer_code, basic_premium,"
            + " gross_premium, excluded, incentive) values (?, 'BI-T-1', ?, 10000, 11200, false, ?)",
        run,
        insurer,
        incentive);
    return runNo;
  }

  @Test
  void anIncentiveOrListsTheSoasOfTheInsurerWithTheirOutstandingAmounts() {
    String insurer = "INS-T" + System.nanoTime() % 100000;
    String runNo = postedIncentiveRun(insurer, new BigDecimal("1500.00"));

    List<Item> open = as.run("cashier", () -> items.open(fx.company(), "INCENTIVE", insurer));
    assertThat(open)
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.reference()).isEqualTo(runNo);
              assertThat(i.outstanding()).isEqualByComparingTo("1500.00");
            });

    Draft or =
        records.or(
            "INCENTIVE", insurer, new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO);
    RecordTender t = or.tender();
    Draft settling =
        new Draft(
            or.kind(),
            or.receiptType(),
            or.branchId(),
            or.party(),
            new RecordTender(
                t.tenderType(),
                t.currency(),
                t.bankAccount(),
                t.amount(),
                t.vat(),
                t.wtax(),
                t.certificateRef(),
                t.check(),
                t.receiptDate(),
                t.remarks(),
                runNo),
            or.accounts());
    ReceiptRecord submitted = records.submitted(settling);
    records.post(submitted.getId());

    assertThat(as.run("cashier", () -> items.open(fx.company(), "INCENTIVE", insurer)))
        .singleElement()
        .satisfies(i -> assertThat(i.outstanding()).isEqualByComparingTo("500.00"));
    assertThat(as.run("cashier", () -> items.open(fx.company(), "OTHERS", insurer))).isEmpty();
  }
}
