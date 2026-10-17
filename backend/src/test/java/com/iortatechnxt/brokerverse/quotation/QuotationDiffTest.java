package com.iortatechnxt.brokerverse.quotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.account.domain.AccountPremium;
import com.iortatechnxt.brokerverse.account.domain.InsuredItem;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.domain.RiskItemData.Location;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationContent;
import com.iortatechnxt.brokerverse.quotation.domain.QuotationItem;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDiff;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDiff.Change;
import com.iortatechnxt.brokerverse.quotation.service.QuotationDiff.ItemChange;
import com.iortatechnxt.brokerverse.quotation.service.QuotationPricing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuotationDiffTest {

  private static AccountPremium gross(String amount) {
    return new AccountPremium(
        "ANNUAL",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        new BigDecimal(amount),
        null,
        null,
        null,
        false);
  }

  private static QuotationItem item(int group, String description, String sum, String premium) {
    return new QuotationItem(
        group,
        RiskItemData.generic(description, new BigDecimal(sum), BigDecimal.ONE),
        new BigDecimal(premium),
        BigDecimal.ONE);
  }

  private static QuotationContent content(
      LocalDate validUntil, AccountPremium premium, QuotationItem... items) {
    return new QuotationContent(
        "INS-A", "B1", null, null, validUntil, false, "ANNUAL", null, List.of(items), premium);
  }

  @Test
  void reportsChangedTermsItemsAndPremiumDelta() {
    QuotationContent v1 =
        content(
            LocalDate.of(2026, 10, 1),
            gross("1000.00"),
            item(1, "Warehouse", "100", "10"),
            item(1, "Office", "50", "5"));
    QuotationContent v2 =
        content(
            LocalDate.of(2026, 11, 1),
            gross("1250.50"),
            item(1, "Warehouse", "150", "15"),
            item(2, "Depot", "40", "4"));
    QuotationDiff diff = QuotationDiff.between(1, v1, 2, v2);
    assertThat(diff.fields())
        .singleElement()
        .satisfies(
            f -> {
              assertThat(f.field()).isEqualTo("Valid until");
              assertThat(f.from()).isEqualTo("2026-10-01");
            });
    assertThat(diff.items())
        .extracting(ItemChange::change, ItemChange::risk)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(Change.CHANGED, "Warehouse"),
            org.assertj.core.groups.Tuple.tuple(Change.REMOVED, "Office"),
            org.assertj.core.groups.Tuple.tuple(Change.ADDED, "Depot"));
    assertThat(diff.grossDelta()).isEqualByComparingTo("250.50");
    assertThat(QuotationDiff.between(1, v1, 1, v1).items()).isEmpty();
    assertThat(QuotationDiff.between(1, v1, 2, content(null, AccountPremium.NONE)).grossDelta())
        .isNull();
  }

  @Test
  void sameRisksAreKeptApartAndLocationsSumTheirInsuredItems() {
    QuotationContent twins =
        content(null, gross("1"), item(1, "Car", "1", "1"), item(1, "Car", "2", "1"));
    assertThat(QuotationDiff.between(1, twins, 2, content(null, gross("1"))).items())
        .extracting(ItemChange::risk)
        .containsExactly("Car", "Car (2)");
    RiskItemData site =
        new RiskItemData(
            null,
            null,
            null,
            null,
            null,
            null,
            new Location(
                "1 Main St",
                null,
                null,
                null,
                null,
                List.of(
                    new InsuredItem("Building", new BigDecimal("700")),
                    new InsuredItem("Stock", new BigDecimal("300")))),
            null);
    assertThat(QuotationPricing.sumInsured(site)).isEqualByComparingTo("1000");
    assertThat(QuotationPricing.label(site)).isEqualTo("1 Main St");
    assertThat(QuotationPricing.label(RiskItemData.generic(null, null, null))).isEqualTo("Item");
    assertThat(twins.groups()).containsExactly(1);
    assertThat(twins.isRated()).isTrue();
  }
}
