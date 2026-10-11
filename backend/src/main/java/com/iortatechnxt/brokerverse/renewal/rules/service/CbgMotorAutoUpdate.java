package com.iortatechnxt.brokerverse.renewal.rules.service;

import com.iortatechnxt.brokerverse.account.domain.RiskItemData;
import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.catalog.domain.RiskProductRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSnapshot;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.Input;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.PolicyType;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.Result;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.Tables;
import com.iortatechnxt.brokerverse.renewal.rules.service.CbgMotorRenewal.Tier;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Applies the CBG Motor automatic renewal values (FRRN.009.01, Annexes D and E) to the renewal
 * account of a CBG Motor renewal when it is created: the depreciated own damage and theft sum
 * insured, the BI and PD limits stepped down, and an Auto Promo account renewed as Regular with its
 * risk code and rate. The account is then rated by the package as any account; the breakdown of
 * Annex D is shown next to it in the Computations tab. Switched by {@value #SWITCH}.
 */
@Component
public class CbgMotorAutoUpdate {

  /** Parameter: true to apply the automatic values. */
  public static final String SWITCH = "RNW_CBG_MOTOR_AUTO_UPDATE";

  /** Parameter: BI tiers as limit:premium pairs. */
  public static final String BI_TIERS = "RNW_CBG_MOTOR_BI_TIERS";

  /** Parameter: PD tiers as limit:premium pairs. */
  public static final String PD_TIERS = "RNW_CBG_MOTOR_PD_TIERS";

  /** Parameter: risk codes of the Auto Promo / FFYMI policies. */
  public static final String PROMO_CODES = "RNW_AUTO_PROMO_RISK_CODES";

  /** Parameter: true to renew an Auto Promo account as Regular. */
  public static final String PROMO_TO_REGULAR = "RNW_PROMO_TO_REGULAR";

  /** Parameter: Regular risk code by Auto Promo risk code. */
  public static final String PROMO_RISK_MAP = "RNW_PROMO_RISK_CODE_MAP";

  /** Parameter: Regular rate by Auto Promo rate. */
  public static final String PROMO_RATE_MAP = "RNW_PROMO_RATE_MAP";

  /** Parameter: risk codes of the CTPL policies. */
  public static final String CTPL_CODES = "RNW_CTPL_RISK_CODES";

  private static final String MOTOR = "MOTOR";
  private static final String TRUE = "true";
  private static final int RATE_SCALE = 4;

  private final SystemParameterService parameters;
  private final RenewalParameters renewal;
  private final RiskProductRepository products;

  /**
   * Creates the update.
   *
   * @param parameters system parameters
   * @param renewal renewal parameters (CBG segments)
   * @param products risk products (the Regular product must exist)
   */
  public CbgMotorAutoUpdate(
      SystemParameterService parameters,
      RenewalParameters renewal,
      RiskProductRepository products) {
    this.parameters = parameters;
    this.renewal = renewal;
    this.products = products;
  }

  /**
   * Whether the automatic values apply to a renewal: a CBG segment, the Motor line and the switch
   * on.
   *
   * @param c renewal
   * @return true for a CBG Motor renewal
   */
  public boolean applies(RenewalCandidate c) {
    CandidateSnapshot.SnapshotProduct p = c.getSnapshot().product();
    return p != null
        && MOTOR.equals(p.lineCode())
        && renewal.cbgSegment(p.segment())
        && TRUE.equals(parameters.text(SWITCH, TRUE).strip());
  }

  /**
   * The Annex D values of a renewal from its expiring values (one vehicle, the first item).
   *
   * @param c renewal
   * @param expiringItems items of the expiring account, or empty
   * @return values, empty when they do not apply
   */
  public Optional<Result> values(RenewalCandidate c, List<RiskItemData> expiringItems) {
    if (!applies(c)) {
      return Optional.empty();
    }
    CandidateSnapshot s = c.getSnapshot();
    CandidateSnapshot.SnapshotPremium p = s.premium();
    RiskItemData item = expiringItems.isEmpty() ? null : expiringItems.get(0);
    Tables tables = tables();
    BigDecimal bi = item == null ? null : item.biLimit();
    BigDecimal pd = item == null ? null : item.pdLimit();
    Input in =
        new Input(
            type(s.product().productCode()),
            1,
            s.product().productCode(),
            rateOf(item, p),
            s.expiryDate(),
            sumInsuredOf(item, p),
            bi,
            premiumOf(bi, tables.biTiers()),
            pd,
            premiumOf(pd, tables.pdTiers()),
            BigDecimal.ZERO,
            p == null ? null : p.basicPremium(),
            null,
            null,
            null,
            null,
            p == null ? null : p.grossPremium());
    return Optional.of(CbgMotorRenewal.compute(in, tables));
  }

  /**
   * The renewal account draft with the automatic values: the sum insured, limits and rate of the
   * vehicle, and the Regular product of an Auto Promo account when the catalogue has it.
   *
   * @param c renewal
   * @param draft draft from the expiring account
   * @return draft to create
   */
  public AccountDraft apply(RenewalCandidate c, AccountDraft draft) {
    Optional<Result> values = values(c, draft.items());
    if (values.isEmpty() || draft.items().isEmpty()) {
      return draft;
    }
    Result r = values.get();
    List<RiskItemData> items = new ArrayList<>(draft.items());
    RiskItemData first = items.get(0);
    items.set(
        0,
        new RiskItemData(
            first.description(),
            r.odTheft(),
            r.premiumRate(),
            first.biLimit() == null ? null : r.bi(),
            first.pdLimit() == null ? null : r.pd(),
            first.vehicle(),
            first.location(),
            first.person()));
    String product =
        products.findByCode(r.riskCode()).isPresent() ? r.riskCode() : draft.productCode();
    return new AccountDraft(
        draft.clientId(),
        product,
        draft.marketSegment(),
        draft.sourceChannel(),
        draft.insurerCode(),
        draft.insurerBranch(),
        r.inception(),
        r.expiry(),
        draft.multiYear(),
        draft.termYears(),
        draft.currency(),
        draft.paymentArrangement(),
        draft.mortgage(),
        draft.contact(),
        items,
        draft.ratingBasis(),
        draft.commissionRate(),
        draft.ffyStart());
  }

  private PolicyType type(String riskCode) {
    if (listed(CTPL_CODES, riskCode)) {
      return PolicyType.CTPL;
    }
    return listed(PROMO_CODES, riskCode) ? PolicyType.AUTO_PROMO : PolicyType.REGULAR;
  }

  /**
   * The tiers and mappings of the parameters.
   *
   * @return tables
   */
  public Tables tables() {
    Map<String, BigDecimal> rates = new HashMap<>();
    pairs(PROMO_RATE_MAP)
        .forEach(
            (k, v) ->
                rates.put(
                    new BigDecimal(k).setScale(RATE_SCALE, RoundingMode.HALF_UP).toPlainString(),
                    new BigDecimal(v)));
    return new Tables(
        tiers(BI_TIERS),
        tiers(PD_TIERS),
        TRUE.equals(parameters.text(PROMO_TO_REGULAR, TRUE).strip()),
        pairs(PROMO_RISK_MAP),
        rates);
  }

  private List<Tier> tiers(String key) {
    List<Tier> tiers = new ArrayList<>();
    pairs(key).forEach((k, v) -> tiers.add(new Tier(new BigDecimal(k), new BigDecimal(v))));
    return tiers;
  }

  private Map<String, String> pairs(String key) {
    Map<String, String> map = new HashMap<>();
    for (String item : parameters.items(key)) {
      String[] kv = item.split("[=:]", 2);
      if (kv.length == 2 && !kv[0].isBlank() && !kv[1].isBlank()) {
        map.put(kv[0].strip(), kv[1].strip());
      }
    }
    return map;
  }

  private boolean listed(String key, String value) {
    return value != null && parameters.items(key).stream().anyMatch(v -> v.strip().equals(value));
  }

  private static BigDecimal premiumOf(BigDecimal limit, List<Tier> tiers) {
    if (limit == null) {
      return BigDecimal.ZERO;
    }
    return tiers.stream()
        .filter(t -> t.limit().compareTo(limit) == 0)
        .map(Tier::premium)
        .findFirst()
        .orElse(BigDecimal.ZERO);
  }

  private static BigDecimal rateOf(RiskItemData item, CandidateSnapshot.SnapshotPremium p) {
    if (item != null && item.rate() != null) {
      return item.rate();
    }
    return p == null || p.premiumRate() == null ? BigDecimal.ZERO : p.premiumRate();
  }

  private static BigDecimal sumInsuredOf(RiskItemData item, CandidateSnapshot.SnapshotPremium p) {
    if (item != null && item.sumInsured() != null) {
      return item.sumInsured();
    }
    return p == null ? BigDecimal.ZERO : p.totalSumInsured();
  }
}
