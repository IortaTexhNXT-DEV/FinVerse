package com.iortatechnxt.brokerverse.renewal.rules.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The automatic renewal values of a CBG Motor account (BDOI Renewal FRS FRRN.009.01, Annexes D and
 * E): the new inception and expiry, the own damage and theft sum insured depreciated by year, the
 * bodily injury and property damage limits stepped down below the new sum insured, the premiums,
 * the documentary stamp tax (12.5% rounded up to PHP 0.50), the VAT (12%) and the local government
 * tax in the proportion of the expiring one; a CTPL policy keeps its expiring values; an Auto Promo
 * account becomes a Regular one with its risk code and rate (Annex E). Pure computation.
 */
public final class CbgMotorRenewal {

  /** Policy types of Annex D. */
  public enum PolicyType {
    /** Regular comprehensive policy. */
    REGULAR,
    /** Auto Promo / FFYMI policy. */
    AUTO_PROMO,
    /** Compulsory third party liability only. */
    CTPL
  }

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final BigDecimal DST_RATE = new BigDecimal("0.125");
  private static final BigDecimal VAT_RATE = new BigDecimal("0.12");
  private static final BigDecimal HALF = new BigDecimal("0.50");
  private static final BigDecimal NINETY = new BigDecimal("0.90");
  private static final int MONEY = 2;
  private static final int WORK = 10;
  private static final int RATE_SCALE = 4;

  /** Annex D: the depreciation of a multi-year policy, divisor and factor by term. */
  private static final Map<Integer, BigDecimal[]> MYP_FACTORS =
      Map.of(
          2, new BigDecimal[] {new BigDecimal("1.9"), new BigDecimal("0.81")},
          3, new BigDecimal[] {new BigDecimal("2.71"), new BigDecimal("0.729")},
          4, new BigDecimal[] {new BigDecimal("3.439"), new BigDecimal("0.6561")},
          5, new BigDecimal[] {new BigDecimal("4.0951"), new BigDecimal("0.59049")});

  private CbgMotorRenewal() {}

  /**
   * The renewal values.
   *
   * @param in expiring values and the term
   * @param tables BI and PD tiers and the Auto Promo mappings
   * @return renewal values
   */
  public static Result compute(Input in, Tables tables) {
    LocalDate inception = in.expiryDate();
    LocalDate expiry = expiry(inception, in.termYears(), in.type());
    if (in.type() == PolicyType.CTPL) {
      return ctpl(in, inception, expiry);
    }
    String riskCode = in.riskCode();
    BigDecimal rate = in.premiumRate();
    if (in.type() == PolicyType.AUTO_PROMO && tables.promoToRegular()) {
      riskCode = tables.promoRiskCodes().getOrDefault(riskCode, riskCode);
      rate = tables.promoRates().getOrDefault(key(rate), rate);
    }
    BigDecimal od = odTheft(in.odTheft(), in.termYears(), in.type());
    BigDecimal odPremium = money(od.multiply(rate).divide(HUNDRED, WORK, RoundingMode.HALF_UP));
    boolean keepLimits = in.type() == PolicyType.AUTO_PROMO;
    Tier bi =
        keepLimits
            ? new Tier(in.bi(), in.biPremium())
            : stepDown(in.bi(), in.biPremium(), od, tables.biTiers());
    Tier pd =
        keepLimits
            ? new Tier(in.pd(), in.pdPremium())
            : stepDown(in.pd(), in.pdPremium(), od, tables.pdTiers());
    BigDecimal basic = odPremium.add(bi.premium()).add(pd.premium()).add(nz(in.autoPaPremium()));
    BigDecimal dst = dst(basic);
    BigDecimal vat = money(basic.multiply(VAT_RATE));
    BigDecimal lgt =
        nz(in.basic()).signum() == 0
            ? BigDecimal.ZERO
            : money(basic.multiply(nz(in.lgt())).divide(in.basic(), WORK, RoundingMode.HALF_UP));
    return new Result(
        riskCode,
        rate,
        inception,
        expiry,
        od,
        bi.limit(),
        pd.limit(),
        odPremium,
        bi.premium(),
        pd.premium(),
        nz(in.autoPaPremium()),
        basic,
        dst,
        vat,
        lgt,
        BigDecimal.ZERO,
        basic.add(dst).add(vat).add(lgt));
  }

  private static Result ctpl(Input in, LocalDate inception, LocalDate expiry) {
    return new Result(
        in.riskCode(),
        in.premiumRate(),
        inception,
        expiry,
        in.odTheft(),
        in.bi(),
        in.pd(),
        BigDecimal.ZERO,
        in.biPremium(),
        in.pdPremium(),
        in.autoPaPremium(),
        in.basic(),
        in.dst(),
        in.vat(),
        in.lgt(),
        in.otherCharges(),
        in.total());
  }

  /**
   * The new expiry: one year after the inception (also for the 3 to 5-year terms and CTPL), two
   * years for a 2-year regular policy (Annex D).
   *
   * @param inception new inception
   * @param termYears term of the policy
   * @param type policy type
   * @return new expiry
   */
  static LocalDate expiry(LocalDate inception, int termYears, PolicyType type) {
    return termYears == 2 && type == PolicyType.REGULAR
        ? inception.plusYears(2)
        : inception.plusYears(1);
  }

