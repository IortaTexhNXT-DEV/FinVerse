package com.iortatechnxt.brokerverse.accounting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.accounting.domain.AccountingEventLogRepository;
import com.iortatechnxt.brokerverse.accounting.domain.EventStatus;
import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalStatus;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.Clock;
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

  private BusinessEvent supplierInvoice(String key) {
    return new BusinessEvent(
        "SUPPLIER_INVOICE",
        data.company().getId(),
        data.branch("HO").getId(),
        BusinessClock.today(Clock.systemUTC()),
        "PHP",
        "PAYABLES",
        key,
        "INV-" + key,
        "S-0002",
        null,
        "IT",
        "Cloud services invoice",
        Map.of(
            "NET_AMOUNT", new BigDecimal("10000.00"),
            "INPUT_VAT", new BigDecimal("1200.00"),
            "WITHHOLDING_TAX", new BigDecimal("200.00"),
            "PAYABLE", new BigDecimal("11000.00")),
        Map.of("EXPENSE", "5610"));
  }

  @Test
  void supplierInvoicePostsBalancedJournalAndIsIdempotent() {
    Long company = data.company().getId();
    Long payable = accounts.getByCode(company, "2501").getId();
    BigDecimal before =
        ledger.netBalance(company, payable, null, BusinessClock.today(Clock.systemUTC()));
    String key = UUID.randomUUID().toString();

    JournalBatch batch = as.run("accountant", () -> publisher.publish(supplierInvoice(key)));

    assertThat(batch.getStatus()).isEqualTo(JournalStatus.POSTED);
    assertThat(batch.getJournalType()).isEqualTo(JournalType.PAYMENT);
    assertThat(batch.getTotalDebit()).isEqualByComparingTo("11200.00");
    assertThat(batch.getLines()).anyMatch(l -> "S-0002".equals(l.getPartyCode()));
    assertThat(ledger.netBalance(company, payable, null, BusinessClock.today(Clock.systemUTC())))
        .isEqualByComparingTo(before.subtract(new BigDecimal("11000.00")));

    JournalBatch again = as.run("accountant", () -> publisher.publish(supplierInvoice(key)));
    assertThat(again.getId()).isEqualTo(batch.getId());
  }

  @Test
  void negativeAmountsReverseSides() {
    String key = UUID.randomUUID().toString();
    BusinessEvent refund =
        new BusinessEvent(
            "MISC_PAYMENT",
            data.company().getId(),
            data.branch("HO").getId(),
            BusinessClock.today(Clock.systemUTC()),
            "PHP",
            "PAYABLES",
            key,
            "PV-" + key,
            null,
            null,
            "IT",
            "Payment reversed",
            Map.of("AMOUNT", new BigDecimal("-5000.00")),
            Map.of("EXPENSE", "5610", "BANK", "1111"));
    JournalBatch batch = as.run("accountant", () -> publisher.publish(refund));
    assertThat(batch.getLines())
        .anyMatch(
            l -> "5610".equals(l.getAccount().getCode()) && l.getSide().name().equals("CREDIT"))
        .anyMatch(
            l -> "1111".equals(l.getAccount().getCode()) && l.getSide().name().equals("DEBIT"));
  }

  @Test
  void missingAccountRoleFailsAndIsLogged() {
    String key = UUID.randomUUID().toString();
    BusinessEvent receipt =
        new BusinessEvent(
            "PREMIUM_RECEIPT",
            data.company().getId(),
            data.branch("HO").getId(),
            BusinessClock.today(Clock.systemUTC()),
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
                    BusinessClock.today(Clock.systemUTC()),
                    BusinessClock.today(Clock.systemUTC()),
                    Pageable.unpaged())
                .getContent())
        .anyMatch(e -> key.equals(e.getSourceReference()));
  }
}
