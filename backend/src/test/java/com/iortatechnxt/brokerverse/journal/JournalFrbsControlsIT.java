package com.iortatechnxt.brokerverse.journal;

import static com.iortatechnxt.brokerverse.support.TestCompanies.line;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.BulkApprovalService;
import com.iortatechnxt.brokerverse.approval.service.BulkApprovalService.Item;
import com.iortatechnxt.brokerverse.approval.service.BulkApprovalService.Outcome;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalSearchCriteria;
import com.iortatechnxt.brokerverse.journal.domain.JournalStatus;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.JournalAuthorizationService;
import com.iortatechnxt.brokerverse.journal.service.JournalAutoReversalService;
import com.iortatechnxt.brokerverse.journal.service.JournalBulkApprovals;
import com.iortatechnxt.brokerverse.journal.service.JournalEntryService;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestCompanies;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * FRBS journal controls (BRD-5 FRBS 2.5.x, 2.8.x; ACSL 2.9.1): automatic reversal date, assignment,
 * bulk posting, negative balance control, short-code entry and correction links.
 */
@IntegrationTest
class JournalFrbsControlsIT {

  private static final String CASH = "1111";
  private static final String RENT = "5603";

  @Autowired private JournalEntryService entries;
  @Autowired private JournalAuthorizationService authorization;
  @Autowired private JournalAutoReversalService reversals;
  @Autowired private JournalBulkApprovals bulk;
  @Autowired private BulkApprovalService inbox;
  @Autowired private SystemJournalService systemJournals;
  @Autowired private TestCompanies companies;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private JournalRequest rent(Long company, Long branch, String amount, LocalDate reverseOn) {
    return rent(company, branch, amount, CASH, reverseOn);
  }

  private JournalRequest rent(
      Long company, Long branch, String amount, String creditAccount, LocalDate reverseOn) {
    BigDecimal value = new BigDecimal(amount);
    return new JournalRequest(
        company,
        branch,
        JournalType.ACCRUAL,
        LocalDate.now(),
        "PHP",
        "Accrued rent",
        "ACR-" + UUID.randomUUID().toString().substring(0, 8),
        List.of(
            new JournalLineRequest(
                RENT, BalanceSide.DEBIT, value, null, null, null, "FIN", null, null, null, null),
            new JournalLineRequest(
                creditAccount,
                BalanceSide.CREDIT,
                value,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)),
        reverseOn);
  }

  private JournalBatch submitted(JournalRequest request) {
    JournalBatch draft = as.run("glofficer", () -> entries.createDraft(request));
    return as.run("glofficer", () -> entries.submit(draft.getId()));
  }