  /**
   * The own damage and theft sum insured: 90% of the previous one for a one-year policy; for a
   * multi-year policy the previous one divided by the divisor of its term times its factor.
   *
   * @param previous previous sum insured
   * @param termYears term
   * @param type policy type
   * @return new sum insured
   */
  static BigDecimal odTheft(BigDecimal previous, int termYears, PolicyType type) {
    BigDecimal p = nz(previous);
    BigDecimal[] f = type == PolicyType.REGULAR ? MYP_FACTORS.get(termYears) : null;
    if (f == null) {
      return money(p.multiply(NINETY));
    }
    return money(p.divide(f[0], WORK, RoundingMode.HALF_UP).multiply(f[1]));
  }

  /**
   * The documentary stamp tax: 12.5% of the basic premium rounded up to PHP 0.50 (PHP 1,537.12
   * becomes 1,537.50; PHP 1,537.60 becomes 1,538.00).
   *
   * @param basic basic premium
   * @return tax
   */
  public static BigDecimal dst(BigDecimal basic) {
    BigDecimal raw = basic.multiply(DST_RATE);
    BigDecimal halves = raw.divide(HALF, 0, RoundingMode.CEILING);
    return halves.multiply(HALF).setScale(MONEY, RoundingMode.UNNECESSARY);
  }

  /**
   * A BI or PD limit: kept when below the new sum insured, else the next tier down until it is
   * below, with the premium of that tier; the expiring premium when the limit is unchanged.
   *
   * @param limit expiring limit
   * @param premium expiring premium
   * @param od new own damage and theft sum insured
   * @param tiers limits and premiums, any order
   * @return new limit and premium
   */
  static Tier stepDown(BigDecimal limit, BigDecimal premium, BigDecimal od, List<Tier> tiers) {
    if (limit == null || limit.signum() == 0 || limit.compareTo(od) < 0) {
      return new Tier(nz(limit), nz(premium));
    }
    return tiers.stream()
        .filter(t -> t.limit().compareTo(od) < 0 && t.limit().compareTo(limit) < 0)
        .max((a, b) -> a.limit().compareTo(b.limit()))
        .orElse(new Tier(BigDecimal.ZERO, BigDecimal.ZERO));
  }

  private static String key(BigDecimal rate) {
    return rate == null ? "" : rate.setScale(RATE_SCALE, RoundingMode.HALF_UP).toPlainString();
  }

  private static BigDecimal money(BigDecimal v) {
    return v.setScale(MONEY, RoundingMode.HALF_UP);
  }

  private static BigDecimal nz(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }

  /**
   * A BI or PD tier.
   *
   * @param limit coverage limit
   * @param premium premium of the limit
   */
  public record Tier(BigDecimal limit, BigDecimal premium) {}

  /**
   * The tiers and the Auto Promo mappings.
   *
   * @param biTiers bodily injury tiers
   * @param pdTiers property damage tiers
   * @param promoToRegular whether an Auto Promo account renews as Regular (Annex E)
   * @param promoRiskCodes risk code of the Regular account by Auto Promo risk code
   * @param promoRates rate of the Regular account by Auto Promo rate (4 decimals)
   */
  public record Tables(
      List<Tier> biTiers,
      List<Tier> pdTiers,
      boolean promoToRegular,
      Map<String, String> promoRiskCodes,
      Map<String, BigDecimal> promoRates) {

    /** Defensive copies. */
    public Tables {
      biTiers = List.copyOf(biTiers);
      pdTiers = List.copyOf(pdTiers);
      promoRiskCodes = Map.copyOf(promoRiskCodes);
      promoRates = Map.copyOf(promoRates);
    }
  }

  /**
   * The expiring values.
   *
   * @param type policy type
   * @param termYears term in years (1 for a non-MYP policy)
   * @param riskCode risk code
   * @param premiumRate premium rate in percent
   * @param expiryDate expiry of the expiring policy
   * @param odTheft own damage and theft sum insured
   * @param bi BI limit
   * @param biPremium BI premium
   * @param pd PD limit
   * @param pdPremium PD premium
   * @param autoPaPremium Auto PA premium
   * @param basic basic premium
   * @param dst documentary stamp tax
   * @param vat VAT
   * @param lgt local government tax
   * @param otherCharges other charges
   * @param total total premium
   */
  public record Input(
      PolicyType type,
      int termYears,
      String riskCode,
      BigDecimal premiumRate,
      LocalDate expiryDate,
      BigDecimal odTheft,
      BigDecimal bi,
      BigDecimal biPremium,
      BigDecimal pd,
      BigDecimal pdPremium,
      BigDecimal autoPaPremium,
      BigDecimal basic,
      BigDecimal dst,
      BigDecimal vat,
      BigDecimal lgt,
      BigDecimal otherCharges,
      BigDecimal total) {}

  /**
   * The renewal values.
   *
   * @param riskCode risk code
   * @param premiumRate premium rate in percent
   * @param inception new inception
   * @param expiry new expiry
   * @param odTheft new OD/theft coverage
   * @param bi new BI coverage
   * @param pd new PD coverage
   * @param odTheftPremium new OD/theft premium
   * @param biPremium new BI premium
   * @param pdPremium new PD premium
   * @param autoPaPremium Auto PA premium
   * @param basic basic premium
   * @param dst documentary stamp tax
   * @param vat VAT
   * @param lgt local government tax
   * @param otherCharges other charges
   * @param total total premium
   */
  public record Result(
      String riskCode,
      BigDecimal premiumRate,
      LocalDate inception,
      LocalDate expiry,
      BigDecimal odTheft,
      BigDecimal bi,
      BigDecimal pd,
      BigDecimal odTheftPremium,
      BigDecimal biPremium,
      BigDecimal pdPremium,
      BigDecimal autoPaPremium,
      BigDecimal basic,
      BigDecimal dst,
      BigDecimal vat,
      BigDecimal lgt,
      BigDecimal otherCharges,
      BigDecimal total) {}
}
