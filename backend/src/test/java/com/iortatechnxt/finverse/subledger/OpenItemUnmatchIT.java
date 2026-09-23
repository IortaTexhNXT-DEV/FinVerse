package com.iortatechnxt.finverse.subledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.domain.OpenItemStatus;
import com.iortatechnxt.finverse.subledger.domain.OpenItemValues;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Unmatching (undoing a knock-off) in the open-item sub-ledger. */
@IntegrationTest
class OpenItemUnmatchIT {

  private static final LocalDate DATE = LocalDate.of(2026, 5, 4);

  @Autowired private OpenItemService openItems;
  @Autowired private PartyService parties;
  @Autowired private AsUser as;
  @Autowired private TestData data;

  private OpenItem item(Party party, ItemDirection dir, String amount) {
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
            DATE,
            DATE,
            "PHP",
            new BigDecimal(amount),
            new BigDecimal(amount),
            "TEST",
            no,
            null,
            "unmatch test"));
  }

  @Test
  void unmatchRestoresBothItems() {
    as.run(
        "accountant",
        () -> {
          Party party = parties.getByCode(data.company().getId(), "C-000102");
          OpenItem debit = item(party, ItemDirection.DEBIT, "500.00");
          OpenItem credit = item(party, ItemDirection.CREDIT, "300.00");
          OpenItem other = item(party, ItemDirection.DEBIT, "100.00");
          var first =
              openItems.match(debit.getId(), credit.getId(), new BigDecimal("200.00"), DATE);
          openItems.match(other.getId(), credit.getId(), new BigDecimal("100.00"), DATE);
          assertThat(openItems.get(credit.getId()).getStatus()).isEqualTo(OpenItemStatus.SETTLED);

          openItems.unmatch(first.getId(), "test");
          assertThat(openItems.get(debit.getId()).getStatus()).isEqualTo(OpenItemStatus.OPEN);
          assertThat(openItems.get(credit.getId()).getStatus())
              .isEqualTo(OpenItemStatus.PARTIALLY_SETTLED);
          assertThatThrownBy(() -> openItems.unmatch(first.getId(), "again"))
              .isInstanceOf(ResourceNotFoundException.class);

          assertThat(openItems.unmatchAll(credit.getId(), "all")).hasSize(1);
          assertThat(openItems.get(credit.getId()).outstanding()).isEqualByComparingTo("300.00");
          assertThat(openItems.get(other.getId()).outstanding()).isEqualByComparingTo("100.00");
          return null;
        });
  }

  @Test
  void unsettleValidatesTheAmount() {
    Party party = parties.getByCode(data.company().getId(), "C-000102");
    OpenItem debit = as.run("accountant", () -> item(party, ItemDirection.DEBIT, "50.00"));
    assertThatThrownBy(() -> debit.unsettle(new BigDecimal("1.00")))
        .isInstanceOf(BusinessRuleException.class);
    debit.settle(new BigDecimal("10.00"));
    debit.writeOff();
    assertThatThrownBy(() -> debit.unsettle(new BigDecimal("10.00")))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("written off");
  }
}
