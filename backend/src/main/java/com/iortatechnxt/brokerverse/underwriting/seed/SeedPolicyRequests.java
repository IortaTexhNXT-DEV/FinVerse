package com.iortatechnxt.brokerverse.underwriting.seed;

import static com.iortatechnxt.brokerverse.underwriting.seed.SeedCatalog.draw;

import com.iortatechnxt.brokerverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.RiskRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.BusinessType;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.domain.SourceType;
import com.iortatechnxt.brokerverse.underwriting.seed.SeedCatalog.ProductProfile;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Builds the deterministic policy requests of the seed portfolio: client, channel, period, share,
 * discount / loading and risks (sums insured and rates within each product's profile).
 */
final class SeedPolicyRequests {

  /** Number of seed policies. */
  static final int POLICY_COUNT = 150;

  /** Cycle of the short-term (renewed) policies. */
  static final int SHORT_TERM_CYCLE = 19;

  private static final LocalDate START = LocalDate.of(2026, 1, 5);
  private static final int SPREAD_DAYS = 256;
  private static final int SHORT_TERM_DAYS = 180;
  private static final int LAST_SHORT_TERM_MONTH = 3;
  private static final BigDecimal BASIS_POINTS = BigDecimal.valueOf(10_000);
  private static final BigDecimal USD_SCALE = BigDecimal.valueOf(50);
  private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
  private static final List<SourceType> SOURCES =
      List.of(
          SourceType.DIRECT,
          SourceType.DIRECT,
          SourceType.AGENT,
          SourceType.BROKER,
          SourceType.BROKER);
  private static final int COINSURANCE_CYCLE = 15;
  private static final int COINSURANCE_SLOT = 4;
  private static final String SHARE_COINSURED = "60";
  private static final int SALT_CLIENT = 1;
  private static final int SALT_BRANCH = 2;
  private static final int SALT_SI = 3;
  private static final int SALT_RATE = 4;
  private static final int SALT_ZONE = 5;
  private static final int CORPORATE_CLIENTS = 4;
  private static final int DISCOUNT_CYCLE = 4;
  private static final int LOADING_CYCLE = 6;
  private static final int RATE_SCALE = 4;
  private static final int DRAW_SCALE = 1000;
  private static final BigDecimal MAIN_BUILDING_SHARE = new BigDecimal("0.7");

  private SeedPolicyRequests() {}

  /**
   * Request of seed policy number {@code i}.
   *
   * @param i policy index
   * @param companyId company
   * @param branches branch ids by code
   * @param product product
   * @param p product profile
   * @return request
   */
  static PolicyRequest request(
      int i, Long companyId, Map<String, Long> branches, Product product, ProductProfile p) {
    int client = client(i, p);
    String clientCode = SeedCatalog.CLIENTS.get(client);
    boolean usd = "MARINE".equals(p.lob()) && "C-000202".equals(clientCode);
    LocalDate issue = START.plusDays((long) i * SPREAD_DAYS / POLICY_COUNT);
    boolean coinsured = i % COINSURANCE_CYCLE == COINSURANCE_SLOT;
    SourceType source = SOURCES.get(i % SOURCES.size());
    return new PolicyRequest(
        companyId,
        branches.get(SeedCatalog.BRANCHES.get(draw(i, SALT_BRANCH) % SeedCatalog.BRANCHES.size())),
        product.getId(),
        clientCode,
        SeedCatalog.INSURED.get(client),
        source,
        intermediary(i, source),
        issue,
        issue,
        periodTo(i, issue),
        usd ? "USD" : "PHP",
        coinsured ? BusinessType.DIRECT_WITH_COINSURANCE : BusinessType.DIRECT,
        new BigDecimal(coinsured ? SHARE_COINSURED : "100"),
        coinsured ? "CO-0001" : null,
        coinsured && i % (2 * COINSURANCE_CYCLE) == COINSURANCE_SLOT,
        i % DISCOUNT_CYCLE == 0 ? new BigDecimal("5") : BigDecimal.ZERO,
        i % LOADING_CYCLE == 1 ? BigDecimal.TEN : BigDecimal.ZERO,
        null,
        risks(i, p, usd));
  }

  /** Individuals buy motor and personal accident; companies everything else. */
  private static int client(int i, ProductProfile p) {
    boolean retail = "MOTOR".equals(p.lob()) || "PA".equals(p.lob());
    return retail ? i % 2 : 2 + draw(i, SALT_CLIENT) % CORPORATE_CLIENTS;
  }

  /** Annual cover, except a few short-term (180 days) policies early in the year. */
  private static LocalDate periodTo(int i, LocalDate issue) {
    boolean shortTerm = i % SHORT_TERM_CYCLE == 1 && issue.getMonthValue() <= LAST_SHORT_TERM_MONTH;
    return shortTerm ? issue.plusDays(SHORT_TERM_DAYS - 1L) : issue.plusYears(1).minusDays(1);
  }

  private static String intermediary(int i, SourceType source) {
    return switch (source) {
      case AGENT -> i % 2 == 0 ? "A-0001" : "A-0002";
      case BROKER -> i % 2 == 0 ? "B-0001" : "B-0002";
      case DIRECT -> null;
    };
  }

  private static List<RiskRequest> risks(int i, ProductProfile p, boolean usd) {
    long range = p.maxSumInsured() - p.minSumInsured();
    BigDecimal si =
        BigDecimal.valueOf(p.minSumInsured() + range * draw(i, SALT_SI) / DRAW_SCALE)
            .divide(THOUSAND, 0, RoundingMode.HALF_EVEN)
            .multiply(THOUSAND);
    if (usd) {
      si = si.divide(USD_SCALE, 0, RoundingMode.HALF_EVEN);
    }
    int rateBp = p.minRateBp() + (p.maxRateBp() - p.minRateBp()) * draw(i, SALT_RATE) / DRAW_SCALE;
    String zone = SeedCatalog.ZONES.get(draw(i, SALT_ZONE) % SeedCatalog.ZONES.size());
    String occupation = SeedCatalog.OCCUPATIONS.get(i % SeedCatalog.OCCUPATIONS.size());
    boolean property = "FIRE".equals(p.lob()) || "ENGG".equals(p.lob());
    if (!property) {
      return List.of(risk(p.description(), si, rateBp, null, zone));
    }
    BigDecimal main = si.multiply(MAIN_BUILDING_SHARE).setScale(0, RoundingMode.HALF_EVEN);
    return List.of(
        risk(p.description() + " - main building", main, rateBp, occupation, zone),
        risk(p.description() + " - stocks", si.subtract(main), rateBp, occupation, zone));
  }

  private static RiskRequest risk(
      String description, BigDecimal si, int rateBp, String occupation, String zone) {
    BigDecimal premium =
        si.multiply(BigDecimal.valueOf(rateBp)).divide(BASIS_POINTS, 2, RoundingMode.HALF_EVEN);
    return new RiskRequest(
        description,
        si,
        BigDecimal.valueOf(rateBp)
            .divide(BigDecimal.valueOf(100), RATE_SCALE, RoundingMode.HALF_EVEN),
        premium,
        occupation,
        zone,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }
}
