package com.iortatechnxt.finverse.reinsurance.demo;

import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest.LayerRequest;
import com.iortatechnxt.finverse.reinsurance.api.dto.TreatyRequest.ParticipantRequest;
import com.iortatechnxt.finverse.reinsurance.domain.TreatyType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Static definition of the demo reinsurance programme for underwriting year 2026: fire quota share
 * 40 % with a 3-line surplus, marine quota share, engineering surplus and a two-layer motor excess
 * of loss, placed with a local reinsurer, two Asian reinsurers and a London market reinsurer,
 * partly through a reinsurance broker. Quota shares withhold half of the reinsurers' outstanding
 * losses as loss reserve; proportional treaties pay 3 % interest on reserves withheld.
 */
final class DemoTreaties {

  /** Programme year. */
  static final int YEAR = 2026;

  /** Local reinsurer. */
  static final String LOCAL = "R-0001";

  /** Asian reinsurer (Singapore). */
  static final String ASIA = "R-0002";

  /** London market reinsurer. */
  static final String LONDON = "R-0003";

  /** Asian reinsurer (Hong Kong). */
  static final String PAN_ASIA = "R-0004";

  /** Reinsurance broker. */
  static final String BROKER = "RB-0001";

  private static final String CURRENCY = "PHP";
  private static final LocalDate FROM = LocalDate.of(YEAR, 1, 1);
  private static final LocalDate TO = LocalDate.of(YEAR, 12, 31);
  private static final BigDecimal NIL = BigDecimal.ZERO;
  private static final BigDecimal P10 = BigDecimal.valueOf(10);
  private static final BigDecimal P20 = BigDecimal.valueOf(20);
  private static final BigDecimal P25 = BigDecimal.valueOf(25);
  private static final BigDecimal P27_5 = new BigDecimal("27.5");
  private static final BigDecimal P30 = BigDecimal.valueOf(30);
  private static final BigDecimal P32_5 = new BigDecimal("32.5");
  private static final BigDecimal P35 = BigDecimal.valueOf(35);
  private static final BigDecimal P40 = BigDecimal.valueOf(40);
  private static final BigDecimal P50 = BigDecimal.valueOf(50);
  private static final BigDecimal P60 = BigDecimal.valueOf(60);
  private static final BigDecimal LEVY = new BigDecimal("0.5");
  private static final BigDecimal INTEREST = BigDecimal.valueOf(3);
  private static final BigDecimal LINE_25M = new BigDecimal("25000000");
  private static final int FIRE_LINES = 3;
  private static final int ENGINEERING_LINES = 5;

  private DemoTreaties() {}

  /**
   * Treaty requests of the demo programme.
   *
   * @param companyId demo company
   * @return requests
   */
  static List<TreatyRequest> requests(Long companyId) {
    return List.of(
        proportional(
            companyId,
            new Terms("FIRE-QS-26", "Fire Quota Share 2026", TreatyType.QUOTA_SHARE, "FIRE"),
            new Capacity(P40, null, null, null),
            BROKER,
            List.of(
                participant(LOCAL, P50, P30, P10, NIL),
                participant(ASIA, P30, P32_5, P10, P20),
                participant(PAN_ASIA, P20, P32_5, P10, P20))),
        proportional(
            companyId,
            new Terms("FIRE-SP-26", "Fire 3-Line Surplus 2026", TreatyType.SURPLUS, "FIRE"),
            new Capacity(null, null, LINE_25M, FIRE_LINES),
            BROKER,
            List.of(
                participant(ASIA, P40, P32_5, NIL, P20),
                participant(LONDON, P35, P30, NIL, NIL),
                participant(PAN_ASIA, P25, P32_5, NIL, P20))),
        proportional(
            companyId,
            new Terms(
                "MAR-QS-26", "Marine Cargo Quota Share 2026", TreatyType.QUOTA_SHARE, "MARINE"),
            new Capacity(P50, new BigDecimal("30000000"), null, null),
            null,
            List.of(
                participant(LOCAL, P60, P25, NIL, NIL), participant(ASIA, P40, P27_5, NIL, NIL))),
        proportional(
            companyId,
            new Terms("ENG-SP-26", "Engineering 5-Line Surplus 2026", TreatyType.SURPLUS, "ENGG"),
            new Capacity(null, null, LINE_25M, ENGINEERING_LINES),
            BROKER,
            List.of(
                participant(LONDON, P50, P30, NIL, NIL),
                participant(PAN_ASIA, P50, P30, NIL, P20))),
        excessOfLoss(companyId));
  }

  private static TreatyRequest proportional(
      Long companyId, Terms t, Capacity c, String broker, List<ParticipantRequest> participants) {
    return new TreatyRequest(
        companyId,
        t.code(),
        t.name(),
        t.type(),
        t.lob(),
        YEAR,
        FROM,
        TO,
        CURRENCY,
        c.quotaShare(),
        c.limit(),
        c.retention(),
        c.lines(),
        LEVY,
        INTEREST,
        c.quotaShare() == null ? NIL : P50,
        broker,
        participants,
        List.of());
  }

  private static TreatyRequest excessOfLoss(Long companyId) {
    return new TreatyRequest(
        companyId,
        "MOT-XL-26",
        "Motor Excess of Loss 2026",
        TreatyType.XOL,
        "MOTOR",
        YEAR,
        FROM,
        TO,
        CURRENCY,
        null,
        null,
        null,
        null,
        NIL,
        NIL,
        NIL,
        null,
        List.of(participant(LOCAL, P50, NIL, NIL, NIL), participant(ASIA, P50, NIL, NIL, NIL)),
        List.of(
            new LayerRequest(
                new BigDecimal("500000"), new BigDecimal("2000000"), new BigDecimal("1200000"), 1),
            new LayerRequest(
                new BigDecimal("2500000"),
                new BigDecimal("5000000"),
                new BigDecimal("600000"),
                1)));
  }

  private static ParticipantRequest participant(
      String code,
      BigDecimal share,
      BigDecimal commission,
      BigDecimal profitCommission,
      BigDecimal reserve) {
    return new ParticipantRequest(code, share, commission, profitCommission, reserve);
  }

  /** Identification of a demo treaty. */
  private record Terms(String code, String name, TreatyType type, String lob) {}

  /** Capacity terms of a proportional demo treaty. */
  private record Capacity(
      BigDecimal quotaShare, BigDecimal limit, BigDecimal retention, Integer lines) {}
}
