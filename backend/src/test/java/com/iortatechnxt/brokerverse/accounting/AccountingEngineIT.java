package com.iortatechnxt.brokerverse.accounting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventLogRepository;
import com.iortatechnxt.brokerverse.accounting.domain.EventStatus;
import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalStatus;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

@IntegrationTest
class AccountingEngineIT {

  @Autowired private AccountingEventPublisher publisher;
  @Autowired private LedgerQueryService ledger;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private AccountingEventLogRepository eventLog;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private BusinessEvent policyIssue(String key) {
    return new BusinessEvent(
        "POLICY_ISSUE",
        data.company().getId(),
        data.branch("HO").getId(),
        LocalDate.now(),
        "PHP",
        "UNDERWRITING",
        key,
        "POL-" + key,
        "C-000201",
        "FIRE",
        null,
        "Fire policy issued",
        Map.of(
            "GROSS_PREMIUM", new BigDecimal("100000.00"),
            "DST", new BigDecimal("12500.00"),
            "VAT", new BigDecimal("12000.00"),
            "LGT", new BigDecimal("500.00"),
            "FST", new BigDecimal("2000.00"),
            "TOTAL_DUE", new BigDecimal("127000.00")),
        Map.of());
  }

  @Test
  void policyIssuePostsBalancedPremiumJournalAndIsIdempotent() {
    Long company = data.company().getId();
    Long receivable = accounts.getByCode(company, "1201").getId();
    BigDecimal before = ledger.netBalance(company, receivable, null, LocalDate.now());
    String key = UUID.randomUUID().toString();

    JournalBatch batch = as.run("uw", () -> publisher.publish(policyIssue(key)));

    assertThat(batch.getStatus()).isEqualTo(JournalStatus.POSTED);
    assertThat(batch.getJournalType()).isEqualTo(JournalType.PREMIUM);
    assertThat(batch.getTotalDebit()).isEqualByComparingTo("127000.00");
    assertThat(batch.getLines()).anyMatch(l -> "C-000201".equals(l.getPartyCode()));
    assertThat(ledger.netBalance(company, receivable, null, LocalDate.now()))
        .isEqualByComparingTo(before.add(new BigDecimal("127000.00")));

    JournalBatch again = as.run("uw", () -> publisher.publish(policyIssue(key)));
    assertThat(again.getId()).isEqualTo(batch.getId());
  }

  @Test
  void negativeAmountsReverseSides() {
    String key = UUID.randomUUID().toString();
    BusinessEvent release =
        new BusinessEvent(
            "CLAIM_RESERVE",
            data.company().getId(),
            data.branch("HO").getId(),
            LocalDate.now(),
            "PHP",
            "CLAIMS",
            key,
            "CLM-" + key,
            null,
            "MOTOR",
            null,
            "Reserve reduced",
            Map.of("RESERVE_CHANGE", new BigDecimal("-5000.00")),
            Map.of());
    JournalBatch batch = as.run("claims", () -> publisher.publish(release));
    assertThat(batch.getLines())
        .anyMatch(
            l -> "2102".equals(l.getAccount().getCode()) && l.getSide().name().equals("DEBIT"));
  }

  @Test
  void missingAccountRoleFailsAndIsLogged() {
    String key = UUID.randomUUID().toString();
    BusinessEvent receipt =
        new BusinessEvent(
            "PREMIUM_RECEIPT",
            data.company().getId(),
            data.branch("HO").getId(),
            LocalDate.now(),
            "PHP",
            "RECEIPTS",
            key,
            "OR-" + key,
            "C-000201",
            null,
            null,
            "Receipt",
            Map.of("AMOUNT", new BigDecimal("1000.00")),
            Map.of());
    assertThatThrownBy(() -> as.run("accountant", () -> publisher.publish(receipt)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("BANK");
    assertThat(
            eventLog
                .search(
                    data.company().getId(),
                    EventStatus.FAILED,
                    "PREMIUM_RECEIPT",
                    LocalDate.now(),
                    LocalDate.now(),
                    Pageable.unpaged())
                .getContent())
        .anyMatch(e -> key.equals(e.getSourceReference()));
  }
}
