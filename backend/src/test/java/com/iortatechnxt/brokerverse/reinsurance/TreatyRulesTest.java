package com.iortatechnxt.brokerverse.reinsurance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import com.iortatechnxt.brokerverse.reinsurance.domain.FacParticipant;
import com.iortatechnxt.brokerverse.reinsurance.domain.Participation;
import com.iortatechnxt.brokerverse.reinsurance.domain.RiLayer;
import com.iortatechnxt.brokerverse.reinsurance.domain.Treaty;
import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyLayer;
import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyParticipant;
import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyParticipant.ParticipantTerms;
import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyTerms;
import com.iortatechnxt.brokerverse.reinsurance.domain.TreatyType;
import com.iortatechnxt.brokerverse.reinsurance.service.TreatyProgramme;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Treaty invariants, programme capacity and participation keys. */
class TreatyRulesTest {

  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 12, 31);

  private static Party reinsurer(long id, String code) {
    Party p = new Party(1L, code, "Reinsurer " + code, PartyType.REINSURER, "PHP");
    ReflectionTestUtils.setField(p, "id", id);
    return p;
  }

  private static TreatyTerms terms(TreatyType type, String qs, String retention, Integer lines) {
    return new TreatyTerms(
        "Test",
        type,
        "FIRE",
        2026,
        FROM,
        TO,
        "PHP",
        qs == null ? null : new BigDecimal(qs),
        null,
        retention == null ? null : new BigDecimal(retention),
        lines,
        null,
        null,
        null);
  }

  private static List<TreatyParticipant> shares(String... pcts) {
    List<TreatyParticipant> out = new java.util.ArrayList<>();
    int line = 1;
    for (String pct : pcts) {
      out.add(
          new TreatyParticipant(
              line,
              reinsurer(line, "R-T" + line),
              new ParticipantTerms(new BigDecimal(pct), new BigDecimal("30"), null, null)));
      line++;
    }
    return out;
  }

  @Test
  void proportionalTreatiesNeedTheirTermsAndFullShares() {
    Treaty qs = new Treaty(1L, "QS", terms(TreatyType.QUOTA_SHARE, "40", null, null));
    qs.define(terms(TreatyType.QUOTA_SHARE, "40", null, null), null, shares("60", "40"), List.of());
    assertThat(qs.getParticipants()).hasSize(2);
    assertThat(qs.quotaShareFraction()).isEqualByComparingTo("0.4");
    assertThat(qs.covers(LocalDate.of(2026, 6, 30))).isTrue();
    assertThat(qs.covers(LocalDate.of(2027, 1, 1))).isFalse();
    assertThat(qs.getStatementFrequency()).isEqualTo("QUARTERLY");
    assertThat(qs.getLevyPct()).isZero();

    Treaty bad = new Treaty(1L, "QS2", terms(TreatyType.QUOTA_SHARE, "40", null, null));
    assertThatThrownBy(
            () ->
                bad.define(
                    terms(TreatyType.QUOTA_SHARE, "40", null, null),
                    null,
                    shares("60", "30"),
                    List.of()))
        .hasMessageContaining("100");
    Treaty surplus = new Treaty(1L, "SP", terms(TreatyType.SURPLUS, null, null, null));
    assertThatThrownBy(
            () ->
                surplus.define(
                    terms(TreatyType.SURPLUS, null, null, null), null, shares("100"), List.of()))
        .hasMessageContaining("surplus");
    assertThat(surplus.quotaShareFraction()).isZero();
  }

  @Test
  void excessOfLossNeedsALayer() {
    Treaty xol = new Treaty(1L, "XL", terms(TreatyType.XOL, null, null, null));
    assertThatThrownBy(
            () ->
                xol.define(terms(TreatyType.XOL, null, null, null), null, shares("100"), List.of()))
        .hasMessageContaining("layer");
    TreatyLayer layer =
        new TreatyLayer(1, new BigDecimal("500000"), new BigDecimal("1000000"), null, 2);
    xol.define(terms(TreatyType.XOL, null, null, null), null, shares("100"), List.of(layer));
    assertThat(xol.getLayers()).hasSize(1);
    assertThat(layer.aggregateLimit()).isEqualByComparingTo("3000000");
    assertThat(layer.getMinDepositPremium()).isZero();
    assertThat(TreatyType.XOL.isProportional()).isFalse();
    assertThat(RiLayer.of(TreatyType.XOL)).isEqualTo(RiLayer.XOL);
    assertThat(RiLayer.FAC.isProportionalTreaty()).isFalse();
  }

  @Test
  void programmeCapacityAndParticipationKeys() {
    Treaty qs = new Treaty(1L, "QS", terms(TreatyType.QUOTA_SHARE, "50", null, null));
    Treaty sp = new Treaty(1L, "SP", terms(TreatyType.SURPLUS, null, "1000", 2));
    assertThat(TreatyProgramme.of(List.of(qs, sp)).capacity().line()).isEqualByComparingTo("1000");
    assertThat(TreatyProgramme.of(List.of(sp)).capacity().quotaShareFraction()).isZero();
    assertThat(TreatyProgramme.of(List.of(qs)).capacity().line()).isNull();
    assertThat(TreatyProgramme.EMPTY.isProportional()).isFalse();
    assertThat(TreatyProgramme.of(List.of(qs)).isProportional()).isTrue();

    assertThat(Participation.retention().isCeded()).isFalse();
    assertThat(Participation.retention().contractKey()).isNull();
    assertThat(new Participation(RiLayer.FAC, null, 7L, 1L, "R", BigDecimal.ZERO).contractKey())
        .isEqualTo("F7");
    assertThat(new FacParticipant(1, reinsurer(9, "R-9"), BigDecimal.TEN, null).getCommissionPct())
        .isZero();
  }
}
