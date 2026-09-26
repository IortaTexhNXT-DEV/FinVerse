package com.iortatechnxt.brokerverse.subledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemStatus;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.brokerverse.subledger.service.AgeingService;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import com.iortatechnxt.brokerverse.support.TestParties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class OpenItemIT {

  @Autowired private OpenItemService openItems;
  @Autowired private AgeingService ageing;
  @Autowired private PartyService parties;
  @Autowired private TestParties testParties;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private OpenItem item(Party party, ItemDirection dir, String amount, LocalDate due) {
    String no = UUID.randomUUID().toString().substring(0, 12);
    return openItems.record(
        new OpenItemValues(
            data.company().getId(),
            data.branch("HO").getId(),
            party.getId(),
            party.getCode(),
            dir,
            dir == ItemDirection.DEBIT ? "DEBIT_NOTE" : "RECEIPT",
            no,
            due,
            due,
            "PHP",
            new BigDecimal(amount),
            new BigDecimal(amount),
            "TEST",
            no,
            null,
            "test"));
  }

  @Test
  void fifoAllocationSettlesOldestDebitsFirst() {
    Party client = testParties.create(PartyType.CORPORATE_CLIENT);
    as.run(
        "accountant",
        () -> {
          OpenItem older =
              item(
                  client,
                  ItemDirection.DEBIT,
                  "300.00",
                  BusinessClock.today(Clock.systemUTC()).minusDays(40));
          OpenItem newer =
              item(
                  client,
                  ItemDirection.DEBIT,
                  "500.00",
                  BusinessClock.today(Clock.systemUTC()).minusDays(5));
          OpenItem receipt =
              item(client, ItemDirection.CREDIT, "600.00", BusinessClock.today(Clock.systemUTC()));

          openItems.allocateFifo(receipt.getId(), BusinessClock.today(Clock.systemUTC()));

          assertThat(openItems.get(older.getId()).getStatus()).isEqualTo(OpenItemStatus.SETTLED);
          assertThat(openItems.get(newer.getId()).outstanding()).isEqualByComparingTo("200.00");
          assertThat(openItems.get(receipt.getId()).getStatus()).isEqualTo(OpenItemStatus.SETTLED);

          var rows =
              ageing.age(
                  data.company().getId(),
                  BusinessClock.today(Clock.systemUTC()),
                  AgeingService.DEFAULT_BUCKETS,
                  i -> i.getPartyCode().equals(client.getCode()));
          assertThat(rows)
              .singleElement()
              .satisfies(r -> assertThat(r.total()).isEqualByComparingTo("200.00"));
          return null;
        });
  }

  @Test
  void matchingDifferentPartiesIsRejected() {
    as.run(
        "accountant",
        () -> {
          OpenItem a =
              item(
                  parties.getByCode(data.company().getId(), "C-000101"),
                  ItemDirection.DEBIT,
                  "10.00",
                  BusinessClock.today(Clock.systemUTC()));
          OpenItem b =
              item(
                  parties.getByCode(data.company().getId(), "C-000102"),
                  ItemDirection.CREDIT,
                  "10.00",
                  BusinessClock.today(Clock.systemUTC()));
          assertThatThrownBy(
                  () ->
                      openItems.match(
                          a.getId(), b.getId(), null, BusinessClock.today(Clock.systemUTC())))
              .isInstanceOf(BusinessRuleException.class);
          return null;
        });
  }

  @Test
  void overSettlementIsRejected() {
    as.run(
        "accountant",
        () -> {
          Party client = parties.getByCode(data.company().getId(), "C-000204");
          OpenItem d =
              item(client, ItemDirection.DEBIT, "10.00", BusinessClock.today(Clock.systemUTC()));
          OpenItem c =
              item(client, ItemDirection.CREDIT, "50.00", BusinessClock.today(Clock.systemUTC()));
          assertThatThrownBy(
                  () ->
                      openItems.match(
                          d.getId(),
                          c.getId(),
                          new BigDecimal("20.00"),
                          BusinessClock.today(Clock.systemUTC())))
              .isInstanceOf(BusinessRuleException.class);
          return null;
        });
  }
}