  @Test
  void accrualIsReversedAutomaticallyOnItsReversalDate() {
    LocalDate today = LocalDate.now();
    Long company = data.company().getId();
    Long branch = data.branch("HO").getId();
    JournalBatch accrual = submitted(rent(company, branch, "1234.00", today.plusDays(1)));
    assertThat(accrual.getReverseOn()).isEqualTo(today.plusDays(1));
    JournalBatch posted = as.run("gltl", () -> authorization.approve(accrual.getId()));
    assertThat(posted.getStatus()).isEqualTo(JournalStatus.POSTED);

    assertThat(reversals.reverseDue(today).reversed()).doesNotContain(posted.getBatchNo());
    JournalAutoReversalService.Result result = reversals.reverseDue(today.plusDays(1));
    assertThat(result.reversed()).hasSizeGreaterThanOrEqualTo(1);
    JournalBatch original = entries.get(posted.getId());
    assertThat(original.getStatus()).isEqualTo(JournalStatus.REVERSED);
    JournalBatch reversal = entries.get(original.getReversedById());
    assertThat(reversal.getJournalType()).isEqualTo(JournalType.REVERSAL);
    assertThat(reversal.getSourceModule()).isEqualTo(JournalAutoReversalService.SOURCE);
    assertThat(reversal.getReversalOfId()).isEqualTo(original.getId());
    assertThat(reversal.getValueDate()).isEqualTo(today.plusDays(1));
    assertThat(reversals.reverseDue(today.plusDays(1)).reversed())
        .doesNotContain(reversal.getBatchNo());

    assertThatThrownBy(
            () ->
                as.run(
                    "glofficer", () -> entries.createDraft(rent(company, branch, "10.00", today))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("after the value date");
  }

  @Test
  void journalsAreAssignedAndPostedInBulk() {
    Long company = data.company().getId();
    Long branch = data.branch("HO").getId();
    JournalBatch first = submitted(rent(company, branch, "101.00", null));
    JournalBatch second = submitted(rent(company, branch, "102.00", null));
    JournalBatch draft =
        as.run("glofficer", () -> entries.createDraft(rent(company, branch, "103.00", null)));

    as.run("gltl", () -> entries.assign(List.of(first.getId(), second.getId()), "glhead"));
    List<Long> assigned =
        entries
            .search(
                new JournalSearchCriteria(
                    company, null, null, null, null, null, null, null, null, null, null, "GLHEAD"),
                Pageable.unpaged())
            .map(JournalBatch::getId)
            .toList();
    assertThat(assigned).contains(first.getId(), second.getId()).doesNotContain(draft.getId());
    assertThatThrownBy(() -> as.run("gltl", () -> entries.assign(List.of(first.getId()), "ao")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not allowed to authorize");

    List<JournalBulkApprovals.Outcome> outcomes =
        as.run(
            "glhead", () -> bulk.approveAll(List.of(first.getId(), second.getId(), draft.getId())));
    assertThat(outcomes).filteredOn(JournalBulkApprovals.Outcome::posted).hasSize(2);
    assertThat(outcomes.get(2).posted()).isFalse();
    assertThat(entries.get(first.getId()).getStatus()).isEqualTo(JournalStatus.POSTED);
    as.run("gltl", () -> entries.assign(List.of(draft.getId()), null));
    assertThat(entries.get(draft.getId()).getAssignedTo()).isNull();

    JournalBatch third = submitted(rent(company, branch, "104.00", null));
    List<Outcome> fromInbox =
        as.run(
            "gltl",
            () ->
                inbox.approve(
                    List.of(
                        new Item("GL", "Journal (ACCRUAL)", third.getBatchNo(), company),
                        new Item("NOPE", "Thing", "X-1", company))));
    assertThat(fromInbox.get(0).approved()).isTrue();
    assertThat(fromInbox.get(1).approved()).isFalse();
    assertThat(inbox.supports("GL", "Journal (MANUAL)")).isTrue();
    assertThat(as.run("ao", () -> inbox.approve(List.of(new Item("GL", "Journal", "x", company)))))
        .allMatch(o -> !o.approved());
  }

  @Test
  void negativeBalanceControlBlocksOrWarnsAndShortCodesResolve() {
    LocalDate today = LocalDate.now();
    Long company = companies.create("TGLJ", "PHP").getId();
    companies.openYear(company, today.getYear());
    Long branch = companies.headOffice(company);
    jdbc.update(
        "update coa_account set short_name = 'CASHHO', negative_balance_policy = 'BLOCK'"
            + " where company_id = ? and code = ?",
        company,
        CASH);

    JournalBatch draft =
        as.run(
            "glofficer",
            () -> entries.createDraft(rent(company, branch, "500.00", "cashho", null)));
    assertThat(draft.getLines())
        .anyMatch(l -> CASH.equals(l.getAccount().getCode()) && l.getSide() == BalanceSide.CREDIT);
    assertThatThrownBy(() -> as.run("glofficer", () -> entries.submit(draft.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("negative (credit) balance");

    jdbc.update(
        "update coa_account set negative_balance_policy = 'WARN' where company_id = ? and code = ?",
        company,
        CASH);
    assertThat(as.run("glofficer", () -> entries.warnings(draft.getId())))
        .singleElement()
        .asString()
        .contains(CASH);
    JournalBatch submitted = as.run("glofficer", () -> entries.submit(draft.getId()));
    assertThat(submitted.getStatus()).isEqualTo(JournalStatus.PENDING_APPROVAL);
  }

  @Test
  void correctingSystemJournalCarriesItsLinks() {
    Long company = data.company().getId();
    Long branch = data.branch("HO").getId();
    JournalBatch original =
        as.run(
            "gltl",
            () -> authorization.approve(submitted(rent(company, branch, "55.00", null)).getId()));
    String key = UUID.randomUUID().toString();
    JournalBatch correction =
        as.run(
            "accountant",
            () ->
                systemJournals.post(
                    new SystemJournalRequest(
                            company,
                            branch,
                            JournalType.ADJUSTMENT,
                            LocalDate.now(),
                            "PHP",
                            "Correction of " + original.getBatchNo(),
                            "ACS-TEST",
                            "ACSL",
                            key,
                            List.of(
                                line(CASH, BalanceSide.DEBIT, "55.00"),
                                line(RENT, BalanceSide.CREDIT, "55.00", "FIN", null)))
                        .correcting(original.getId(), "SI-000123", "SI-000100")));
    assertThat(correction.getCorrectsBatchId()).isEqualTo(original.getId());
    assertThat(correction.getRelatedInvoiceNo()).isEqualTo("SI-000123");
    assertThat(correction.getRootInvoiceNo()).isEqualTo("SI-000100");
  }
}
