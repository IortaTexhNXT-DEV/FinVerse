package com.iortatechnxt.brokerverse.nonpackage;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.nonpackage.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseStatus;
import com.iortatechnxt.brokerverse.nonpackage.domain.ResponseTerms;
import com.iortatechnxt.brokerverse.nonpackage.domain.RiskDetails;
import com.iortatechnxt.brokerverse.nonpackage.service.ComparativeTable;
import com.iortatechnxt.brokerverse.nonpackage.service.ComparativeTable.Row;
import com.iortatechnxt.brokerverse.nonpackage.service.ProposalRules;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ComparativeTableTest {

  private static InsurerResponse response(String code, ResponseStatus status, String premium) {
    InsurerResponse r = new InsurerResponse(1L, code, code + " Insurance");
    if (status != ResponseStatus.PENDING) {
      r.record(
          new ResponseTerms(
              status,
              premium == null ? null : new BigDecimal(premium),
              null,
              null,
              null,
              null,
              null),
          Instant.EPOCH);
    }
    return r;
  }

  @Test
  void receivedTermsComeFirstCheapestFirstWithLowestAndRecommendedFlags() {
    InsurerResponse pending = response("P", ResponseStatus.PENDING, null);
    InsurerResponse dear = response("D", ResponseStatus.RECEIVED, "900");
    InsurerResponse cheap = response("C", ResponseStatus.RECEIVED, "700");
    InsurerResponse declined = response("X", ResponseStatus.DECLINED, null);
    dear.recommend(true);
    ComparativeTable table = ComparativeTable.of(List.of(pending, dear, cheap, declined));
    assertThat(table.rows()).extracting(Row::insurerCode).containsExactly("C", "D", "P", "X");
    assertThat(table.rows()).extracting(Row::lowest).containsExactly(true, false, false, false);
    assertThat(table.recommendedInsurer()).isEqualTo("D");
    assertThat(dear.getRevision()).isEqualTo(2);
    declined.recommend(false);
    assertThat(declined.getRevision()).isEqualTo(1);
    assertThat(ComparativeTable.of(List.of(pending)).recommendedInsurer()).isNull();
  }

  @Test
  void riskDetailsDefaultToOneGroupAndSumTheirItems() {
    assertThat(RiskDetails.EMPTY.groups()).containsExactly(1);
    RiskDetails details =
        new RiskDetails(
            null,
            List.of(
                new RiskDetails.Item(2, RiskItemData.generic("a", new BigDecimal("5"), null)),
                new RiskDetails.Item(1, RiskItemData.generic("b", null, null))));
    assertThat(details.groups()).containsExactly(1, 2);
    assertThat(details.itemsOf(2)).hasSize(1);
    assertThat(ProposalRules.sumInsured(details)).isEqualByComparingTo("5");
  }
}
